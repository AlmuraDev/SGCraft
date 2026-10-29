package gcewing.sg.features.gdo.client.gui;

import com.google.common.eventbus.Subscribe;
import gcewing.sg.client.renderer.SGBaseTERenderer;
import gcewing.sg.features.gdo.network.GdoNetworkHandler;
import gcewing.sg.util.GateShape;
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
import net.malisis.core.client.gui.render.GuiIcon;
import net.malisis.core.client.gui.render.GuiTexture;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.decoration.UIImage;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

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
    private IrisState lastIrisState;
    private SGState lastGateState;

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
        this.form = new BasicForm(400, 225, "");
        this.form.setAnchor(Anchor.CENTER | Anchor.MIDDLE);
        this.form.setMovable(true);
        this.form.setClosable(true);
        this.form.setBorder(FontColors.WHITE, 1, 185);
        this.form.setBackgroundAlpha(215);
        this.form.setBottomPadding(3);
        this.form.setRightPadding(3);
        this.form.setTopPadding(20);
        this.form.setLeftPadding(3);

        final UILabel titleLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.gdo.label.gdoController"));
        titleLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        titleLabel.setPosition(0, -15, Anchor.CENTER | Anchor.TOP);
        // ****************************************************************************************************************************
        // Local Gate Control
        // ****************************************************************************************************************************
        localGateControlArea = new BasicForm(195, 185, "");
        localGateControlArea.setPosition(0, 0, Anchor.LEFT | Anchor.MIDDLE);
        localGateControlArea.setMovable(false);
        localGateControlArea.setClosable(false);
        localGateControlArea.setBorder(FontColors.WHITE, 1, 185);
        localGateControlArea.setBackgroundAlpha(215);
        localGateControlArea.setBottomPadding(3);
        localGateControlArea.setRightPadding(3);
        localGateControlArea.setTopPadding(3);
        localGateControlArea.setLeftPadding(3);

        final UILabel localGateControlLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.gdo.label.localGateControl"));
        localGateControlLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        localGateControlLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        localGateImage = new UIImage(wholeTexture(SGCraft.mod.resourceLocation("textures/gui_image1.png")));
        localGateImage.setSize(110, 110);
        localGateImage.setPosition(0, 20, Anchor.TOP | Anchor.CENTER);

        localGateAddressLabel = new UILabel("gateAddress");
        localGateAddressLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.3F).build());
        localGateAddressLabel.setPosition(0, -30, Anchor.CENTER | Anchor.BOTTOM);

        localIrisOpenButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.openIris"))
            .enabled(this.canAccessLocal)
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 1);
            })
            .listener(this)
            .build("button.local.iris.open");

        localGateCloseButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.CENTER)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.disconnect"))
            .visible(false) // until refresh finds the gate connected
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 3);
            })
            .listener(this)
            .build("button.local.gate.disconnect");

        localIrisCloseButton = new UIButtonBuilder()
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
        remoteGateControlArea = new BasicForm(195, 185, "");
        remoteGateControlArea.setPosition(0, 0, Anchor.RIGHT | Anchor.MIDDLE);
        remoteGateControlArea.setMovable(false);
        remoteGateControlArea.setClosable(false);
        remoteGateControlArea.setBorder(FontColors.WHITE, 1, 185);
        remoteGateControlArea.setBackgroundAlpha(215);
        remoteGateControlArea.setBottomPadding(3);
        remoteGateControlArea.setRightPadding(3);
        remoteGateControlArea.setTopPadding(3);
        remoteGateControlArea.setLeftPadding(3);

        final UILabel remoteGateControlLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.gdo.label.remoteGateControl"));
        remoteGateControlLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        remoteGateControlLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        remoteGateImage = new UIImage(wholeTexture(SGCraft.mod.resourceLocation("textures/gui_image1.png")));  // Fake Image
        remoteGateImage.setSize(110, 110);
        remoteGateImage.setPosition(0, 20, Anchor.TOP | Anchor.CENTER);

        remoteGateAddressLabel = new UILabel("gateAddress"); // Incorrect Value
        remoteGateAddressLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.3F).build());
        remoteGateAddressLabel.setPosition(0, -30, Anchor.CENTER | Anchor.BOTTOM);

        remoteIrisOpenButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.openIris"))
            .enabled(this.canAccessRemote)
            .onClick(() -> {
                GdoNetworkHandler.sendGdoInputToServer((SGBaseTE)localGate, 4);
            })
            .listener(this)
            .build("button.remote.iris.open");

        remoteGateCloseButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.CENTER)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.disconnect"))
            .visible(false)
            .onClick(() -> {
                // Do Nothing at the moment
            })
            .listener(this)
            .build("button.remote.gate.disconnect");

        remoteIrisCloseButton = new UIButtonBuilder()
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
        final UIButton buttonClose = new UIButtonBuilder()
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
    private GuiIcon gateImage(int gateType, boolean connected, boolean hasIrisUpgrade, boolean irisClosed) {
        String iris = !hasIrisUpgrade ? "no_iris" : irisClosed ? "iris_closed" : "iris_open";
        // Universe and Ori gates have no pictures of their own, so show as Milky Way gates
        GateType type = GateType.fromId(gateType);
        String name = (type == GateType.PEGASUS ? type : GateType.MILKY_WAY).assetName + (connected ? "_connected_" : "_disconnected_") + iris;
        return wholeTexture(SGCraft.mod.resourceLocation("textures/" + name + ".png"));
    }

    // A gate with a shield in place of the iris has its buttons say so
    private void updateIrisButtonLabels() {
        boolean localShield = localGate != null && localGate.hasShield;
        boolean remoteShield = remoteGateView != null && remoteGateView.hasShield;
        setButtonLabel(this.localIrisOpenButton, localShield ? "sgcraft.gui.button.deactivateShield" : "sgcraft.gui.button.openIris");
        setButtonLabel(this.localIrisCloseButton, localShield ? "sgcraft.gui.button.activateShield" : "sgcraft.gui.button.closeIris");
        setButtonLabel(this.remoteIrisOpenButton, remoteShield ? "sgcraft.gui.button.deactivateShield" : "sgcraft.gui.button.openIris");
        setButtonLabel(this.remoteIrisCloseButton, remoteShield ? "sgcraft.gui.button.activateShield" : "sgcraft.gui.button.closeIris");
        // Each panel's two side buttons as wide as each other, so Disconnect is in the middle of the gap between them
        // ("Deactivate Shield" is longer than "Activate Shield")
        matchWidths(this.localIrisOpenButton, this.localIrisCloseButton);
        matchWidths(this.remoteIrisOpenButton, this.remoteIrisCloseButton);
    }

    private static void matchWidths(UIButton a, UIButton b) {
        int width = Math.max(a.getWidth(), b.getWidth());
        if (a.getWidth() != width) {
            a.setSize(width, a.getHeight());
        }
        if (b.getWidth() != width) {
            b.setSize(width, b.getHeight());
        }
    }

    private void setButtonLabel(UIButton button, String key) {
        // The button keeps the key of its label (MalisisCore 6.6 buttons have no getText)
        if (!key.equals(button.getData())) {
            button.attachData(key);
            button.setText(TextFormatting.WHITE + I18n.format(key));
            button.setSize(40, button.getHeight()); // grows to fit the text, and back down when it's shorter again
        }
    }

    // A whole image as an icon: the texture size only converts pixels to UVs, unused for 0 to 1
    private static GuiIcon wholeTexture(ResourceLocation location) {
        return new GuiIcon(new GuiTexture(location, 1, 1), 0F, 0F, 1F, 1F);
    }

    // The panels are 195 wide, or as wide as their three buttons need (the shield's labels are longer than the
    // iris's), both the same width; the window holds one panel, or both side by side
    private void fitControlPanels(boolean showRemote) {
        int width = Math.max(195, Math.max(buttonRowWidth(localIrisOpenButton, localGateCloseButton, localIrisCloseButton),
            showRemote ? buttonRowWidth(remoteIrisOpenButton, remoteGateCloseButton, remoteIrisCloseButton) : 0));
        this.localGateControlArea.setWidth(width);
        this.remoteGateControlArea.setWidth(width);
        this.form.setWidth(showRemote ? 2 * width + 10 : width + 5);
    }

    // Room for a row of buttons at the left, middle and right, with gaps between and the panel's padding
    private static int buttonRowWidth(UIButton left, UIButton middle, UIButton right) {
        return Math.max(left.getWidth(), right.getWidth()) * 2 + middle.getWidth() + 2 * 6 + 6;
    }

    private void refresh() {
        updateIrisButtonLabels();

        if (localGate != null) {
            if (localGate.isMerged) {
                this.localGateAddressLabel.setText(SGAddressing.formatAddress(localGate.homeAddress, "-", "-"));
            }

            if (liveGateRenderer() != null) {
                // drawScreen draws the live gate here instead; the image just holds its place
                this.localGateImage.setVisible(false);
            } else {
                this.localGateImage.setIcon(gateImage(localGate.gateType, localGate.isConnected(), localGate.hasIrisUpgrade, localGate.irisIsClosed()));
            }

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
            // Disconnect is only shown while the gate is connected
            this.localGateCloseButton.setVisible(localGate.isConnected() && localGate.state != SGState.Dialling
                && localGate.state != SGState.Disconnecting);
            if (!localGate.isConnected() || localGate.state == SGState.Dialling || localGate.state == SGState.Disconnecting) {
                this.remoteGateImage.setVisible(false);
                this.localGateCloseButton.setEnabled(false);
                this.remoteGateCloseButton.setEnabled(false);
                this.remoteIrisOpenButton.setEnabled(false);
                this.remoteIrisCloseButton.setEnabled(false);
                fitControlPanels(false);
                this.remoteGateControlArea.setVisible(false);

            } else if (isRemoteConnected) {
                fitControlPanels(true);
                this.remoteGateControlArea.setVisible(true);
                this.remoteGateAddressLabel.setText(r_address);

                // The remote gate is always shown connected
                this.remoteGateImage.setIcon(gateImage(r_gateType, true, r_hasIrisUpgrade, r_isIrisClosed));

                // The live copy of the remote gate is updated twice a second, far sooner than the timed refresh that
                // sets r_hasIrisUpgrade and r_isIrisClosed, so its iris decides the buttons once it has arrived
                if (remoteGateView != null) {
                    r_hasIrisUpgrade = remoteGateView.hasIrisUpgrade;
                    r_isIrisClosed = remoteGateView.irisIsClosed();
                }
                boolean remoteIrisMoving = remoteGateView != null
                    && (remoteGateView.irisState == IrisState.Opening || remoteGateView.irisState == IrisState.Closing);

                if (!r_hasIrisUpgrade || remoteIrisMoving) {
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
                // The picture only stands in when the live gate can't be drawn; otherwise the panel stays empty until the remote
                // gate's data first arrives, rather than flashing the picture
                this.remoteGateImage.setVisible(liveGateRenderer() == null);
                this.localGateCloseButton.setEnabled(true);
                this.remoteGateCloseButton.setEnabled(true);
            }
        }

    }

    @Override
    public void update() {
        super.update();
        if (unlockMouse && this.lastUpdate == 25) {
            Mouse.setGrabbed(false); // Force the mouse to be visible even though Mouse.isGrabbed() is false.  //#BugsUnited.
            unlockMouse = false; // Only unlock once per session.
        }

        if (!localGate.isMerged)
            this.close();

        // While connected, ask for the remote gate's latest state twice a second; it ticks itself in between
        if (this.form != null && localGate.isMerged && localGate.isConnected() && isRemoteConnected) {
            long now = world.getTotalWorldTime();
            if (now - lastRemoteGateRequest >= 10 || now < lastRemoteGateRequest) {
                lastRemoteGateRequest = now;
                GdoNetworkHandler.requestRemoteGateFromServer(localGate);
            }
        } else {
            remoteGateView = null;
        }

        // Update the buttons as soon as the gate's iris or connection changes, rather than waiting for the next timed refresh
        if (this.form != null && localGate.isMerged && (localGate.irisState != this.lastIrisState || localGate.state != this.lastGateState)) {
            this.lastIrisState = localGate.irisState;
            this.lastGateState = localGate.state;
            this.refresh();
        }

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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (liveGateRenderer() != null && localGate != null && localGate.isMerged && localGateImage != null) {
            drawLiveGate(localGate, localGateImage, partialTicks);
        }
        if (liveGateRenderer() != null && remoteGateView != null && remoteGateControlArea != null && remoteGateControlArea.isVisible()) {
            // Animate it a tick at a time, as the world does the local gate
            long now = world.getTotalWorldTime();
            for (int i = 0; i < 20 && remoteGateViewTick < now; i++, remoteGateViewTick++) {
                remoteGateView.update();
            }
            remoteGateViewTick = now;
            drawLiveGate(remoteGateView, remoteGateImage, partialTicks);
        }
    }

    // The far end of the connection, which this client usually can't see, kept up to date from the server while
    // the remote panel shows (see SGBaseTE.remoteView)
    private SGBaseTE remoteGateView;
    private long remoteGateViewTick, lastRemoteGateRequest;
    private IrisState lastRemoteIrisState;

    public void updateRemoteGate(BlockPos localPos, BlockPos remotePos, NBTTagCompound nbt) {
        if (localGate == null || !localPos.equals(localGate.getPos())) {
            return;
        }
        if (remoteGateView == null || !remotePos.equals(remoteGateView.getPos())) {
            remoteGateView = SGBaseTE.remoteView(world, remotePos);
            remoteGateViewTick = world.getTotalWorldTime();
        }
        IrisState oldIrisState = remoteGateView.irisState;
        remoteGateView.applyRemoteState(nbt);
        if (remoteGateView.irisState != oldIrisState || remoteGateView.irisState != lastRemoteIrisState) {
            lastRemoteIrisState = remoteGateView.irisState;
            refresh(); // update the remote iris buttons as soon as its iris starts or stops moving
        }
        if (liveGateRenderer() != null) {
            remoteGateImage.setVisible(false); // drawn live instead
        }
    }

    // The renderer that draws gates in the world, used to show the local gate as it really is: ring
    // spinning, chevrons locking, event horizon forming and the iris opening and closing
    private static SGBaseTERenderer liveGateRenderer() {
        TileEntitySpecialRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(SGBaseTE.class);
        return renderer instanceof SGBaseTERenderer ? (SGBaseTERenderer) renderer : null;
    }

    // Draws the gate face-on, filling the given component's area
    private void drawLiveGate(SGBaseTE gate, UIImage area, float partialTicks) {
        GateShape shape = gate.getShape();
        double scale = Math.min(area.getWidth(), area.getHeight()) / 2.0 / shape.ringOuterRadius;

        GlStateManager.pushMatrix();
        GlStateManager.translate(area.screenX() + area.getWidth() / 2.0, area.screenY() + area.getHeight() / 2.0, 200);
        GlStateManager.scale(scale, -scale, scale); // GUI y runs down, the gate's y up
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT); // the ring hides the iris behind it
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
        mc.entityRenderer.enableLightmap();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F); // full brightness

        liveGateRenderer().renderGate(gate, shape, partialTicks);

        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        super.keyTyped(keyChar, keyCode);
        this.lastUpdate = 0; // Reset the timer when key is typed.
    }

    @Override
    protected void mousePressed(final UIComponent component, final int button) {
        super.mousePressed(component, button);
        this.lastUpdate = 0; // Reset the timer when mouse is pressed.
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // Can't stop the game otherwise the Sponge Scheduler also stops.
    }
}
