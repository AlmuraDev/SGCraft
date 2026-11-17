package gcewing.sg.features.configurator;

import static gcewing.sg.tileentity.SGBaseTE.sendBasicMsg;
import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;

import gcewing.sg.SGCraft;
import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GateUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public class ConfiguratorItem extends Item {

    // The gate each player last sneak-clicked, for the next DHD they sneak-click to be linked to: by player (an item is
    // one object for every Configurator on the server, so a field on it was one gate for everyone), and by place, the gate
    // itself perhaps unloaded and loaded again in between
    private static final Map<UUID, BlockPos> toLink = new HashMap<>();
    private static final Map<UUID, Integer> toLinkDimension = new HashMap<>();

  public ConfiguratorItem() {}

  @SideOnly(Side.CLIENT)
  @Override
  public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
    super.addInformation(stack, player, tooltip, advanced);
  }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
      if (!world.isRemote && player.isSneaking()) {
            TileEntity te = world.getTileEntity(pos);
            UUID id = player.getUniqueID();
            if (te instanceof DHDTE) {
                BlockPos gatePos = toLink.remove(id);
                Integer dimension = toLinkDimension.remove(id);
                // A gate in this DHD's world, still there, that the player is still an admin of
                SGBaseTE gate = gatePos != null && dimension != null && dimension == world.provider.getDimension()
                    && world.isBlockLoaded(gatePos) ? SGBaseTE.at(world, gatePos) : null;
                if (gate != null && gate.allowAdminAccess(player)) {
                    ((DHDTE) te).linkToStargate(gate);
                    sendBasicMsg(player, "configuratorLink");
                } else if (gatePos != null) {
                    sendErrorMsg(player, "cantFindStargate");
                }
            }
            if (te instanceof SGBaseTE) {
                // Its admins only: a DHD linked to it works it
                if (!((SGBaseTE) te).allowAdminAccess(player)) {
                    sendErrorMsg(player, "insufficientPlayerAdminAccessPermission");
                    return EnumActionResult.PASS;
                }
                toLink.put(id, pos.toImmutable());
                toLinkDimension.put(id, world.provider.getDimension());
                sendBasicMsg(player, "gateLocationSaved");
            }
        }

        return EnumActionResult.PASS;
    }


  @Override
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer player, EnumHand handIn) {
      if (!worldIn.isRemote && !player.isSneaking()) {
          TileEntity localGateTE = GateUtil.locateGateFor(worldIn, player, SGCraft.toolSearchRange);

          if (localGateTE instanceof SGBaseTE) {
              SGBaseTE localGate = (SGBaseTE) localGateTE;

              if (localGate.homeAddress == null) {
                  // Fallback since the Gate doesn't have an established address until its merged.
                  return new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(handIn));
              }

              boolean isPermissionsAdmin = SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin"); // Fallback for a full permissions system override to the Access System

              if (SGCraft.hasPermission(player, "sgcraft.gui.configurator") && localGate.allowAdminAccess(player) || isPermissionsAdmin) {
                  // The lists for the gate and player access screens; clients aren't otherwise sent them
                  ConfiguratorNetworkHandler.sendAccessListsToClient(player, localGate);
                  ConfiguratorNetworkHandler.openGuiAtClient(localGate, player, 1, isPermissionsAdmin,
                      SGCraft.hasPermission(player, "sgcraft.configurator.secondsToStayOpen"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.ringRotationSpeed"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.maxEnergyBuffer"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.energyPerFuelItem"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.gateOpeningsPerFuelItem"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.distanceFactorMultiplier"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.interDimensionalMultiplier"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.oneWayTravel"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.hasIrisUpgrade"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.hasChevronUpgrade"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.gateType"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.reverseWormholeKills"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.closeFromEitherEnd"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.preserveInventory"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.noPowerRequired"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.chevronsLockOnDial"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.returnToPreviousIrisState"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.transientDamage"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.transparency"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.useDHDFuelSource"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.allowRedstoneOutput"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.allowRedstoneInput"),
                      SGCraft.hasPermission(player, "sgcraft.gui.gateAccessMenu") && localGate.allowAdminAccess(player),
                      SGCraft.hasPermission(player, "sgcraft.gui.playerAccessMenu") && localGate.allowAdminAccess(player),
                      SGCraft.hasPermission(player, "sgcraft.configurator.canPlayerBreakPlayerGate"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.canPlayerBreakGeneratedGate"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.displayGateAddress"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.isGenerated"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.invulnerable"),
                      SGCraft.hasPermission(player, "sgcraft.configurator.dynmap"));

              } else {
                  if (!SGCraft.hasPermission(player, "sgcraft.gui.configurator"))
                      sendErrorMsg(player, "configuratorPermission");
                  if (!localGate.allowAdminAccess(player))
                      sendErrorMsg(player, "insufficientPlayerAdminAccessPermission");

                  return new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(handIn));
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
