package gcewing.sg.features.gdo.client.gui;

import com.google.common.eventbus.Subscribe;
import gcewing.sg.features.gdo.network.GdoNetworkHandler;
import gcewing.sg.network.GuiNetworkHandler;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.util.GateType;
import gcewing.sg.util.IrisState;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.network.SGChannel;
import gcewing.sg.SGCraft;
import gcewing.sg.util.SGAddressing;
import gcewing.sg.util.SGState;
import gcewing.sg.util.GateUtil;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.GuiTexture;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.decoration.UIImage;
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
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GdoScreen extends BasicScreen {
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private boolean isAdmin;
    private boolean debugScreen = false;
    private BasicForm form, localGateControlArea, remoteGateControlArea;
    private UIButton localIrisOpenButton, localGateCloseButton, localIrisCloseButton, remoteIrisOpenButton, remoteGateCloseButton, remoteIrisCloseButton;
    private UIImage localGateImage, remoteGateImage;
    private UILabel localGateAddressLabel, remoteGateAddressLabel;
    private BlockPos location;
    private BlockPos gatePos;
    private World world;
    private EntityPlayer player;
    public boolean isRemoteConnected, r_hasIrisUpgrade, r_hasChevronUpgrade, r_isIrisClosed, canAccessLocal, canAccessRemote;
    public String r_address;
    public int r_gateType;
    public SGBaseTE localGate;

    public GdoScreen(EntityPlayer player, World worldIn,  BlockPos gatePos, boolean isAdmin, boolean isRemoteConnected, boolean r_hasIrisUpgrade, boolean r_hasChevronUpgrade,
        boolean r_isIrisClosed, int r_gateType, String r_address, boolean canAccessLocal, boolean canAccessRemote) {
        this.player = player;
        this.world = worldIn;
        this.gatePos = gatePos;
        this.isAdmin = isAdmin;
        this.location = new BlockPos(player.posX, player.posY, player.posZ);
        this.isRemoteConnected = isRemoteConnected;
        this.r_hasIrisUpgrade = r_hasIrisUpgrade;
        this.r_hasChevronUpgrade = r_hasChevronUpgrade;
        this.r_isIrisClosed = r_isIrisClosed;
        this.r_gateType = r_gateType;
        this.r_address = r_address;
        this.canAccessLocal = canAccessLocal;
        this.canAccessRemote = canAccessRemote;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void construct() {
        this.guiscreenBackground = false;
        Keyboard.enableRepeatEvents(true);

        TileEntity localGateTE = world.getTileEntity(gatePos);

        if (localGateTE instanceof SGBaseTE) {
            localGate = (SGBaseTE) localGateTE;
            if (!localGate.isMerged) { // Block GDO usage when gate is not merged.
                SGBaseTE.sendGenericErrorMsg(player, "No Local Stargate within range.");
                return;
            }
        } else {
            SGBaseTE.sendGenericErrorMsg(player, "No Local Stargate within range.");
            return;
        }

        // Master Panel
        this.form = new BasicForm(this, 400, 225, "");
        this.form.setAnchor(Anchor.CENTER | Anchor.MIDDLE);
        this.form.setMovable(true);
        this.form.setClosable(true);
        this.form.setBorder(FontColors.WHITE, 1, 185);
        this.form.setBackgroundAlpha(215);
        this.form.setBottomPadding(3);
        this.form.setRightPadding(3);
        this.form.setTopPadding(20);
        this.form.setLeftPadding(3);

        final UILabel titleLabel = new UILabel(this, TextFormatting.WHITE + I18n.format("sgcraft.gui.gdo.label.gdoController"));
        titleLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        titleLabel.setPosition(0, -15, Anchor.CENTER | Anchor.TOP);
        // ****************************************************************************************************************************
        // Local Gate Control
        // ****************************************************************************************************************************
        localGateControlArea = new BasicForm(this, 195, 185, "");
        localGateControlArea.setPosition(0, 0, Anchor.LEFT | Anchor.MIDDLE);
        localGateControlArea.setMovable(false);
        localGateControlArea.setClosable(false);
        localGateControlArea.setBorder(FontColors.WHITE, 1, 185);
        localGateControlArea.setBackgroundAlpha(215);
        localGateControlArea.setBottomPadding(3);
        localGateControlArea.setRightPadding(3);
        localGateControlArea.setTopPadding(3);
        localGateControlArea.setLeftPadding(3);

        final UILabel localGateControlLabel = new UILabel(this, TextFormatting.WHITE + I18n.format("sgcraft.gui.gdo.label.localGateControl"));
        localGateControlLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        localGateControlLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        localGateImage = new UIImage(this, new GuiTexture(SGCraft.mod.resourceLocation("textures/gui_image1.png")), null);
        localGateImage.setSize(110, 110);
        localGateImage.setPosition(0, 20, Anchor.TOP | Anchor.CENTER);

        localGateAddressLabel = new UILabel(this, "gateAddress");
        localGateAddressLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.3F).build());
        localGateAddressLabel.setPosition(0, -30, Anchor.CENTER | Anchor.BOTTOM);

        localIrisOpenButton = new UIButtonBuilder(this)
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.openIris"))
            .enabled(this.canAccessLocal)
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 1);
            })
            .listener(this)
            .build("button.local.iris.open");

        localGateCloseButton = new UIButtonBuilder(this)
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.CENTER)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.disconnect"))
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 3);
            })
            .listener(this)
            .build("button.local.gate.disconnect");

        localIrisCloseButton = new UIButtonBuilder(this)
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.closeIris"))
            .enabled(this.canAccessLocal)
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 2);
            })
            .listener(this)
            .build("button.local.iris.close");

        localGateControlArea.add(localGateControlLabel, localGateImage, localGateAddressLabel, localIrisOpenButton, localGateCloseButton, localIrisCloseButton);

        // ****************************************************************************************************************************
        // Remote Gate Control
        // ****************************************************************************************************************************
        remoteGateControlArea = new BasicForm(this, 195, 185, "");
        remoteGateControlArea.setPosition(0, 0, Anchor.RIGHT | Anchor.MIDDLE);
        remoteGateControlArea.setMovable(false);
        remoteGateControlArea.setClosable(false);
        remoteGateControlArea.setBorder(FontColors.WHITE, 1, 185);
        remoteGateControlArea.setBackgroundAlpha(215);
        remoteGateControlArea.setBottomPadding(3);
        remoteGateControlArea.setRightPadding(3);
        remoteGateControlArea.setTopPadding(3);
        remoteGateControlArea.setLeftPadding(3);

        final UILabel remoteGateControlLabel = new UILabel(this, TextFormatting.WHITE + I18n.format("sgcraft.gui.gdo.label.remoteGateControl"));
        remoteGateControlLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        remoteGateControlLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        remoteGateImage = new UIImage(this, new GuiTexture(SGCraft.mod.resourceLocation("textures/gui_image1.png")), null);  // Fake Image
        remoteGateImage.setSize(110, 110);
        remoteGateImage.setPosition(0, 20, Anchor.TOP | Anchor.CENTER);

        remoteGateAddressLabel = new UILabel(this, "gateAddress"); // Incorrect Value
        remoteGateAddressLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.3F).build());
        remoteGateAddressLabel.setPosition(0, -30, Anchor.CENTER | Anchor.BOTTOM);

        remoteIrisOpenButton = new UIButtonBuilder(this)
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.openIris"))
            .enabled(this.canAccessRemote)
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 4);
            })
            .listener(this)
            .build("button.remote.iris.open");

        remoteGateCloseButton = new UIButtonBuilder(this)
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.CENTER)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.disconnect"))
            .visible(false)
            .onClick(() -> {
                // Do Nothing at the moment
            })
            .listener(this)
            .build("button.remote.gate.disconnect");

        remoteIrisCloseButton = new UIButtonBuilder(this)
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.closeIris"))
            .enabled(this.canAccessRemote)
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 5);
            })
            .listener(this)
            .build("button.remote.iris.close");

        remoteGateControlArea.add(remoteGateControlLabel, remoteGateImage, remoteGateAddressLabel, remoteIrisOpenButton, remoteGateCloseButton, remoteIrisCloseButton);

        // ****************************************************************************************************************************

        // Close button
        final UIButton buttonClose = new UIButtonBuilder(this)
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.close"))
            .onClick(() -> {
                this.close();
            })
            .listener(this)
            .build("button.close");

        this.form.add(titleLabel, localGateControlArea, remoteGateControlArea, buttonClose);
        addToScreen(this.form);
        this.refresh();
    }

    // Picture of a gate for the local or remote gate panel
    private GuiTexture gateImage(int gateType, boolean connected, boolean hasIrisUpgrade, boolean irisClosed) {
        String iris = !hasIrisUpgrade ? "no_iris" : irisClosed ? "iris_closed" : "iris_open";
        String name = GateType.fromId(gateType).assetName + (connected ? "_connected_" : "_disconnected_") + iris;
        return new GuiTexture(SGCraft.mod.resourceLocation("textures/" + name + ".png"));
    }

    private void refresh() {

        if (localGate != null) {
            if (localGate.isMerged) {
                this.localGateAddressLabel.setText(SGAddressing.formatAddress(localGate.homeAddress, "-", "-"));
            }

            this.localGateImage.setIcon(gateImage(localGate.gateType, localGate.isConnected(), localGate.hasIrisUpgrade, localGate.irisIsClosed()), null);

            if (!localGate.hasIrisUpgrade) {
                this.localIrisOpenButton.setEnabled(false);
                this.localIrisCloseButton.setEnabled(false);
            } else {
                if (localGate.irisState == IrisState.Closing || localGate.irisState == IrisState.Opening) {
                    this.localIrisOpenButton.setEnabled(false);
                    this.localIrisCloseButton.setEnabled(false);
                } else {
                    if (this.canAccessLocal) {
                        if (localGate.irisIsClosed()) {
                            this.localIrisOpenButton.setEnabled(true);
                            this.localIrisCloseButton.setEnabled(false);
                        } else {
                            this.localIrisOpenButton.setEnabled(false);
                            this.localIrisCloseButton.setEnabled(true);
                        }
                    }
                }
            }
            if (!localGate.isConnected() || localGate.state == SGState.Dialling || localGate.state == SGState.Disconnecting) {
                this.remoteGateImage.setVisible(false);
                this.localGateCloseButton.setEnabled(false);
                this.remoteGateCloseButton.setEnabled(false);
                this.remoteIrisOpenButton.setEnabled(false);
                this.remoteIrisCloseButton.setEnabled(false);
                this.form.setWidth(200);
                this.remoteGateControlArea.setVisible(false);

            } else if (isRemoteConnected) {
                this.form.setWidth(400);
                this.remoteGateControlArea.setVisible(true);
                this.remoteGateAddressLabel.setText(r_address);

                // The remote gate is always shown connected
                this.remoteGateImage.setIcon(gateImage(r_gateType, true, r_hasIrisUpgrade, r_isIrisClosed), null);

                if (!r_hasIrisUpgrade) {
                    this.remoteIrisOpenButton.setEnabled(false);
                    this.remoteIrisCloseButton.setEnabled(false);
                } else {
                    if (this.canAccessRemote) {
                        if (r_isIrisClosed) {
                            this.remoteIrisOpenButton.setEnabled(true);
                            this.remoteIrisCloseButton.setEnabled(false);
                        } else {
                            this.remoteIrisOpenButton.setEnabled(false);
                            this.remoteIrisCloseButton.setEnabled(true);
                        }
                    }
                }
                this.remoteGateImage.setVisible(true);
                this.localGateCloseButton.setEnabled(true);
                this.remoteGateCloseButton.setEnabled(true);
            }
        }

    }

    @Override
    public void update(int mouseX, int mouseY, float partialTick) {
        super.update(mouseX, mouseY, partialTick);
        if (unlockMouse && this.lastUpdate == 25) {
            Mouse.setGrabbed(false); // Force the mouse to be visible even though Mouse.isGrabbed() is false.  //#BugsUnited.
            unlockMouse = false; // Only unlock once per session.
        }

        if (!localGate.isMerged)
            this.close();

        if (this.lastUpdate == 100) {
            if (localGate != null) {
                GuiNetworkHandler.sendGuiRequestToServer((SGBaseTE) localGate, player, 2);
            }
            if (localGate.isMerged) {
                this.refresh();
            }
        }

        if (++this.lastUpdate > 120) {
            this.lastUpdate = 0;
        }
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        super.keyTyped(keyChar, keyCode);
        this.lastUpdate = 0; // Reset the timer when key is typed.
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        this.lastUpdate = 0; // Reset the timer when mouse is pressed.
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // Can't stop the game otherwise the Sponge Scheduler also stops.
    }
}
