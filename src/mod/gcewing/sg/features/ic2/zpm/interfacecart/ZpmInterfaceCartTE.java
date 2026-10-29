package gcewing.sg.features.ic2.zpm.interfacecart;

import static gcewing.sg.BaseUtils.min;
import static gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCart.ZPM_LOADED;

import gcewing.sg.BaseMod;
import gcewing.sg.BaseTileInventory;
import gcewing.sg.features.ic2.zpm.Ic2ZpmSources;
import gcewing.sg.features.zpm.ZPMItem;
import gcewing.sg.features.zpm.ZpmBuffer;
import gcewing.sg.features.zpm.ZpmSource;
import gcewing.sg.interfaces.ISGEnergySource;
import gcewing.sg.SGCraft;
import gcewing.sg.features.zpm.link.ILinkableZpm;
import gcewing.sg.features.zpm.link.ZpmLink;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GateUtil;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// Works without IC2 (stargates and Forge Energy machines draw on it); with IC2 it is on the IC2 energy network too. Forge
// strips IEnergySource and the method naming IC2's types when IC2 isn't installed.
@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "ic2")
public final class ZpmInterfaceCartTE extends BaseTileInventory implements ISGEnergySource, ILinkableZpm, IEnergySource, IInventory, ITickable, gcewing.sg.BaseContainer.ChangesSlotItems {

    // Changes made to items going into or out of its slots, for open screens to send (see BaseContainer.ChangesSlotItems)
    private int slotItemChanges = 0;

    @Override
    public int slotItemChanges() {
        return slotItemChanges;
    }
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    public final ZpmSource source;
    // The ZPM's energy, saved by the cart itself so a world opens with or without IC2 (IC2's source saved it before)
    private static final String ENERGY_TAG = "zpmEnergy";
    public static final int firstZpmSlot = 0;
    public static final int numZpmSlots = 1;
    public static final int numSlots = numZpmSlots; // future usage > 1

    private int update = 0;

    // Same per-call cap as the ZPM console.
    private static final int maxForgeEnergyExtract = 5000;
    private final IEnergyStorage forgeEnergy = new ForgeEnergyAdapter();

    public ZpmInterfaceCartTE() {
        this.source = BaseMod.isModLoaded("ic2") ? Ic2ZpmSources.cart(this, Integer.MAX_VALUE, 3)
            : new ZpmBuffer(Integer.MAX_VALUE, 3, this::afterDraw);
    }

    // After something drew on the ZPM: save it, and a tainted ZPM blows up
    void afterDraw(double amount) {
        markChanged();
        if (isTainted(getStackInSlot(0))) {
            world.newExplosion(null, pos.getX(), pos.getY(), pos.getZ(), (float) 250, true, true);
        }
    }

    /* TileEntity */

    // The stargate it's linked to, if any (see ZpmLinks)
    private final ZpmLink zpmLink = new ZpmLink();

    @Override
    public ZpmLink getZpmLink() {
        return zpmLink;
    }

    @Override
    public boolean hasNoZpm() {
        return isEmpty();
    }

    @Override
    public void readFromNBT(final NBTTagCompound compound) {
        // On the client, what the server sends is all it holds: a slot it leaves out is empty (the block shows what's
        // here, see getActualState), not whatever was there before
        if (world != null && world.isRemote) {
            items.clear();
        }
        reading = true;
        try {
            super.readFromNBT(compound);
        } finally {
            reading = false;
        }
        this.zpmLink.readFromNBT(compound);
        // The energy of the ZPM put back in its slot
        if (compound.hasKey(ENERGY_TAG, 99 /* number */)) {
            this.source.setEnergyStored(Math.max(0, compound.getDouble(ENERGY_TAG)));
        } else if (BaseMod.isModLoaded("ic2")) {
            Ic2ZpmSources.readLegacy(this.source, compound);
        }
    }

    // Reading saved data, which puts the ZPM back in its slot; its energy is saved by the cart (see readFromNBT)
    private boolean reading = false;

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        super.writeToNBT(compound);
        this.zpmLink.writeToNBT(compound);
        compound.setDouble(ENERGY_TAG, this.source.getEnergyStored());

