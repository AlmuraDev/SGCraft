//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate controller fuelling gui screen
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.features.ic2.zpm.modulehub;

import gcewing.sg.BaseMod;
import gcewing.sg.SGCraft;
import gcewing.sg.client.gui.SGScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.text.DecimalFormat;

public class ZpmHubScreen extends SGScreen {

    //static String screenTitle = I18n.format("tile.sgcraft:zpm_interface_cart.name");
    static String screenTitle = "ZPM Hub";
    static final int guiWidth = 256;
    static final int guiHeight = 208;
    final static DecimalFormat dFormat = new DecimalFormat("###,###,###,##0");

    ZpmHubTE te;

    public static ZpmHubScreen create(EntityPlayer player, World world, BlockPos pos) {
        ZpmHubTE te = ZpmHubTE.at(world, pos);
        if (te != null)
            return new ZpmHubScreen(player, te);
        else
            return null;
    }

    public ZpmHubScreen(EntityPlayer player, ZpmHubTE te) {
        super(new ZpmHubContainer(player, te), guiWidth, guiHeight);
        this.te = te;
    }
    
    @Override
    protected void drawBackgroundLayer() {
        bindTexture(SGCraft.mod.resourceLocation("textures/gui/zpm_hub_gui.png"), 256, 256);
        drawTexturedRect(0, 0, guiWidth, guiHeight, 0, 0);

        int cx = xSize / 2;
        setTextColor(0x004c66);
        drawCenteredString(screenTitle, cx, 8);
        drawString("ZPM Slots", 123, 42);
        // The tier only means anything on the IC2 energy network
        if (BaseMod.isModLoaded("ic2") && te.getZpmSlotsloaded() > 0) {
            drawString("IC2 Power Tier: " + dFormat.format(te.getZpmSlotsloaded() + 2), 60, 90);
        }
        if (te.getZpmSlotsloaded() > 1) {
            drawString("Optimization factor: x" + dFormat.format(te.getZpmSlotsloaded()), 60, 100);
        }
        drawString(I18n.format("sgcraft.gui.zpmConsole.label.availablePower")+": " + dFormat.format(te.storedZpmEnergy()), 60, 110);
    }
}
