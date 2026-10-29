package gcewing.sg.features.shield.client.gui;

import gcewing.sg.features.shield.network.ShieldConsoleNetworkHandler;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.IrisState;
import gcewing.sg.util.SGAddressing;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;

// The Shield Console's screen: the gate it works, whether its shield is up, and buttons to raise or lower it
public class ShieldConsoleScreen extends BasicScreen {

    private final EntityPlayer player;
    private final BlockPos consolePos;
    private final BlockPos gatePos; // null when no gate with a shield is in range
    private final boolean canControl;
    private final int range;
    private UILabel addressLabel, statusLabel;
    private UIButton activateButton, deactivateButton;
    private int lastUpdate = 0;
    private boolean unlockMouse = true;

    public ShieldConsoleScreen(EntityPlayer player, BlockPos consolePos, BlockPos gatePos, boolean canControl, int range) {
        this.player = player;
        this.consolePos = consolePos;
        this.gatePos = gatePos;
        this.canControl = canControl;
        this.range = range;
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;

        final BasicForm form = new BasicForm(230, 105, "");
        form.setAnchor(Anchor.CENTER | Anchor.MIDDLE);
        form.setMovable(true);
        form.setClosable(true);
        form.setBorder(FontColors.WHITE, 1, 185);
        form.setBackgroundAlpha(215);
        form.setBottomPadding(3);
        form.setRightPadding(3);
        form.setTopPadding(20);
        form.setLeftPadding(3);

        final UILabel titleLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.shieldconsole.label.title"));
        titleLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        titleLabel.setPosition(0, -15, Anchor.CENTER | Anchor.TOP);

        this.addressLabel = new UILabel("");
        this.addressLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.3F).build());
        this.addressLabel.setPosition(0, 5, Anchor.CENTER | Anchor.TOP);

        this.statusLabel = new UILabel("");
        this.statusLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        this.statusLabel.setPosition(0, 30, Anchor.CENTER | Anchor.TOP);

        this.activateButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.activateShield"))
            .onClick(() -> ShieldConsoleNetworkHandler.sendShieldInputToServer(this.consolePos, this.gatePos, true))
            .listener(this)
            .build("button.shield.activate");

        this.deactivateButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.deactivateShield"))
            .onClick(() -> ShieldConsoleNetworkHandler.sendShieldInputToServer(this.consolePos, this.gatePos, false))
            .listener(this)
            .build("button.shield.deactivate");

        form.add(titleLabel, this.addressLabel, this.statusLabel, this.activateButton, this.deactivateButton);
        addToScreen(form);
        refresh();
    }

    // The gate, as this client has it, if it's still there with its shield
    private SGBaseTE gate() {
        if (this.gatePos == null) {
            return null;
        }
        TileEntity te = this.player.world.getTileEntity(this.gatePos);
        if (te instanceof SGBaseTE) {
            SGBaseTE gate = (SGBaseTE) te;
            if (gate.isMerged && gate.hasIrisUpgrade && gate.hasShield) {
                return gate;
            }
        }
        return null;
    }

    private void refresh() {
        SGBaseTE gate = gate();
        if (gate == null) {
            this.addressLabel.setText("");
            this.statusLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.shieldconsole.label.noGate", this.range));
            this.activateButton.setEnabled(false);
            this.deactivateButton.setEnabled(false);
            return;
        }

        this.addressLabel.setText(gate.homeAddress != null && gate.displayGateAddress
            ? SGAddressing.formatAddress(gate.homeAddress, "-", "-") : "");

        IrisState state = gate.irisState;
        if (!this.canControl) {
            this.statusLabel.setText(TextFormatting.RED + I18n.format("sgcraft.gui.shieldconsole.label.noAccess"));
        } else if (state == IrisState.Closed) {
            this.statusLabel.setText(TextFormatting.AQUA + I18n.format("sgcraft.gui.shieldconsole.label.active"));
        } else if (state == IrisState.Closing) {
            this.statusLabel.setText(TextFormatting.AQUA + I18n.format("sgcraft.gui.shieldconsole.label.raising"));
        } else if (state == IrisState.Opening) {
            this.statusLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.shieldconsole.label.lowering"));
        } else {
            this.statusLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.shieldconsole.label.inactive"));
        }
        // One button at a time, and neither while the shield is moving
        this.activateButton.setEnabled(this.canControl && state == IrisState.Open);
        this.deactivateButton.setEnabled(this.canControl && state == IrisState.Closed);
    }

    @Override
    public void update() {
        super.update();
        if (this.unlockMouse && this.lastUpdate == 25) {
            Mouse.setGrabbed(false); // Force the mouse to be visible even though Mouse.isGrabbed() is false.  //#BugsUnited.
            this.unlockMouse = false;
        }
        if (++this.lastUpdate > 30) {
            this.lastUpdate = 0;
        }
        refresh(); // the shield's state, as the server sends it
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
