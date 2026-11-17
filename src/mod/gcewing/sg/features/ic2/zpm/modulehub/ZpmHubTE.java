package gcewing.sg.features.ic2.zpm.modulehub;

import static gcewing.sg.BaseUtils.min;
import static gcewing.sg.features.ic2.zpm.modulehub.ZpmHub.ZPMS;

import gcewing.sg.BaseMod;
import gcewing.sg.BaseTileInventory;
import gcewing.sg.SGCraft;
import gcewing.sg.features.ic2.zpm.Ic2ZpmSources;
import gcewing.sg.features.zpm.ZpmBuffer;
import gcewing.sg.features.zpm.ZpmSource;
import gcewing.sg.features.zpm.link.ILinkableZpm;
import gcewing.sg.features.zpm.link.ZpmLink;
import gcewing.sg.features.zpm.ZPMItem;
import gcewing.sg.interfaces.ISGEnergySource;
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
public final class ZpmHubTE extends BaseTileInventory implements ISGEnergySource, ILinkableZpm, IEnergySource, IInventory, ITickable, gcewing.sg.BaseContainer.ChangesSlotItems {

    // Changes made to items going into or out of its slots, for open screens to send (see BaseContainer.ChangesSlotItems)
    private int slotItemChanges = 0;

    @Override
    public int slotItemChanges() {
        return slotItemChanges;
    }
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    public final ZpmSource hubSource;
    public static final int firstZpmSlot = 0;
    public static final int numZpmSlots = 3;
    public static final int numSlots = numZpmSlots; // future usage > 1
    // The energy of the ZPM in each slot, which it takes back as it leaves (see setInventorySlotContents). The hub's
    // source holds their sum.
    private final double[] slotEnergy = new double[numZpmSlots];
    // Reading saved data, which puts the ZPMs back in their slots with their energy already in slotEnergy
    private boolean reading = false;

    private int update = 0;

    // Same per-call cap as the ZPM console and cart.
    private static final int maxForgeEnergyExtract = 5000;
    private final IEnergyStorage forgeEnergy = new ForgeEnergyAdapter();

    public ZpmHubTE() {
        this.hubSource = BaseMod.isModLoaded("ic2") ? Ic2ZpmSources.hub(this, Integer.MAX_VALUE, 3)
            : new ZpmBuffer(Integer.MAX_VALUE, 3, this::drawShared);
    }

    // ZPMs in a hub together go further: with n of them, each unit drawn costs them 1/n (the "optimization factor" its
    // screen shows), shared among them. Their own energies, which they take back as they leave, used to be lowered by
    // whole units only, so small draws (from a slow cable) took nothing from them at all.
    void drawShared(double amount) {
        if (amount <= 0) {
            return;
        }
        int zpmCount = getZpmSlotsloaded();
        drawFromZpms(zpmCount > 0 ? amount / zpmCount : amount); // sets the source's energy from theirs
        markChanged();
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
        // The source's energy is the ZPMs' together, set from them below (syncSource), so it isn't saved itself
        for (int i = 0; i < numZpmSlots; i++) {
            // Saved as whole numbers before; read either way
            slotEnergy[i] = isValidFuelItem(items.get(i)) ? Math.max(0, compound.getDouble("zpmSlot" + i + "Energy")) : 0;
        }
        syncSource();
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        super.writeToNBT(compound);
        this.zpmLink.writeToNBT(compound);
        for (int i = 0; i < numZpmSlots; i++) {
            compound.setDouble("zpmSlot" + i + "Energy", slotEnergy[i]);
        }
        return compound;
    }

    // The source holds what the ZPMs hold together
    private void syncSource() {
        double total = 0;
        for (double e : slotEnergy) {
            total += e;
        }
        this.hubSource.setEnergyStored(total);
    }

