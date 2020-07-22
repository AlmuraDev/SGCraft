package gcewing.sg.features.pdd;

import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;

import gcewing.sg.SGCraft;
import gcewing.sg.features.pdd.network.PddNetworkHandler;
import gcewing.sg.network.GuiNetworkHandler;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GateUtil;
import gcewing.sg.util.SGState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import javax.annotation.Nullable;

public class PddItem extends Item {

    public PddItem() {}

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, player, tooltip, advanced);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand handIn) {
        if (!worldIn.isRemote && player.getHeldItemMainhand().getItem() == SGCraft.pdd) {
            final ItemStack stack = player.getHeldItemMainhand();

            NBTTagCompound compound = stack.getTagCompound();
            if (compound == null) {
                compound = new NBTTagCompound();
            }

            if (!compound.hasKey(AddressData.ADDRESSES)) {
                List<AddressData> genericAddressList = AddressNameRegistry.getDefaultPDDEntries();
                AddressData.writeAddresses(compound, genericAddressList);
                stack.setTagCompound(compound);
                player.inventoryContainer.detectAndSendChanges();
            }

            TileEntity localGateTE = GateUtil.locateLocalGate(worldIn, new BlockPos(player.posX, player.posY, player.posZ), SGCraft.toolSearchRange, true);

            if (localGateTE instanceof SGBaseTE) {
                SGBaseTE localGate = (SGBaseTE) localGateTE;

                boolean canEditLocal = localGate.allowGateAccess(player.getName());
                boolean canEditRemote = false;
                if (localGate.isConnected() && localGate.state == SGState.Connected) {
                    SGBaseTE remoteGate = localGate.getConnectedStargateTE();
                    if (remoteGate != null) {
                        canEditRemote = remoteGate.allowGateAccess(player.getName());
                    }
                }

                boolean isPermissionsAdmin = SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin"); // Fallback for a full permissions system override to the Access System

                if (SGCraft.hasPermission(player, "sgcraft.gui.pdd")) {
                    if (player.isSneaking()) {
                        if (localGate.displayGateAddress) {
                            final List<AddressData> addresses = AddressData.getAddresses(compound);
                            String localGateAddress = localGate.homeAddress.toUpperCase().replace("-", "");
                            if (addresses.stream().noneMatch(data -> data.getAddress().replaceAll("-", "").equalsIgnoreCase(localGateAddress))) {
                                PddNetworkHandler.addPddEntryFromServer(player, localGateAddress, addresses.size() + 10);
                            } else {
                                sendErrorMsg(player, "pddContainsAddress");
                            }
                        } else {
                            sendErrorMsg(player, "gateAddressHidden");
                        }
                    } else {
                        if (localGate.allowGateAccess(player.getName()) || isPermissionsAdmin){
                            GuiNetworkHandler.openGuiAtClient(localGate, player, 3, isPermissionsAdmin, canEditLocal, canEditRemote);
                        } else {
                            if (!SGCraft.hasPermission(player, "sgcraft.gui.pdd"))
                                sendErrorMsg(player, "pddPermission");
                            if (!localGate.allowGateAccess(player.getName()))
                                sendErrorMsg(player, "insufficientPlayerAccessPermission");
                        }
                    }
                } else {
                    sendErrorMsg(player, "pddPermission");
                }

                return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(handIn));  //Both Server & Client expect a returned value.
            } else {
                sendErrorMsg(player, "cantFindStargate");
                return new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(handIn));
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(handIn));  //Both Server & Client expect a returned value.
    }
}