        return compound;
    }

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        this.source.update();
        pushForgeEnergy();
    }

    // Feed neighbouring Forge Energy acceptors (EnderIO conduits, RF/FE machines), alongside the IC2 network when installed.
    private void pushForgeEnergy() {
        if (this.source.getEnergyStored() <= 0) {
            return;
        }
        for (EnumFacing side : EnumFacing.values()) {
            TileEntity tile = this.world.getTileEntity(this.pos.offset(side));
            if (tile == null || tile instanceof ZpmInterfaceCartTE || !tile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) {
                continue;
            }
            IEnergyStorage acceptor = tile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (acceptor == null || !acceptor.canReceive()) {
                continue;
            }
            int accepted = acceptor.receiveEnergy(this.forgeEnergy.extractEnergy(maxForgeEnergyExtract, true), true);
            if (accepted > 0) {
                acceptor.receiveEnergy(this.forgeEnergy.extractEnergy(accepted, false), false);
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) this.forgeEnergy;
        }
        return super.getCapability(capability, facing);
    }

    /**
     * Extract-only Forge Energy view of the cart's source, converting at {@link SGCraft#Ic2FEPerEU}.
     */
    private class ForgeEnergyAdapter implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            double ratio = SGCraft.Ic2FEPerEU;
            if (ratio <= 0 || maxExtract <= 0) {
                return 0;
            }
            int fe = (int) Math.min(Math.min(maxExtract, maxForgeEnergyExtract), Math.floor(source.getEnergyStored() * ratio));
            if (fe <= 0) {
                return 0;
            }
            if (!simulate) {
                drawEnergy(fe / ratio); // goes through the taint check and markChanged
            }
            return fe;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, source.getEnergyStored() * SGCraft.Ic2FEPerEU);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, source.getCapacity() * SGCraft.Ic2FEPerEU);
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    @Override
    public void onChunkUnload() {
        this.source.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate(); // this is important for mc!
        this.source.invalidate(); // notify the energy hubSource
    }

    @Override
    @Nonnull
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        final NBTTagCompound result = new NBTTagCompound();
        this.writeToNBT(result);
        return result;
    }

    @Override
    public void onDataPacket(final NetworkManager net, final SPacketUpdateTileEntity packet) {
        final NBTTagCompound tag = packet.getNbtCompound();
        this.readFromNBT(tag);
    }

    /* Energy */

    // In SGCraft energy units, as every ISGEnergySource gives it, not the ZPM energy it holds
    @Override
    public double availableEnergy() {
        return this.source.getEnergyStored() / SGCraft.ZPMEnergyPerSGEnergyUnit;
    }

    // The ZPM energy it holds, for its screen
    public double storedZpmEnergy() {
        return this.source.getEnergyStored();
    }

    @Override
    public double totalAvailableEnergy() {
        return this.source.getCapacity();
    }

    @Override
    public double drawEnergyDouble(double amount) {
        double available = availableEnergy();
        double supply = min(amount, available);
        this.source.drawEnergy(supply * SGCraft.ZPMEnergyPerSGEnergyUnit);

        if (isTainted(this.getStackInSlot(0))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
        return supply;
    }

    @Override
    public double getOfferedEnergy() {
        return this.source.getOfferedEnergy();
    }

    @Override
    public void drawEnergy(double v) {
        this.source.drawEnergy(v);
        if (isTainted(this.getStackInSlot(0))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
    }

    @Override
    public int getSourceTier() {
        return this.source.getSourceTier();
    }

    @Override
    @Optional.Method(modid = "ic2")
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing enumFacing) {
        if (isTainted(this.getStackInSlot(0))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
        return Ic2ZpmSources.emitsEnergyTo(this.source, iEnergyAcceptor, enumFacing);
    }

    /* Inventory */

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    protected IInventory getInventory() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        for(final ItemStack item : this.items) {
            if(!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(final int index) {
        return this.items.get(0);
    }

    @Override // This prevents the zpm from being input/extract from the console.
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[0];
    }

    @Override
    public ItemStack removeStackFromSlot(final int index) {
        return decrStackSize(index, 1);
    }

    // The ZPM, with what's left of its energy (see setInventorySlotContents)
    @Override
    public ItemStack decrStackSize(final int index, final int quantity) {
        final ItemStack item = this.items.get(0);
        if (item.isEmpty() || quantity <= 0) {
            return ItemStack.EMPTY;
        }
        setInventorySlotContents(0, ItemStack.EMPTY);
        return item;
    }

    private void pingSGBaseTE() {
        if (world != null && !world.isRemote) {
            TileEntity localGateTE = GateUtil.locateLocalGate(this.world, this.pos, SGCraft.zpmSearchRange, false);

            if (localGateTE instanceof SGBaseTE) {
                SGBaseTE localGate = (SGBaseTE) localGateTE;
                localGate.validateZPM();
            }
        }
    }

    // A ZPM's energy goes into the source as it goes into the slot, and what's left goes back onto it as it leaves,
    // however it leaves: taken, shift-clicked, swapped with a hotbar key or for the stack on the cursor, or by another
    // mod. Only one taken through decrStackSize used to get it back, so a ZPM swapped out kept the energy it went in
    // with while the cart kept supplying it too: endless energy. (On a client the server's sync sets the source.)
    @Override
    public void setInventorySlotContents(final int index, final ItemStack item) {
        final ItemStack old = this.items.get(0);
        final boolean server = world != null && !world.isRemote && !reading;
        if (server && old != item) {
            slotItemChanges++;
            if (isValidFuelItem(old)) {
                NBTTagCompound tag = old.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    old.setTagCompound(tag);
                }
                tag.setDouble(ZPMItem.ENERGY, this.source.getEnergyStored());
                tag.setBoolean(ZPMItem.LOADED, false);
            }
            this.source.setEnergyStored(0);
            if (isValidFuelItem(item)) {
                NBTTagCompound tag = item.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    item.setTagCompound(tag);
                }
                if (!tag.hasKey(ZPMItem.ENERGY, 99 /* number */)) {
                    tag.setDouble(ZPMItem.ENERGY, Integer.MAX_VALUE);
                }
                tag.setBoolean(ZPMItem.LOADED, true);
                this.source.setCapacity(Math.max(this.source.getCapacity(), tag.getDouble(ZPMItem.ENERGY)));
                this.source.setEnergyStored(Math.max(0, tag.getDouble(ZPMItem.ENERGY)));
            }
        }
        this.items.set(0, item);

        if (world != null) {
            // Only while it's still a cart: this also runs as it's broken (see ZpmInterfaceCart.breakBlock)
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ZpmInterfaceCart && state.getValue(ZPM_LOADED) != isValidFuelItem(item)) {
                world.setBlockState(pos, state.withProperty(ZPM_LOADED, isValidFuelItem(item)), 3);
            }
            markChanged();
        }
        if (server && item.isEmpty() && !old.isEmpty()) {
            pingSGBaseTE();
        }
    }

    public static boolean isValidFuelItem(ItemStack stack) {
        return stack != null && stack.getItem() == SGCraft.zpm && stack.getCount() > 0;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUsableByPlayer(final EntityPlayer player) {
        return player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }


    @Override
    public boolean isItemValidForSlot(final int index, final ItemStack item) {
        return item.getItem() instanceof ZPMItem;
    }

    @Override
    public int getField(final int id) {
        return 0;
    }

    @Override
    public void setField(final int id, final int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    @Override
    public String getName() {
        return "container.zero_point_module";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString("ZPM Container");
    }

    public static ZpmInterfaceCartTE at(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof ZpmInterfaceCartTE ? (ZpmInterfaceCartTE) te : null;
    }

    public boolean isTainted(ItemStack item) {
        boolean hasTaint = false;
        NBTTagList nbttaglist = item.getEnchantmentTagList();
        for (int j = 0; j < nbttaglist.tagCount(); ++j) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(j);
            int k = nbttagcompound.getShort("id");
            int l = nbttagcompound.getShort("lvl");
            Enchantment enchantment = Enchantment.getEnchantmentByID(k);
            if (k == 51) {
                hasTaint = true;
            }
        }
        return hasTaint;
    }

    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        if (oldState.getBlock() != newState.getBlock()) { // Prevents the TE from nullifying itself when we force change the state to change it models.  Vanilla mechanics invalidate the TE.
            return true;
        } else {
            return false;
        }
    }
}