    // Takes energy (ZPM units) from the ZPMs that have any left, shared evenly as far as each has it
    void drawFromZpms(double amount) {
        for (int pass = 0; pass < numZpmSlots && amount > 0; pass++) {
            int left = 0;
            for (double e : slotEnergy) {
                if (e > 0) {
                    left++;
                }
            }
            if (left == 0) {
                break;
            }
            double share = amount / left;
            for (int i = 0; i < numZpmSlots; i++) {
                if (slotEnergy[i] > 0) {
                    double take = Math.min(share, slotEnergy[i]);
                    slotEnergy[i] -= take;
                    amount -= take;
                }
            }
        }
        syncSource();
    }

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        this.hubSource.update();
        pushForgeEnergy();
    }

    // Feed neighbouring Forge Energy acceptors (EnderIO conduits, RF/FE machines), alongside the IC2 network when installed.
    private void pushForgeEnergy() {
        if (this.hubSource.getEnergyStored() <= 0) {
            return;
        }
        for (EnumFacing side : EnumFacing.values()) {
            TileEntity tile = this.world.getTileEntity(this.pos.offset(side));
            if (tile == null || tile instanceof ZpmHubTE || !tile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) {
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
     * Extract-only Forge Energy view of the hub's source, converting at {@link SGCraft#Ic2FEPerEU}. Draws go through
     * {@link #drawShared}, so the loaded ZPMs share them exactly as they do IC2 and stargate draws.
     */
    private class ForgeEnergyAdapter implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            double ratio = SGCraft.Ic2FEPerEU;
            if (ratio <= 0 || maxExtract <= 0 || getZpmSlotsloaded() == 0) {
                return 0;
            }
            int fe = (int) Math.min(Math.min(maxExtract, maxForgeEnergyExtract), Math.floor(hubSource.getEnergyStored() * ratio));
            if (fe <= 0) {
                return 0;
            }
            if (!simulate) {
                if (isTainted(getStackInSlot(0)) || isTainted(getStackInSlot(1)) || isTainted(getStackInSlot(2))) {
                    world.newExplosion(null, pos.getX(), pos.getY(), pos.getZ(), (float) 250, true, true);
                }
                hubSource.drawEnergy(fe / ratio);
            }
            return fe;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, hubSource.getEnergyStored() * SGCraft.Ic2FEPerEU);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, hubSource.getCapacity() * SGCraft.Ic2FEPerEU);
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
        this.hubSource.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate(); // this is important for mc!
        this.hubSource.invalidate(); // notify the energy hubSource
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
        return this.hubSource.getEnergyStored() / SGCraft.ZPMEnergyPerSGEnergyUnit;
    }

    // The ZPM energy it holds, for its screen
    public double storedZpmEnergy() {
        return this.hubSource.getEnergyStored();
    }

    @Override
    public double totalAvailableEnergy() {
        return this.hubSource.getCapacity();
    }

    @Override
    public double drawEnergyDouble(double amount) {
        double available = availableEnergy();
        double supply = min(amount, available);
        this.hubSource.drawEnergy(supply * SGCraft.ZPMEnergyPerSGEnergyUnit);

        if (isTainted(this.getStackInSlot(0))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
        return supply;
    }

    @Override
    public double getOfferedEnergy() {
        return this.hubSource.getOfferedEnergy();
    }

    public int getZpmSlotsloaded() {
        int zpmCount = 0;

        for (double e : slotEnergy) {
            if (e > 0) {
                zpmCount = zpmCount + 1;
            }
        }

        if (zpmCount == 1) {
            this.hubSource.setSourceTier(3);
        } else if (zpmCount == 2) {
            this.hubSource.setSourceTier(4);
        } else if (zpmCount == 3) {
            this.hubSource.setSourceTier(5);
        }

        return zpmCount;
    }

    @Override
    public void drawEnergy(double v) {
        // IC2 draws through the source (ZpmHubBasicSource), not this
    }

    @Override
    public int getSourceTier() {
        // Note functionality wise this method does nothing.
        if (getZpmSlotsloaded() == 2) {
            return 4;
        }
        if (getZpmSlotsloaded() == 3) {
            return 5;
        }

        return 3;
    }

    @Override
    @Optional.Method(modid = "ic2")
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing enumFacing) {
        if (isTainted(this.getStackInSlot(0)) || isTainted(this.getStackInSlot(1))|| isTainted(this.getStackInSlot(2))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
        return Ic2ZpmSources.emitsEnergyTo(this.hubSource, iEnergyAcceptor, enumFacing);
    }

    /* Inventory */

    @Override
    public int getSizeInventory() {
        return 3;
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
        return this.items.get(index);
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
        if (index < 0 || index >= numZpmSlots) {
            return ItemStack.EMPTY;
        }
        final ItemStack item = this.items.get(index);
        if (item.isEmpty() || quantity <= 0) {
            return ItemStack.EMPTY;
        }
        setInventorySlotContents(index, ItemStack.EMPTY);
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

    // A ZPM's energy is the slot's as it goes in, and what's left goes back onto it as it leaves, however it leaves:
    // taken, shift-clicked, swapped with a hotbar key or for the stack on the cursor, or by another mod. Only one taken
    // through decrStackSize used to get it back, so a ZPM swapped out kept the energy it went in with, and one swapped
    // in for another added its own without taking the other's away: endless energy. (On a client the server's sync sets
    // the energies.)
    @Override
    public void setInventorySlotContents(final int index, final ItemStack item) {
        if (index < 0 || index >= numZpmSlots) {
            return;
        }
        final ItemStack old = this.items.get(index);
        final boolean server = world != null && !world.isRemote && !reading;
        if (server && old != item) {
            slotItemChanges++;
            if (isValidFuelItem(old)) {
                NBTTagCompound tag = old.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    old.setTagCompound(tag);
                }
                tag.setDouble(ZPMItem.ENERGY, slotEnergy[index]);
                tag.setBoolean(ZPMItem.LOADED, false);
            }
            slotEnergy[index] = 0;
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
                slotEnergy[index] = Math.max(0, tag.getDouble(ZPMItem.ENERGY));
            }
            syncSource();
        }
        this.items.set(index, item);

        if (world != null) {
            // Only while it's still a hub: this also runs as it's broken (see ZpmHub.breakBlock)
            IBlockState state = world.getBlockState(pos);
            int zpms = this.getZpmSlotsloaded();
            if (state.getBlock() instanceof ZpmHub && state.getValue(ZPMS) != zpms) {
                world.setBlockState(pos, state.withProperty(ZPMS, zpms), 3);
            }
            markChanged();
        }
        if (server && this.isEmpty() && !old.isEmpty()) {
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

    public static ZpmHubTE at(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof ZpmHubTE ? (ZpmHubTE) te : null;
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
