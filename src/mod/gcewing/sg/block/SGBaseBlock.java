//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate base block
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.block;

import static gcewing.sg.BaseOrientation.Orient4WaysByState.FACING;

import gcewing.sg.BaseBlockUtils;
import gcewing.sg.BaseConfiguration;
import gcewing.sg.BaseOrientation;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.SGCraft;
import gcewing.sg.util.GateShape;
import gcewing.sg.util.SGState;
import gcewing.sg.Trans3;
import gcewing.sg.Vector3;
import gcewing.sg.client.gui.SGGui;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.world.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;

import gcewing.sg.BaseMod.*;

public class SGBaseBlock extends SGBlock<SGBaseTE> {

    public static boolean debugMerge = false;
    static int explosionRadius = 10;
    static boolean fieryExplosion = true;
    static boolean smokyExplosion = true;
    static boolean debug = false;

    protected static String[] textures = {"stargateblock", "stargatering", "stargatebase_front"};
    protected static ModelSpec model = new ModelSpec("block/sg_base_block.smeg", textures);
    
    public static void configure(BaseConfiguration config) {
        explosionRadius = config.getInteger("stargate", "explosionRadius", explosionRadius);
        fieryExplosion = config.getBoolean("stargate", "explosionFlame", fieryExplosion);
        smokyExplosion = config.getBoolean("stargate", "explosionSmoke", smokyExplosion);
    }
    
