//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate block
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.block;

import gcewing.sg.BaseBlock;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.interfaces.ISGBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;


public abstract class SGBlock<TE extends TileEntity> extends BaseBlock<TE> implements ISGBlock {

    public SGBlock(Material material, Class<TE> teClass) {
        super(material, teClass);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {

        if(requiresAdminToBreak(world,pos) && ! isOperator(player)) {
            return false; //Prevents the gate from being broken by non-admin when the requiresAdminToBreak field is true
        }

        if (!player.capabilities.isCreativeMode) {
            if (!canPlayerBreakGeneratedGates(world, pos)) {
                return false;  // Prevents gates from being broken when we don't want them to be.
            }
        }

        if (player.capabilities.isCreativeMode && isConnected(world, pos)) {
            if (world.isRemote)
                SGBaseTE.sendErrorMsg(player, "disconnectFirst");
            return false;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if(takesBlastDamage(world,pos)) {
            super.onBlockExploded(world, pos, explosion);
        }
        return;
    }

    boolean isConnected(World world, BlockPos pos) {
        SGBaseTE bte = getBaseTE(world, pos);
        return bte != null && bte.isConnected();
    }

    boolean canPlayerBreakGeneratedGates(World world, BlockPos pos) {
        SGBaseTE bte = getBaseTE(world, pos);
        if (bte != null) {
            return bte.canPlayerBreakGate;
        }
        return true;
    }

    boolean takesBlastDamage(World world, BlockPos pos) {
        SGBaseTE bte = getBaseTE(world, pos);
        if(bte != null) {
            return bte.takesBlastDamage;
        }
        return true;
    }

    boolean requiresAdminToBreak(World world, BlockPos pos) {
        SGBaseTE bte = getBaseTE(world, pos);
        if(bte != null) {
            return bte.requiresAdminToBreak;
        }
        return false;
    }

    private boolean isOperator(EntityPlayer player) {
        return player.canUseCommand(4, "")  ||
                player.canUseCommand(3, "") ||
                player.canUseCommand(2, "") ||
                player.canUseCommand(1, "");
    }
}
