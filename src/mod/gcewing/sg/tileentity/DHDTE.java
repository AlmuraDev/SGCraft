//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate Controller Tile Entity
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.tileentity;

import static gcewing.sg.BaseBlockUtils.getWorldTileEntity;
import static gcewing.sg.BaseUtils.min;

import gcewing.sg.BaseConfiguration;
import gcewing.sg.BaseTileInventory;
import gcewing.sg.SGCraft;
import gcewing.sg.Trans3;
import gcewing.sg.Vector3;
import gcewing.sg.block.DHDBlock;
import gcewing.sg.interfaces.ISGEnergySource;
import gcewing.sg.util.SGState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class DHDTE extends BaseTileInventory implements ISGEnergySource {

    // Debug options
    public static boolean debugLink = false;

    // Static Configuration options
    public static int linkRangeX = 5; // either side
    public static int linkRangeY = 1; // up or down
    public static int linkRangeZ = 6; // in front
    public static AxisAlignedBB bounds;
    public static final int firstFuelSlot = 0;
    public static final int numFuelSlots = 4;
    private static final int numSlots = numFuelSlots;

    public static double cfgMaxEnergyBuffer = 2500;


    // Instanced Options
    public boolean isLinkedToStargate;
    public BlockPos linkedPos = new BlockPos(0, 0, 0);
    public String enteredAddress = "";
    public IInventory inventory = new InventoryBasic("DHD", false, numSlots);
    public double maxEnergyBuffer = 2500;
    public double energyInBuffer = 0;

    // Required
    public DHDTE() {}

    // Required to handle instanced variables
    public DHDTE(final double maxEnergyBuffer) {
        this.maxEnergyBuffer = maxEnergyBuffer;
    }

    public static void configure(BaseConfiguration cfg) {
        linkRangeX = cfg.getInteger("dhd", "linkRangeX", linkRangeX);
        linkRangeY = cfg.getInteger("dhd", "linkRangeY", linkRangeY);
        linkRangeZ = cfg.getInteger("dhd", "linkRangeZ", linkRangeZ);
        cfgMaxEnergyBuffer = cfg.getDouble("dhd", "maxEnergyBuffer", cfgMaxEnergyBuffer);
    }

    public static DHDTE at(IBlockAccess world, BlockPos pos) {
        TileEntity te = getWorldTileEntity(world, pos);
        return te instanceof DHDTE ? (DHDTE) te : null;
    }

    public static DHDTE at(IBlockAccess world, NBTTagCompound nbt) {
        BlockPos pos = new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
        return DHDTE.at(world, pos);
    }

    // Only while its gate is idle, as its screen allows. A symbol entered while the gate was dialling (a computer's dial,
    // say, started just as the key was pressed) wrote over the dial's address and chevrons, and the gate then crashed
    // the server reading past the end of its address.
    public void enterSymbol(EntityPlayer player, char symbol) {
        SGBaseTE gate = getLinkedStargateTE();
        if (gate != null && gate.state == SGState.Idle) {
            if (enteredAddress.length() < gate.getNumChevrons()) {
                enteredAddress += symbol;
                if (gate.chevronsLockOnDial) {
                    boolean last = enteredAddress.length() == gate.getNumChevrons();
                    gate.finishDiallingSymbol(Character.toString(symbol), true, false, last);
                    gate.dialledAddress = enteredAddress;
                    gate.numEngagedChevrons = enteredAddress.length();
                    gate.markChanged();
                }
            }
        }
    }

    public void unsetSymbol(EntityPlayer player) {
        SGBaseTE gate = getLinkedStargateTE();
        if (gate != null && gate.state == SGState.Idle) {
            if (!enteredAddress.isEmpty()) {
                char symbol = enteredAddress.charAt(enteredAddress.length() - 1);
                enteredAddress = enteredAddress.substring(0, enteredAddress.length() - 1);
                if (gate.chevronsLockOnDial) {
                    gate.unsetSymbol(symbol);
                    gate.dialledAddress = enteredAddress;
                    gate.numEngagedChevrons = enteredAddress.length();
                    gate.markChanged();
                }
            }
        }
    }

    public void clearAddress() {
        enteredAddress = "";
        markChanged();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        // The model's bounds are about its own origin; move them to this block. (expand() here
        // stretched the box from the model out to the block's world coordinates, so it was in
        // view from almost anywhere and the DHD drew whenever it was in range.)
        if (bounds == null)
            return super.getRenderBoundingBox();
        return bounds.offset(getX() + 0.5, getY(), getZ() + 0.5);
    }

    @Override
    public  IInventory getInventory() {
        return this.inventory;
    }

    public DHDBlock getBlock() {
        return (DHDBlock)getBlockType();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.isLinkedToStargate = nbt.getBoolean("isLinkedToStargate");
        this.energyInBuffer = nbt.getDouble("energyInBuffer");
        int x = nbt.getInteger("linkedX");
        int y = nbt.getInteger("linkedY");
        int z = nbt.getInteger("linkedZ");
        this.linkedPos = new BlockPos(x, y, z);
        this.enteredAddress = nbt.getString("enteredAddress");

        // Check if Key doesn't exist or if Admin is trying to update all the DHD's with new values.
        if (!nbt.hasKey("bufferSize") || SGCraft.forceDHDCfgUpdate) {
            this.maxEnergyBuffer = cfgMaxEnergyBuffer;
        } else {
            this.maxEnergyBuffer = nbt.getDouble("bufferSize");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("isLinkedToStargate", this.isLinkedToStargate);
        nbt.setDouble("energyInBuffer", this.energyInBuffer);
        nbt.setInteger("linkedX", this.linkedPos.getX());
        nbt.setInteger("linkedY", this.linkedPos.getY());
        nbt.setInteger("linkedZ", this.linkedPos.getZ());
        nbt.setString("enteredAddress", this.enteredAddress);
        nbt.setDouble("bufferSize", this.maxEnergyBuffer);
        return nbt;
    }

    public SGBaseTE getLinkedStargateTE() {
        if (isLinkedToStargate) {
            TileEntity gte = getWorldTileEntity(world, linkedPos);
            if (gte instanceof SGBaseTE)
                return (SGBaseTE)gte;
        }
        return null;
    }

    public void checkForLink() {
        if (debugLink) {
            System.out.printf("DHDTE.checkForLink at %s: isLinkedToStargate = %s\n", pos, isLinkedToStargate);
        }
        if (!isLinkedToStargate) {
            Trans3 t = localToGlobalTransformation();
            for (int i = -linkRangeX; i <= linkRangeX; i++) {
                for (int j = -linkRangeY; j <= linkRangeY; j++) {
                    for (int k = -linkRangeZ; k <= linkRangeZ; k++) {
                        Vector3 p = t.p(i, j, -k);
                        //System.out.printf("DHDTE: Looking for stargate at (%d,%d,%d)\n",
                        //  p.floorX(), p.floorY(), p.floorZ());
                        BlockPos bp = new BlockPos(p.floorX(), p.floorY(), p.floorZ());
                        if (debugLink) {
                            System.out.printf("DHDTE.checkForLink: probing %s\n", bp);
                        }
                        TileEntity te = world.getTileEntity(bp);
                        if (te instanceof SGBaseTE) {
                            if (debugLink) {
                                System.out.printf("DHDTE.checkForLink: Found stargate at %s\n", te.getPos());
                            }
                            if (linkToStargate((SGBaseTE) te)) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean linkToStargate(SGBaseTE gte) {
        if (!isLinkedToStargate && !gte.isLinkedToController && gte.isMerged) {
            if (debugLink) {
                System.out.printf("DHDTE.linkToStargate: Linking controller at %s with stargate at %s\n", pos, gte.getPos());
            }
            linkedPos = gte.getPos();
            isLinkedToStargate = true;
            markChanged();
            gte.linkedPos = pos;
            gte.isLinkedToController = true;
            gte.markChanged();
            return true;
        }
        return false;
    }

    public void clearLinkToStargate() {
        if (debugLink) {
            System.out.printf("DHDTE: Unlinking controller at %s from stargate\n", pos);
        }
        isLinkedToStargate = false;
        markChanged();
    }

    @Override
    public double availableEnergy() {
        double energy = energyInBuffer;
        for (int i = 0; i < numFuelSlots; i++) {
            ItemStack stack = fuelStackInSlot(i);
            if (stack != null) {
                SGBaseTE baseTe = this.getLinkedStargateTE();
                energy += stack.getCount() * baseTe.energyPerFuelItem;
            }
        }
        return energy;
    }

    @Override
    public double drawEnergyDouble(double amount) {
        double energyDrawn = 0;
        while (energyDrawn < amount) {
            if (energyInBuffer == 0) {
                if (!useFuelItem())
                    break;
            }
            double e = min(amount, energyInBuffer);
            energyDrawn += e;
            energyInBuffer -= e;
        }
        if (SGBaseTE.debugEnergyUse) {
            System.out.printf("DHDTE.drawEnergyDouble: %s; supplied: %s; buffered: %s\n", amount, energyDrawn, energyInBuffer);
        }
        markChanged();
        return energyDrawn;
    }

    @Override public double totalAvailableEnergy() {
        return energyInBuffer;
    }

    boolean useFuelItem() {
        for (int i = numFuelSlots - 1; i >= 0; i--) {
            ItemStack stack = fuelStackInSlot(i);
            if (stack != null) {
                decrStackSize(i, 1);
                SGBaseTE baseTe = this.getLinkedStargateTE();
                energyInBuffer += baseTe.energyPerFuelItem;
                return true;
            }
        }
        return false;
    }

    ItemStack fuelStackInSlot(int i) {
        ItemStack stack = getStackInSlot(firstFuelSlot + i);
        return isValidFuelItem(stack) ? stack : null;
    }

    public static boolean isValidFuelItem(ItemStack stack) {
        return stack != null && stack.getItem() == SGCraft.naquadah && stack.getCount() > 0;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return isValidFuelItem(stack);
    }
}