    public SGBaseBlock() {
        super(Material.ROCK, SGBaseTE.class);
        setHardness(1.5F);
        setCreativeTab(CreativeTabs.MISC);
    }
    
    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return true; // So that translucent camouflage blocks render correctly
    }

    @Override
    public IOrientationHandler getOrientationHandler() {
        return BaseOrientation.orient4WaysByState;
    }
    
    @Override
    public String[] getTextureNames() {
        return textures;
    }
    
    @Override
    public ModelSpec getModelSpec(IBlockState state) {
        return model;
    }
    
    public GateShape getShape() {
        return GateShape.STANDARD;
    }

    @Override
    public SGBaseTE getBaseTE(IBlockAccess world, BlockPos pos) {
        return getTileEntity(world, pos);
    }

    @Override
    protected String getRendererClassName() {
        return "gcewing.sg.client.renderer.SGRingBlockRenderer";
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    
    @Override
    public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return SGCraft.canHarvestSGBaseBlock;
    }

    @Override
    public boolean isMerged(IBlockAccess world, BlockPos pos) {
        SGBaseTE te = getTileEntity(world, pos);
        return te != null && te.isMerged;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase player, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, player, stack);
        SGBaseTE te = getTileEntity(world, pos);
        if (te != null) {
            if (te instanceof SGBaseTE) {
                if (debug) {
                    System.out.println("Stargate base block placed " + player.getName() + " on world: " + world.getWorldInfo().getWorldName() + " at: " + pos);
                }
                te.isGenerated = false;
            }
        }
    }
    
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (debug) {
            System.out.println("STARGATE Base block added on world: " + world.getWorldInfo().getWorldName() + " at: " + pos);
        }

        checkForVerticalMerge(world, pos);
        checkForHorizontalMerge(world, pos);
        SGBaseTE te = getTileEntity(world, pos);
        if (te != null) {
            if (te instanceof SGBaseTE) {
                if (state.getValue(FACING).toString().equalsIgnoreCase("north")) {
                    te.facingDirectionOfBase = 0;
                }
                if (state.getValue(FACING).toString().equalsIgnoreCase("west")) {
                    te.facingDirectionOfBase = 1;
                }
                if (state.getValue(FACING).toString().equalsIgnoreCase("south")) {
                    te.facingDirectionOfBase = 2;
                }
                if (state.getValue(FACING).toString().equalsIgnoreCase("east")) {
                    te.facingDirectionOfBase = 3;
                }
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float cx, float cy, float cz) {
        String Side = world.isRemote ? "Client" : "Server";
        SGBaseTE te = getTileEntity(world, pos);
        if (te != null) {
            if (debugMerge) {
                System.out.printf("SGBaseBlock.onBlockActivated: %s: isMerged = %s\n", Side, te.isMerged);
            }

            if (te.isMerged && !world.isRemote)  {
                if (player.getHeldItemMainhand().getItem() == SGCraft.pdd || player.getHeldItemMainhand().getItem() == SGCraft.configurator || player.getHeldItemMainhand().getItem() == SGCraft.gdo) {
                    return false; // Return false here so the itemUse method fires
                }
                SGCraft.mod.openGui(player, SGGui.SGBase, world, pos);
                return true;
            }

            if (world.isRemote) {
                if (player.getHeldItemMainhand().getItem() == SGCraft.pdd || player.getHeldItemMainhand().getItem() == SGCraft.configurator || player.getHeldItemMainhand().getItem() == SGCraft.gdo) {
                    return false; // Return false here so the itemUse method fires
                }
                return true; //Client needs to be told too.
            }
        }
        return false;
    }
    
    @Override
    public boolean getWeakChanges(IBlockAccess world, BlockPos pos) {
        return true;
    }
    
    @Override    
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        neighbourChanged(world, pos);
    }

    protected void neighbourChanged(IBlockAccess world, BlockPos pos) {
        SGBaseTE te = getTileEntity(world, pos);
        if (te != null)
            te.onNeighborBlockChange();
    }

    public void checkForVerticalMerge(World world, BlockPos pos) {
        if (!isMerged(world, pos)) {
            GateShape shape = getShape();
            Trans3 t = localToGlobalTransformation(world, pos);
            for (int i = -shape.halfWidth; i <= shape.halfWidth; i++) {
                for (int j = 0; j < shape.height; j++) {
                    if (!(i == 0 && j == 0)) {
                        BlockPos rp = t.p(i, j, 0).blockPos();
                        int type = getRingBlockType(world, rp);
                        int pat = shape.cellAt(i, j);
                        if (pat != GateShape.EMPTY && type != pat) {
                            return;
                        }
                    }
                }
            }

            SGBaseTE te = getTileEntity(world, pos);
            te.setMerged(true);
            te.gateOrientation = 1;
            BaseBlockUtils.markBlockForUpdate(world, pos);
            for (int i = -shape.halfWidth; i <= shape.halfWidth; i++)
                for (int j = 0; j < shape.height; j++)
                    if (!(i == 0 && j == 0)) {
                        BlockPos rp = t.p(i, j, 0).blockPos();
                        Block block = world.getBlockState(rp).getBlock();
                        if (block instanceof SGRingBlock) {
                            ((SGRingBlock) block).mergeWith(world, rp, pos);
                        }
                    }
            te.checkForLink();
        }
    }

    public void checkForHorizontalMerge(World world, BlockPos pos) {
        boolean debugThisMerge = true;
        if (!isMerged(world, pos)) {
            GateShape shape = getShape();
            Trans3 t = localToGlobalTransformation(world, pos);
            for (int x = -shape.halfWidth; x <= shape.halfWidth; x++) {
                for (int z = -(shape.height - 1); z <= 0; z++) {
                    if (!(x == 0 && z == 0)) {
                        BlockPos rp = t.p(x, 0, z).blockPos();
                        int type = getRingBlockType(world, rp);
                        int pat = shape.cellAt(x, -z);
                        if (pat != GateShape.EMPTY && type != pat) {
                            return;
                        }
                    }
                }
            }

            SGBaseTE te = getTileEntity(world, pos);
            te.setMerged(true);
            te.gateOrientation = 2;
            BaseBlockUtils.markBlockForUpdate(world, pos);

            for (int x = -shape.halfWidth; x <= shape.halfWidth; x++) {
                for (int z = -(shape.height - 1); z <= 0; z++) {
                    if (!(x == 0 && z == 0)) {
                        BlockPos rp = t.p(x, 0, z).blockPos();
                        Block block = world.getBlockState(rp).getBlock();
                        if (block instanceof SGRingBlock) {
                            ((SGRingBlock) block).mergeWith(world, rp, pos);
                        }
                    }
                }
            }
            te.checkForLink();
            te.markForUpdate();
        }
    }
    
    protected int getRingBlockType(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.AIR)
            return GateShape.EMPTY;
        if (block == SGCraft.sgRingBlock) {
            if (!SGCraft.sgRingBlock.isMerged(world, pos)) {
                switch (state.getValue(SGRingBlock.VARIANT)) {
                    case 0: return GateShape.RING;
                    case 1: return GateShape.CHEVRON;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosionIn) {
        return SGCraft.canHarvestSGBaseBlock;
    }
    
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        unmerge(world, pos);
        if (SGCraft.canHarvestSGBaseBlock) {
            dropUpgrades(world, pos);
        }
        super.breakBlock(world, pos, state);
    }
    
    void dropUpgrades(World world, BlockPos pos) {
        SGBaseTE te = getTileEntity(world, pos);
        if (te != null) {
            if (te.hasChevronUpgrade)
                spawnAsEntity(world, pos, new ItemStack(SGCraft.sgChevronUpgrade));
            if (te.hasIrisUpgrade)
                spawnAsEntity(world, pos, new ItemStack(SGCraft.sgIrisUpgrade));
        }
    }
    
    public void unmerge(World world, BlockPos pos) {
        SGBaseTE te = getTileEntity(world, pos);
        boolean goBang = false;
        if (te != null /*&& te.isMerged*/) {
            if (te.isMerged && te.state == SGState.Connected) {
                te.state = SGState.Idle;
                goBang = true;
            }
            te.disconnect();
            te.unlinkFromController();
            te.setMerged(false);
            BaseBlockUtils.markBlockForUpdate(world, pos);
            unmergeRing(world, pos);
        }
        if (goBang && explosionRadius > 0)
            explode(world, new Vector3(pos).add(0.5, 0.5 + getShape().centreY, 0.5), explosionRadius);
    }
    
    void explode(World world, Vector3 p, double s) {
        world.newExplosion(null, p.x, p.y, p.z, (float)s, fieryExplosion, smokyExplosion);
    }
    
    void unmergeRing(World world, BlockPos pos) {
        GateShape shape = getShape();
        int r = shape.searchRadius();
        for (int i = -r; i <= r; i++)
            for (int j = 0; j <= shape.searchHeight(); j++)
                for (int k = -r; k <= r; k++)
                    unmergeRingBlock(world, pos, pos.add(i, j, k));
    }
    
    void unmergeRingBlock(World world, BlockPos pos, BlockPos ringPos) {
        Block block = world.getBlockState(ringPos).getBlock();
        if (debugMerge)
            System.out.printf("SGBaseBlock.unmergeRingBlock: found %s at %s\n", block, ringPos);
        if (block instanceof SGRingBlock) {
            ((SGRingBlock)block).unmergeFrom(world, ringPos, pos);
        }
    }
    
    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }
    
    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return getWeakPower(state, world, pos, side);
    }
    
    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        SGBaseTE te = getTileEntity(world, pos);
        if (te.allowRedstoneOutput) {
            return (te != null && te.state != SGState.Idle) ? 15 : 0;
        } else {
            return 0;
        }
    }
    
    protected static Trans3 itemTrans = Trans3.sideTurn(0, 2);
    
    @Override
    public Trans3 itemTransformation() {
        return itemTrans;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
    
}
