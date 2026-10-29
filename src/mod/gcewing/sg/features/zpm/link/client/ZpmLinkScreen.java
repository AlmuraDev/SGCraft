package gcewing.sg.features.zpm.link.client;

import gcewing.sg.features.zpm.link.ZpmLinkNetworkHandler;
import gcewing.sg.util.SGAddressing;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;

// A ZPM device's link screen (clicking it with the Configurator): the stargate it's linked to, where that is, and a
// button to unlink it
public class ZpmLinkScreen extends BasicScreen {

    // What the server sends of the device and its link
    public static class Status {
        public BlockPos pos;
        public String deviceName = "";
        public boolean linked, gateFound;
        public BlockPos gatePos;
        public String gateAddress = "", worldName = "";
    }

    private Status status;
    private UILabel deviceLabel, linkLabel, whereLabel, distanceLabel;
    private UIButton unlinkButton;
    private int lastUpdate = 0;
    private boolean unlockMouse = true;

    private ZpmLinkScreen(Status status) {
        this.status = status;
    }

    // Opens the screen, or brings one already open on that device up to date (after unlinking)
    public static void show(Status status) {
        if (Minecraft.getMinecraft().currentScreen instanceof ZpmLinkScreen) {
            ZpmLinkScreen screen = (ZpmLinkScreen) Minecraft.getMinecraft().currentScreen;
            if (screen.status.pos.equals(status.pos)) {
                screen.status = status;
                screen.refresh();
                return;
            }
        }
        new ZpmLinkScreen(status).display();
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;

        final BasicForm form = new BasicForm(250, 108, TextFormatting.WHITE + I18n.format("sgcraft.gui.zpmLink.label.title"));
        form.setMovable(true);
        form.setBackgroundAlpha(255);

        this.deviceLabel = label(2);
        this.linkLabel = label(14);
        this.whereLabel = label(26);
        this.distanceLabel = label(38);
        // How to link it, on two lines (one runs past the window)
        final UILabel hint = hintLine("sgcraft.gui.zpmLink.label.hint1", 52);
        final UILabel hint2 = hintLine("sgcraft.gui.zpmLink.label.hint2", 61);

        this.unlinkButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .position(-45, 0)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.zpmLink.button.unlink"))
            .onClick(this::unlink)
            .listener(this)
            .build("button.zpmLink.unlink");
        final UIButton closeButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.close"))
            .onClick(this::close)
            .listener(this)
            .build("button.zpmLink.close");

        form.add(this.deviceLabel, this.linkLabel, this.whereLabel, this.distanceLabel, hint, hint2, this.unlinkButton, closeButton);
        addToScreen(form);
        refresh();
    }

    // Shows it unlinked at once; the server then sends what it really is (still linked, if it couldn't unlink it)
    private void unlink() {
        ZpmLinkNetworkHandler.sendUnlinkToServer(this.status.pos);
        Status unlinked = new Status();
        unlinked.pos = this.status.pos;
        unlinked.deviceName = this.status.deviceName;
        this.status = unlinked;
        refresh();
    }

    private UILabel hintLine(String key, int y) {
        final UILabel label = new UILabel(TextFormatting.GRAY + I18n.format(key));
        label.setFontOptions(FontOptions.builder().from(FontColors.GRAY_FO).scale(0.8F).build());
        label.setPosition(5, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    private UILabel label(int y) {
        final UILabel label = new UILabel("");
        label.setPosition(5, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    private void refresh() {
        BlockPos p = this.status.pos;
        this.deviceLabel.setText(TextFormatting.WHITE + I18n.format(this.status.deviceName) + TextFormatting.GRAY + " "
            + I18n.format("sgcraft.gui.zpmLink.label.at", p.getX(), p.getY(), p.getZ()));
        if (!this.status.linked) {
            this.linkLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.zpmLink.label.notLinked"));
            this.whereLabel.setText("");
            this.distanceLabel.setText("");
        } else {
            String gate;
            if (!this.status.gateFound) {
                gate = TextFormatting.RED + I18n.format("sgcraft.gui.zpmLink.label.gateGone");
            } else if (this.status.gateAddress.length() == SGAddressing.maxAddressLength) {
                gate = TextFormatting.AQUA + SGAddressing.formatAddress(this.status.gateAddress, "-", "-");
            } else {
                gate = TextFormatting.AQUA + I18n.format("sgcraft.gui.zpmLink.label.stargate");
            }
            this.linkLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.zpmLink.label.linkedTo") + ": " + gate);
            BlockPos g = this.status.gatePos;
            this.whereLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.zpmLink.label.where") + ": " + TextFormatting.WHITE
                + g.getX() + ", " + g.getY() + ", " + g.getZ() + TextFormatting.GRAY + " " + I18n.format("sgcraft.gui.zpmLink.label.in")
                + " " + TextFormatting.WHITE + this.status.worldName);
            this.distanceLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.zpmLink.label.distance") + ": " + TextFormatting.WHITE
                + I18n.format("sgcraft.gui.zpmLink.label.blocks", (int) Math.round(Math.sqrt(g.distanceSq(p)))));
        }
        this.unlinkButton.setEnabled(this.status.linked);
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
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
