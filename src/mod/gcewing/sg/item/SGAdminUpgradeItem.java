package gcewing.sg.item;

import gcewing.sg.interfaces.ISGBlock;
import gcewing.sg.tileentity.SGBaseTE;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SGAdminUpgradeItem extends Item {
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        Block block = worldIn.getBlockState(pos).getBlock();
        if(!(block instanceof ISGBlock)) {
            return EnumActionResult.FAIL;
        }

        SGBaseTE SGBaseTE = ((ISGBlock) block).getBaseTE(worldIn,pos);
        if(SGBaseTE == null) {
            return EnumActionResult.FAIL;
        }

        return SGBaseTE.applyAdminUpgrade(player.getHeldItem(hand), player);
    }
}
