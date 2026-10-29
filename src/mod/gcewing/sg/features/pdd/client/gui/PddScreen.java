package gcewing.sg.features.pdd.client.gui;

import com.google.common.eventbus.Subscribe;
import gcewing.sg.SGCraft;
import gcewing.sg.features.pdd.AddressData;
import gcewing.sg.features.pdd.network.PddNetworkHandler;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.tileentity.data.GateAccessData;
import gcewing.sg.util.GateUtil;
import gcewing.sg.util.SGAddressing;
import gcewing.sg.util.SGState;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.BasicContainer;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.container.BasicList;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.decoration.UISeparator;
import net.malisis.core.client.gui.element.size.Size;
import net.malisis.core.client.gui.render.GuiIcon;
import net.malisis.core.client.gui.render.GuiTexture;
import net.malisis.core.client.gui.component.decoration.UIImage;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;
import static org.lwjgl.opengl.GL11.*;

public class PddScreen extends BasicScreen {
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private boolean isAdmin;
    private BasicForm form;
    private BasicContainer<?> addressContainer;
    private UIButton addAddressButton, editAddressButton, deleteAddressButton, buttonReset, closeButton, buttonDial, buttonDisconnect;
    private UILabel localGateAddressLabel, gateStatusLabel, availableAddressesLabel, addressTextureLabel, userFeedbackLabel;
    private UISeparator valuesSeparator;
    private UICheckBox autoCloseChk;
    private UILabel searchLabel;
    private UITextField searchField;
    private UIButton searchClearButton; // the red x after the search field, which empties it
    private static final int SEARCH_CLEAR_SIZE = 12;
    private String searchText = ""; // what the list is narrowed to, as last applied
    private BlockPos location;
    private BlockPos gatePos;
    private World world;
    private EntityPlayer player;
    private boolean delayedUpdate = true;
    private int digit = 0;
    private boolean dialling = false;
    private String enteredAddress = "";
    private String diallingAddress = "";
    private GState gstate = new GState();
    private SGBaseTE localGate = null;
    private boolean last = false;
    public boolean firstOpen = true;
    private List<AddressData> clonedList;
    private final Map<String, String> worldNames = new HashMap<>(); // address -> name of the world it leads to
    private BasicList<AddressData> addressList;
    private long timer;

    // The Explorer Dialer Device: lists the server's generated gates, sent by the server, instead of the addresses
    // kept on the held PDD
    private final boolean explorer;
    private List<AddressData> explorerList; // latest from the server, null until it arrives
    private final Set<String> visitedAddresses = new HashSet<>();
    private int explorerRevision = 0;
    private long lastExplorerRequest = 0;
    // How often the open list checks with the server for newly generated gates
    private static final long EXPLORER_REFRESH_MILLIS = 3000;

    public PddScreen(EntityPlayer player, World worldIn, BlockPos gatePos, boolean isAdmin) {
        this(player, worldIn, gatePos, isAdmin, false);
    }

    public PddScreen(EntityPlayer player, World worldIn, BlockPos gatePos, boolean isAdmin, boolean explorer) {
        this.player = player;
        this.isAdmin = false;
        this.world = worldIn;
        this.location = new BlockPos(player.posX, player.posY, player.posZ);
        this.gatePos = gatePos;
        this.explorer = explorer;
    }

    @Override
    public void construct() {
        long timer = System.currentTimeMillis();

        this.guiscreenBackground = false;
        Keyboard.enableRepeatEvents(true);
        TileEntity localGateTE = world.getTileEntity(gatePos);

        if (localGateTE instanceof SGBaseTE) {
            localGate = (SGBaseTE) localGateTE;
        }

        // Master Panel
        this.form = new BasicForm(BASE_FORM_WIDTH, 241, TextFormatting.WHITE + I18n.format(this.explorer
            ? "sgcraft.gui.edd.label.explorerDialerDevice" : "sgcraft.gui.pdd.label.personalDialerDevice"));
        this.form.setMovable(true);
        this.form.setBackgroundAlpha(255);

        // ****************************************************************************************************************************
        // Address List Area
        // ****************************************************************************************************************************
        this.addressContainer = new BasicContainer(BASE_CONTAINER_WIDTH, 201);
        this.addressContainer.setPosition(0, 0, Anchor.LEFT | Anchor.MIDDLE);
        this.addressContainer.setBorder(FontColors.WHITE, 1, 185);
        this.addressContainer.setPadding(0, 3);
        this.addressContainer.setBackgroundAlpha(0);

        availableAddressesLabel = new UILabel(TextFormatting.WHITE + I18n.format(this.explorer
            ? "sgcraft.gui.edd.label.generatedAddresses" : "sgcraft.gui.pdd.label.availableAddresses")+":");
        availableAddressesLabel.setPosition(5, 1, Anchor.LEFT | Anchor.TOP);

        // Narrows the list to the entries whose name, world or address has what's typed in it
        this.searchLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.search") + ":");
        this.searchLabel.setPosition(5, BasicScreen.getPaddedY(availableAddressesLabel, 4), Anchor.LEFT | Anchor.TOP);
        this.searchField = new UITextField("");
        this.searchField.setPosition(BasicScreen.getPaddedX(this.searchLabel, 4), this.searchLabel.getY() - 2, Anchor.LEFT | Anchor.TOP);
        this.searchField.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(false).build());
        this.searchClearButton = new UIButtonBuilder()
            .text(TextFormatting.RED + "x")
            .size(SEARCH_CLEAR_SIZE, SEARCH_CLEAR_SIZE)
            .anchor(Anchor.LEFT | Anchor.TOP)
            .onClick(() -> this.searchField.setText(""))
            .build("button.pdd.search.clear");
        layoutSearchRow(BASE_CONTAINER_WIDTH);

        localGateAddressLabel = new UILabel("gateAddress");
        localGateAddressLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.0F).build());
        localGateAddressLabel.setPosition(-5, 1, Anchor.RIGHT | Anchor.TOP);

        gateStatusLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.dialing"));
        gateStatusLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.8F).build());
        gateStatusLabel.setPosition(-5, 50, Anchor.CENTER | Anchor.TOP);
        gateStatusLabel.setVisible(true);

        addressTextureLabel = new UILabel("... template ...");
        addressTextureLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.8F).build());
        addressTextureLabel.setPosition(-5, 60, Anchor.CENTER | Anchor.TOP);
        addressTextureLabel.setVisible(false);

        valuesSeparator = new UISeparator(false);
        valuesSeparator.setSize(this.addressContainer.getWidth() - 55, 1);
        valuesSeparator.setPosition(0, 70, Anchor.TOP | Anchor.CENTER);

        this.addressList = new BasicList<>(UIComponent.INHERITED, this.addressContainer.getHeight() - 30);
        this.addressList.setPosition(0, BasicScreen.getPaddedY(this.searchField, 2));
        this.addressList.setItemComponentFactory(AddressItemComponent::new);
        this.addressList.setItemComponentSpacing(1);
        this.addressList.setPadding(2);
        this.addressList.setBorder(FontColors.WHITE, 1, 185);
        this.addressList.setBorders(FontColors.WHITE, 185, 0, 1, 0, 0);
        this.addressList.setName("List");
        this.addressList.register(this);

        this.addressContainer.add(availableAddressesLabel, this.searchLabel, this.searchField, this.searchClearButton, localGateAddressLabel, gateStatusLabel, addressTextureLabel, this.addressList);

        // ****************************************************************************************************************************

        this.addAddressButton = new UIButtonBuilder()
            .text(TextFormatting.GREEN + "+")
            .onClick(() -> new PddEntryScreen(this, player, "Name Here", "Address Here", 10, 0, false, false,false, 1).display())
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .position(0, 0)
            //.visible(this.canAdd)
            .build("button.add");

        this.deleteAddressButton = new UIButtonBuilder()
            .text(TextFormatting.RED + "-")
            .onClick(() -> {
                if (addressList.getSelectedItem() != null && !addressList.getSelectedItem().isLocked()) {
                    new PddEntryScreen(this, player, addressList.getSelectedItem().getName(), addressList.getSelectedItem().getAddress(), addressList.getSelectedItem().getIndex(), addressList.getSelectedItem().getUnid(), addressList.getSelectedItem().isLocked(), addressList.getSelectedItem().isAutoClose(), true,3).display();
                }
            })
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .position(BasicScreen.getPaddedX(this.addAddressButton, 2), 0)
            //.visible(this.canRemove)
            .build("button.remove");

        this.editAddressButton = new UIButtonBuilder()
            .text(TextFormatting.YELLOW + "?")
            .onClick(() -> {
                if (this.explorer) {
                    // Only the name can be changed; the rest belongs to the generated gate
                    final AddressData selected = addressList.getSelectedItem();
                    if (selected != null) {
                        new PddEntryScreen(this, player, selected.getName(), selected.getAddress(), selected.getIndex(), selected.getUnid(), false, false, false, 4).display();
                    }
                    return;
                }
                new PddEntryScreen(this, player, addressList.getSelectedItem().getName(), addressList.getSelectedItem().getAddress(), addressList.getSelectedItem().getIndex(), addressList.getSelectedItem().getUnid(), addressList.getSelectedItem().isLocked(), addressList.getSelectedItem().isAutoClose(),false,2).display();
            })
            .anchor(Anchor.BOTTOM | Anchor.LEFT)
            .position(BasicScreen.getPaddedX(this.deleteAddressButton, 2), 0)
            //.visible(this.canModify)
            .build("button.details");
        if (this.explorer) {
            this.editAddressButton.setPosition(0, 0); // where the add and delete buttons, not shown here, would be
        }

        this.autoCloseChk = new UICheckBox(TextFormatting.WHITE + "Auto Close");
        this.autoCloseChk.setAnchor(Anchor.BOTTOM | Anchor.LEFT);
        this.autoCloseChk.setPosition(BasicScreen.getPaddedX(this.editAddressButton, 2), -2);
        // The PDD shows each entry's own setting; generated gates have none, so there the player ticks it themselves
        this.autoCloseChk.setEnabled(this.explorer);
        this.autoCloseChk.setChecked(false);

        buttonDial = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.CENTER)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.dialSelectedAddress"))
            .visible(false)
            .onClick(() -> {
                dial();
            })
            .build("button.dial");

        buttonReset = new UIButtonBuilder()
                .width(40)
                .anchor(Anchor.BOTTOM | Anchor.CENTER)
                .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.reset"))
                .visible(false)
                .onClick(() -> {
                    resetGui();
                })
                .build("button.reset");

        buttonDisconnect = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.CENTER)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.disconnect"))
            .visible(false)
            .onClick(() -> {
                TileEntity localGate = this.world.getTileEntity(this.gatePos);
                if (!(localGate instanceof SGBaseTE)) {
                    return;
                } else if (localGate != null) {
                    PddNetworkHandler.sendPddInputToServer((SGBaseTE) localGate, 2, "", "");
                }
            })
            .build("button.disconnect");

        final UIButton buttonClose = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.close"))
            .onClick(this::close)
            .build("button.close");

        userFeedbackLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.warning"));
        userFeedbackLabel.setPosition(-15, -3, Anchor.CENTER | Anchor.BOTTOM);

        this.form.add(this.addressContainer, addAddressButton, editAddressButton, deleteAddressButton, autoCloseChk, buttonDial, buttonReset, buttonDisconnect, userFeedbackLabel, buttonClose);
        addToScreen(this.form);
        //this.readAddresses(player);
    }

    @Subscribe
    public void onListChange(BasicList.SelectEvent<GateAccessData> event) {
        //System.out.println("Ping");
        if (this.addressList.getSize() == 0) {
            this.deleteAddressButton.setEnabled(false);
            this.editAddressButton.setEnabled(false);
            this.buttonDial.setEnabled(false);
        } else {
            this.deleteAddressButton.setEnabled(true);
            this.editAddressButton.setEnabled(true);
            this.buttonDial.setEnabled(true);
        }
    }

    // The dialled symbols, drawn over the finished screen. update() runs before the screen is drawn, so symbols drawn
    // there blended their soft edges with the world behind the screen, and kept the panel from drawing over them,
    // leaving a see-through outline round each.
    private String symbolsToDraw;
    private int symbolsX, symbolsY;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.symbolsToDraw != null) {
            this.drawAddressSymbols(this.symbolsX, this.symbolsY, this.symbolsToDraw);
        }
    }

    @Override
    public void update() {
        super.update();
        this.symbolsToDraw = null; // set again below while the symbols should show

        if (unlockMouse && this.lastUpdate == 25) {
            Mouse.setGrabbed(false); // Force the mouse to be visible even though Mouse.isGrabbed() is false.  //#BugsUnited.
            unlockMouse = false; // Only unlock once per session.
        }

        if (this.searchField != null && !this.searchField.getText().equals(this.searchText)) {
            this.searchText = this.searchField.getText();
            if (this.clonedList != null) {
                fillList(this.clonedList);
            }
        }
        this.refresh();
        this.detectChange();

        if (System.currentTimeMillis() > timer + 750) {
            this.checkDiallingStatus();
            timer = System.currentTimeMillis();
        }

        if (++this.lastUpdate > 125) {
            this.lastUpdate = 0;
        }
    }

    private void refresh() { // Method is checked every frame.
        if (localGate == null) { // This is used when PDD is opened but not near a gate.
            this.buttonDial.setVisible(false);
            this.gateStatusLabel.setVisible(false);
            this.localGateAddressLabel.setVisible(false);
            this.userFeedbackLabel.setVisible(false);
        }

        if (localGate != null) {
            if (localGate.isMerged) {
                this.localGateAddressLabel.setText(SGAddressing.formatAddress(localGate.homeAddress, "-", "-"));
            } else {
                this.localGateAddressLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.noLocalStargateFound"));
            }

            if (localGate != null) {

                if ((this.dialling || this.last || localGate.state == SGState.SyncAwait || localGate.state == SGState.Transient)) {
                    this.buttonDial.setVisible(false);
                    this.addressList.setVisible(false);
                    this.autoCloseChk.setVisible(false);
                    this.availableAddressesLabel.setVisible(false);
                    this.localGateAddressLabel.setVisible(false);
                    if (localGate.state == SGState.SyncAwait || localGate.state == SGState.Transient) {
                        if (this.autoCloseChk.isChecked()) {
                            this.close();
                        }
                        this.gateStatusLabel.setText(I18n.format("sgcraft.gui.pdd.label.establishing"));
                        gateStatusLabel.setFontOptions(FontOptions.builder().from(FontColors.GREEN_FO).shadow(true).scale(1.8F).build());
                        if (this.enteredAddress.isEmpty()) {
                            this.enteredAddress = localGate.dialledAddress;
                        }
                    }
                } else {

                    if (localGate.state == SGState.Idle && !localGate.errorState && !dialling) {
                        this.addressList.setVisible(true);
                        this.autoCloseChk.setVisible(true);
                        if (this.addressList.getSize() > 0 && this.addressList.getSelectedItem() != null && !this.addressList.getSelectedItem().getAddress().isEmpty()) {
                            if (this.addressList.getSelectedItem().getAddress().equalsIgnoreCase(this.localGateAddressLabel.getText())) {
                                this.buttonDial.setEnabled(false);
                            } else {
                                this.buttonDial.setEnabled(true);
                            }
                        }
                        this.buttonDial.setVisible(true);
                        this.localGateAddressLabel.setVisible(true);
                        this.availableAddressesLabel.setVisible(true);
                        this.buttonReset.setVisible(false);
                    }

                    if (localGate.state == SGState.Dialling) {
                        if (this.enteredAddress.isEmpty()) {
                            this.enteredAddress = localGate.dialledAddress;
                        }
                        if (this.diallingAddress.isEmpty()) {
                            this.diallingAddress = this.enteredAddress;
                        }

                        this.addressList.setVisible(false);
                        this.autoCloseChk.setVisible(false);
                        this.buttonDisconnect.setVisible(true);
                        this.gateStatusLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.8F).build());
                        this.gateStatusLabel.setText(I18n.format("sgcraft.gui.pdd.label.dialing"));
                        this.buttonReset.setVisible(true);
                    }

                    if (localGate.state == SGState.Disconnecting) {
                        this.addressList.setVisible(false);
                        this.autoCloseChk.setVisible(false);
                        this.gateStatusLabel.setFontOptions(FontOptions.builder().from(FontColors.YELLOW_FO).shadow(true).scale(1.8F).build());
                        this.gateStatusLabel.setText(I18n.format("sgcraft.gui.pdd.label.disconnecting"));

                    }
                    if (localGate.state == SGState.Connected) {
                        if (this.enteredAddress.isEmpty()) {
                            this.enteredAddress = localGate.dialledAddress;
                        }
                        if (this.diallingAddress.isEmpty()) {
                            this.diallingAddress = this.enteredAddress;
                        }

                        this.addressList.setVisible(false);
                        this.buttonDisconnect.setVisible(true);
                        this.gateStatusLabel.setText(I18n.format("sgcraft.gui.pdd.label.connected"));
                        this.gateStatusLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.8F).build());
                    }
                }

                if (localGate.errorState) {
                    this.addressList.setVisible(false);
                    this.buttonDisconnect.setVisible(true);
                    this.gateStatusLabel.setFontOptions(FontOptions.builder().from(FontColors.RED_FO).shadow(true).scale(1.8F).build());
                    this.gateStatusLabel.setText(I18n.format("sgcraft.gui.pdd.label.error"));
                    this.diallingAddress = localGate.dialledAddress; // Account for someone opening the GUI to a connected gate.
                    dialling = false;
                    this.buttonDial.setVisible(false);
                    this.buttonReset.setVisible(true);
                }

                if (!this.addressList.isVisible() && (!(localGate.state == SGState.Disconnecting)) && !this.gateStatusLabel.getText().equalsIgnoreCase("... Error ...")) {
                    // Drawn in drawScreen, once the screen itself is drawn (see there)
                    this.symbolsToDraw = this.enteredAddress;
                    this.symbolsX = getXStartLocation(diallingAddress);
                    this.symbolsY = this.gateStatusLabel.screenY() + 40;
                }

                if (localGate.isConnected() && localGate.state == SGState.Connected) {
                    this.last = false;
                    this.buttonDial.setVisible(false);
                    this.buttonReset.setVisible(false);
                    this.autoCloseChk.setVisible(false);
                    this.buttonDisconnect.setVisible(true);
                    this.buttonDisconnect.setEnabled(true);
                } else {
                    this.buttonDisconnect.setVisible(false);
                }
                this.addAddressButton.setVisible(this.addressList.isVisible());
                this.deleteAddressButton.setVisible(this.addressList.isVisible());
                this.editAddressButton.setVisible(this.addressList.isVisible());
                this.availableAddressesLabel.setVisible(this.addressList.isVisible());
                this.localGateAddressLabel.setVisible(this.addressList.isVisible());
                this.searchLabel.setVisible(this.addressList.isVisible());
                this.searchField.setVisible(this.addressList.isVisible());
                this.searchClearButton.setVisible(this.addressList.isVisible());
                this.valuesSeparator.setVisible(!this.addressList.isVisible());
                this.userFeedbackLabel.setVisible(!this.buttonDial.isVisible() && !this.buttonReset.isVisible() && !this.buttonDisconnect.isVisible() && !(localGate.state == SGState.Disconnecting));
                this.gateStatusLabel.setVisible(!this.addressList.isVisible());

                if (!this.explorer && this.addressList.getSize() > 0 && this.addressList.getSelectedItem() != null) {
                    this.autoCloseChk.setChecked(this.addressList.getSelectedItem().isAutoClose());
                    this.deleteAddressButton.setEnabled(!this.addressList.getSelectedItem().isLocked());
                }
            }
        }

        if (this.explorer) {
            // The server decides which gates are listed, so there's nothing to add or delete
            this.addAddressButton.setVisible(false);
            this.deleteAddressButton.setVisible(false);
            this.editAddressButton.setEnabled(this.addressList.getSelectedItem() != null);
        }
    }

    private void dial() {
        if (localGate != null) { // This will be null in the event user is editing the PDD while not standing near a stargate.
            timer = System.currentTimeMillis();
            if (this.addressList.getSize() > 0 && this.addressList.getSelectedItem() != null && !this.addressList.getSelectedItem().getAddress().isEmpty()) {
                if (this.addressList.getSelectedItem().getAddress().equalsIgnoreCase(this.localGateAddressLabel.getText())) {
                    // Don't dial, user lacks common sense.
                    return;
                }
            }
            if (this.addressList.getSize() > 0 && this.addressList.getSelectedItem() != null && !this.addressList.getSelectedItem().getAddress().isEmpty()) {
                this.resetGui(); //Reset before starting to account for half dialed sequences
                this.lastUpdate = 0;
                if (localGate.chevronsLockOnDial) {
                    if (!isAdmin) {
                        // Progressive dial: the gate enters each symbol itself (SGBaseTE.startLockDial), so this closes as for
                        // a turning dial rather than staying open to send them
                        dialSelectedAddress(5);
                        this.close();
                    } else {
                        // Todo: the below is a very stupid way to implement this...
                        dialSelectedAddress(); // Immediate dial address and lock all chevrons at one
                        // Note: this happens because gate.Connect() checks for "chevronsLockOnDial", if true, then immediately lock dialed address.
                    }
                } else {
                    dialSelectedAddress(); // Immediate Dial but display ring rotation.
                    // This does NOT lock all chevrons at once because the TE value "chevronsLockOnDial" is false; and gate.Connect() knows.
                    this.close();
                }
            }
        }
    }

    private void resetGui() {
        // Note:  this is programmed specifically to wait for TE data to arrive to reset the clients gate.
        PddNetworkHandler.sendPddInputToServer(localGate, 4, "", "");
        localGate.immediateDialAddress = ""; //client side only variable for renderer.
        dialling = false;
        firstOpen = true;
        last = false;
        this.addressList.setVisible(true);
        this.buttonReset.setVisible(false);
        this.buttonDial.setVisible(true);
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

// ******************************************************************************************************************************

    final static String symbolTextureFile = "symbols48.png";
    // The same symbols in teal, for the address list
    private final GuiTexture tealSymbols = new GuiTexture(SGCraft.mod.resourceLocation("textures/gui/symbols48_teal.png"), symbolTextureWidth, symbolTextureHeight);
    final static int symbolsPerRowInTexture = 10;
    final static int symbolWidthInTexture = 48;
    final static int symbolHeightInTexture = 48;
    final static int symbolTextureWidth = 512;
    final static int symbolTextureHeight = 256;
    final static int frameWidth = 236;
    final static int frameHeight = 44;
    final static int borderSize = 6;
    final static int cellSize = 24;

    private void detectChange() {
        if (this.explorer) {
            // The server only replies when something changed, so asking often costs little
            if (System.currentTimeMillis() > this.lastExplorerRequest + EXPLORER_REFRESH_MILLIS) {
                this.lastExplorerRequest = System.currentTimeMillis();
                PddNetworkHandler.requestExplorerListFromServer(this.explorerList == null ? null : this.explorerRevision);
            }
            if (this.explorerList != null && !this.explorerList.equals(this.clonedList)) {
                showAddresses(this.explorerList);
            }
            return;
        }

        final ItemStack stack = player.getHeldItemMainhand();
        NBTTagCompound compound = stack.getTagCompound();
        if (compound != null) {
            List<AddressData> comparedList = AddressData.getAddresses(compound);
            if (!comparedList.equals(clonedList)) {
                this.clonedList = AddressData.getAddresses(compound);
                fillList(this.clonedList);
                this.availableAddressesLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.availableAddresses")
                    + ": (" + this.clonedList.size() + ")");

                // Ask the server which world any new addresses lead to; setWorldNames shows them when they arrive
                final Set<String> unknown = new HashSet<>();
                for (AddressData data : this.clonedList) {
                    if (!this.worldNames.containsKey(data.getAddress())) {
                        unknown.add(data.getAddress());
                    }
                }
                if (!unknown.isEmpty()) {
                    PddNetworkHandler.requestWorldNamesFromServer(unknown);
                }
            }
        }
    }

    // The generated gates from the server, with the world each is in and which this player has visited
    public void setExplorerList(final int revision, final List<AddressData> addresses, final Map<String, String> worlds,
            final Set<String> visited) {
        if (!this.explorer) {
            return;
        }
        this.explorerRevision = revision;
        this.worldNames.putAll(worlds);
        this.visitedAddresses.clear();
        this.visitedAddresses.addAll(visited);
        this.explorerList = addresses;
        if (this.addressList == null) {
            return; // not built yet; detectChange shows the list once it is
        }
        // Shown even if only the visits changed, which the entries alone don't tell apart
        showAddresses(addresses);
    }

    // Shows the generated gates, keeping the selected one selected
    private void showAddresses(final List<AddressData> addresses) {
        this.clonedList = new ArrayList<>(addresses);
        fillList(this.clonedList);
        int visitedCount = 0;
        for (AddressData data : addresses) {
            if (this.visitedAddresses.contains(data.getAddress())) {
                visitedCount++;
            }
        }
        this.availableAddressesLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.edd.label.generatedAddresses")
            + ": (" + addresses.size() + ")" + TextFormatting.GRAY + " - " + I18n.format("sgcraft.gui.edd.label.visitedCount", visitedCount));
    }

    public void setWorldNames(final Map<String, String> names) {
        this.worldNames.putAll(names);
        if (this.clonedList != null) {
            fillList(this.clonedList); // so their address lines pick up the names (and a search by world finds them)
        }
    }

    // Shows the entries that match the search (all of them with none), keeping the selected one selected if it's shown
    private void fillList(final List<AddressData> all) {
        final AddressData selected = this.addressList.getSelectedItem();
        final List<AddressData> shown = matching(all);
        this.addressList.clearItems();
        resetAddressLineWidth();
        this.addressList.addItems(new ArrayList<>(shown));
        if (selected != null) {
            AddressData same = null;
            for (AddressData data : shown) {
                if (data.getAddress().equals(selected.getAddress())) {
                    same = data;
                    if (data.getUnid() == selected.getUnid()) {
                        break; // the very entry, not just one with the same address
                    }
                }
            }
            if (same != null) {
                this.addressList.setSelectedItem(same);
            }
        }
    }

    // The entries whose name, world name or address has the search text in it, ignoring case (and dashes, for addresses)
    private List<AddressData> matching(final List<AddressData> all) {
        final String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return all;
        }
        final String querySymbols = query.replace("-", "");
        final List<AddressData> shown = new ArrayList<>();
        for (AddressData data : all) {
            final String world = this.worldNames.get(data.getAddress());
            if ((data.getName() != null && data.getName().toLowerCase(Locale.ROOT).contains(query))
                    || (world != null && world.toLowerCase(Locale.ROOT).contains(query))
                    || (!querySymbols.isEmpty() && data.getAddress().replace("-", "").toLowerCase(Locale.ROOT).contains(querySymbols))) {
                shown.add(data);
            }
        }
        return shown;
    }

    private void dialRandom() {
        // Todo: figure out how to properly use this.
        final TileEntity localGateTE = this.world.getTileEntity(this.gatePos);
        if (localGateTE instanceof SGBaseTE) {
            localGate = (SGBaseTE) localGateTE;
        }
        if (localGate != null) {
            PddNetworkHandler.sendPddInputToServer(localGate, 3, localGate.homeAddress, ""); // Dials specified address based on Gates configuration, rotation vs. immediate dial.
        }
    }

    private void dialSelectedAddress() {
        dialSelectedAddress(1);
    }

    // Sends the selected address to the server to dial: setting 1 dials it all at once (see PddNetworkHandler), 5 a symbol at
    // a time
    private void dialSelectedAddress(int setting) {
        TileEntity localGateTE = this.world.getTileEntity(this.gatePos);
        if (localGateTE instanceof SGBaseTE) {
            localGate = (SGBaseTE) localGateTE;
        }

       if (localGate != null) {
            if (this.addressList.getSelectedItem() != null && !this.addressList.getSelectedItem().getAddress().isEmpty()) {
                this.gateStatusLabel.setText(I18n.format("sgcraft.gui.pdd.label.dialing"));
                String destination = this.addressList.getSelectedItem().getAddress().toUpperCase().replaceAll("-", "");
                if (SGAddressing.inSameDimension(localGate.homeAddress,destination)) {
                    this.enteredAddress = destination.substring(0,7);
                } else {
                    this.enteredAddress = destination.substring(0,9);
                }
                PddNetworkHandler.sendPddInputToServer(localGate, setting, localGate.homeAddress, destination); // Dials specified address based on Gates configuration, rotation vs. immediate dial.
            }
        }
    }

    private void dialSelectedAddressLockEachChevronIndividually() {
        final TileEntity localGateTE = this.world.getTileEntity(this.gatePos);
        if (localGateTE instanceof SGBaseTE) {
            localGate = (SGBaseTE) localGateTE;
        }

        if (localGate != null) {
            this.enteredAddress = "";
            this.gateStatusLabel.setFontOptions(FontOptions.builder().from(FontColors.BLUE_FO).shadow(true).scale(1.8F).build());
            this.gateStatusLabel.setText(I18n.format("sgcraft.gui.pdd.label.dialing"));
            this.dialling = true; // Allows checkDiallingStatus method to execute.
            this.digit = 0;
        }
    }

    private void checkDiallingStatus() { // Method is checked every 100 frames.
        if (localGate != null) {
            if (dialling && !localGate.errorState) {
                firstOpen = false;
                diallingAddress = this.addressList.getSelectedItem().getAddress().toUpperCase().replaceAll("-", "");

                if (SGAddressing.inSameDimension(localGate.homeAddress, diallingAddress)) {
                    diallingAddress = diallingAddress.substring(0,7);
                }

                if (diallingAddress.length() != 7 && diallingAddress.length() != 9) {
                    //SGBaseTE.sendGenericErrorMsg(player, "Invalid Address Specified.");
                    sendErrorMsg(player, "malformedAddress");
                    dialling = false;
                    return;
                }

                if (diallingAddress.length()  > localGate.getNumChevrons()) {
                    sendErrorMsg(player, "targetLackChevrons");
                    //SGBaseTE.sendGenericErrorMsg(player, "Gate does not have Chevron Upgrade.");
                    dialling = false;
                    return;
                }

                this.addressList.setVisible(false);
                TileEntity localGateTE = this.world.getTileEntity(this.gatePos);

                if (localGateTE instanceof SGBaseTE) {
                    SGBaseTE localGate = (SGBaseTE) localGateTE;
                    this.last = false;
                    localGate.immediateDialAddress = diallingAddress;
                    PddNetworkHandler.sendEnterSymbolToServer(localGate, diallingAddress, this.digit);
                    char currentSymbol = diallingAddress.charAt(digit);
                    enteredAddress += currentSymbol;
                    this.digit += 1;
                }
                if (this.digit >= diallingAddress.length()) {
                    this.dialling = false;
                    this.last = true;
                    this.digit = 0;
                }
            }
        }
    }

    public void stopDialing() {
        this.dialling = false;
    }

    private int getXStartLocation(String address) {
        if (address.length() == 7 || address.length() == 0) {
            return (this.addressTextureLabel.screenX() - 25) + (enteredAddress.length() * 12);
        } else {
            return  (this.addressTextureLabel.screenX() - 55) + (enteredAddress.length() * 12);
        }
    }

    private void drawAddressSymbols(int x, int y, String address) {
        int x0 = x - address.length() * cellSize / 2;
        int y0 = y + frameHeight / 2 - cellSize / 2;
        bindSGTexture(symbolTextureFile,
                symbolTextureWidth * cellSize / symbolWidthInTexture,
                symbolTextureHeight * cellSize / symbolHeightInTexture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        // Previous GUI components can leave a partial alpha and blend mode behind; draw the symbols fully opaque.
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, 0.1F);
        GlStateManager.disableDepth(); // drawn over the finished screen, in front of everything
        GlStateManager.disableLighting();
        int n = address.length();
        for (int i = 0; i < n; i++) {
            int s = SGBaseTE.charToSymbol(address.charAt(i));
            int row = s / symbolsPerRowInTexture;
            int col = s % symbolsPerRowInTexture;

            // Sample a texel inside the cell: with smooth filtering, its edge would pick up the neighbouring symbol
            double inset = cellSize / (double) symbolWidthInTexture;
            drawTexturedRect(x0 + i * cellSize, y0, cellSize, cellSize,
                col * cellSize + inset, row * cellSize + inset, cellSize - 2 * inset, cellSize - 2 * inset);
        }
    }

    private void drawTexturedRect(double x, double y, double w, double h, double u, double v) {
        drawTexturedRect(x, y, w, h, u, v, w, h);
    }

    private void drawTexturedRect(double x, double y, double w, double h, double u, double v, double us, double vs) {
        drawTexturedRectUV(x, y, w, h, u * gstate.uscale, v * gstate.vscale, us * gstate.uscale, vs * gstate.vscale);
    }

    private void drawTexturedRectUV(double x, double y, double w, double h, double u, double v, double us, double vs) {
        glBegin(GL_QUADS);
        glColor4f(gstate.red, gstate.green, gstate.blue, 1.0F);
        glTexCoord2d(u, v+vs);
        glVertex3d(x, y+h, 1000); //zLevel
        glTexCoord2d(u+us, v+vs);
        glVertex3d(x+w, y+h, 1000); //zLevel
        glTexCoord2d(u+us, v);
        glVertex3d(x+w, y, 1000); //zLevel
        glTexCoord2d(u, v);
        glVertex3d(x, y, 1000);//zLevel
        glEnd();
    }

    private void bindSGTexture(String name, int usize, int vsize) {
        bindTexture(SGCraft.mod.resourceLocation("textures/gui/" + name), usize, vsize);
    }

    private void bindTexture(ResourceLocation rsrc, int usize, int vsize) {
        gstate.texture = rsrc;
        mc.getTextureManager().bindTexture(rsrc);
        gstate.uscale = 1.0 / usize;
        gstate.vscale = 1.0 / vsize;
    }

    // Whether an address is the gate being dialled from: its full address, or (as dialled within one dimension) the
    // first 7 symbols of it, whatever the dashes or case
    private boolean isLocalGateAddress(String address) {
        if (localGate == null || localGate.homeAddress == null || address == null) {
            return false;
        }
        String home = localGate.homeAddress.replace("-", "").toUpperCase();
        String a = address.replace("-", "").trim().toUpperCase();
        return a.equals(home) || (a.length() == SGAddressing.numCoordSymbols && home.startsWith(a));
    }

    // ---- Widening the window to fit the longest address line ----

    // The window's normal widths, which it never shrinks below
    private static final int BASE_FORM_WIDTH = 300, BASE_CONTAINER_WIDTH = 295;
    // Room each list entry keeps on its right for the move arrows and locked marker
    private static final int ENTRY_RIGHT_RESERVE = 27;
    // The widest address line (to the end of its world name) among the entries shown
    private int widestAddressLine = 0;

    // Called by each entry as it is laid out; widens the window if this line wouldn't fit
    private void fitAddressLine(int lineEnd) {
        if (lineEnd > this.widestAddressLine) {
            this.widestAddressLine = lineEnd;
            resizeToAddressLines();
        }
    }

    // Starts over when the list is rebuilt, so the window narrows again once long lines are gone
    private void resetAddressLineWidth() {
        this.widestAddressLine = 0;
        resizeToAddressLines();
    }

    // The search field fills the row but for the clear button after it, which is exactly as tall as the field and level
    // with it (a button sizes itself to at least its text plus 6 pixels unless told not to, which made it taller)
    private void layoutSearchRow(int containerWidth) {
        int fieldWidth = containerWidth - this.searchField.getX() - 6 - SEARCH_CLEAR_SIZE - 2;
        this.searchField.setSize(fieldWidth, 0);
        // A fixed size: setSize(width, height) only sets the least a button may be (it grows to fit its text)
        this.searchClearButton.setSize(Size.of(SEARCH_CLEAR_SIZE, this.searchField.getHeight()));
        this.searchClearButton.setPosition(this.searchField.getX() + fieldWidth + 2, this.searchField.getY(),
            Anchor.LEFT | Anchor.TOP);
    }

    private void resizeToAddressLines() {
        // An entry's width at the normal size: the list's inner width, less room for its scroll bar whether or not it shows
        int baseEntryWidth = BASE_CONTAINER_WIDTH - this.addressList.getLeftPadding() - this.addressList.getRightPadding()
            - (this.addressList.getScrollBar().getRawWidth() + 5);
        int extra = Math.max(0, this.widestAddressLine + ENTRY_RIGHT_RESERVE - baseEntryWidth);
        this.form.setWidth(BASE_FORM_WIDTH + extra);
        this.addressContainer.setWidth(BASE_CONTAINER_WIDTH + extra);
        this.valuesSeparator.setSize(BASE_CONTAINER_WIDTH + extra - 55, 1);
        if (this.searchField != null) {
            layoutSearchRow(BASE_CONTAINER_WIDTH + extra);
        }
    }

    private class AddressItemComponent extends BasicList.ItemComponent<AddressData> {

        private UILabel addressLabel, nameLabel;

        // Each address's symbols, small and in teal, shown in its list entry
        private static final int listSymbolSize = 9;
        private BasicContainer lockedStatusContainer;
        private UIButton moveUpButton, moveDownButton;

        public AddressItemComponent(final MalisisGui gui, final BasicList<AddressData> parent, final AddressData item) {
            super(gui, parent, item);
            this.setOnDoubleClickConsumer(i -> {
                if (!isLocalGateAddress(this.item.getAddress())) {
                    dial();
                }
            });
        }

        // The gate being dialled from can't be dialled, so its entry is greyed out and can't be selected (its arrows
        // still move it)
        @Override
        public boolean onClick() {
            if (isLocalGateAddress(this.item.getAddress())) {
                return true;
            }
            return super.onClick();
        }

        @Override
        protected void construct(final MalisisGui gui) {
            super.construct(gui);

            this.setHeight(28);
            this.setPadding(1);

            final boolean isLocal = isLocalGateAddress(this.item.getAddress());
            this.nameLabel = new UILabel((isLocal ? TextFormatting.DARK_GRAY : TextFormatting.WHITE) + this.item.getName());
            this.nameLabel.setPosition(4, 3);

            // The address, then its symbols in teal, then the world it leads to: "ABCD-EFG-HI - <symbols> - world"
            final String worldName = worldNames.get(this.item.getAddress());
            final String symbols = this.item.getAddress().replace("-", "").trim().toUpperCase();
            final boolean showSymbols = !symbols.isEmpty() && SGAddressing.validSymbols(symbols);
            this.addressLabel = new UILabel((isLocal ? TextFormatting.DARK_GRAY : TextFormatting.BLUE) + this.item.getAddress()
                + (showSymbols || worldName != null ? TextFormatting.GRAY + " - " : ""));
            this.addressLabel.setPosition(4, BasicScreen.getPaddedY(this.nameLabel, 2));

            int x = this.addressLabel.getX() + this.addressLabel.getWidth() + 1;
            if (showSymbols) {
                for (int i = 0; i < symbols.length(); i++) {
                    int s = SGBaseTE.charToSymbol(symbols.charAt(i));
                    final UIImage symbol = new UIImage(new GuiIcon(tealSymbols, (s % symbolsPerRowInTexture) * symbolWidthInTexture,
                        (s / symbolsPerRowInTexture) * symbolHeightInTexture, symbolWidthInTexture, symbolHeightInTexture));
                    symbol.setSize(listSymbolSize, listSymbolSize);
                    symbol.setPosition(x, this.addressLabel.getY() - 1);
                    this.add(symbol);
                    x += listSymbolSize + 1;
                }
            }
            if (worldName != null) {
                final UILabel worldLabel = new UILabel(TextFormatting.GRAY + (showSymbols ? " - " : "") + worldName);
                worldLabel.setPosition(x, this.addressLabel.getY());
                this.add(worldLabel);
                x += worldLabel.getWidth();
            }
            fitAddressLine(x);

            this.lockedStatusContainer = new BasicContainer(5, UIComponent.INHERITED);
            this.lockedStatusContainer.setPosition(0, 0, Anchor.MIDDLE | Anchor.RIGHT);

            if (explorer) {
                // Whether the player has been to this gate, where the PDD has its move arrows: generated gates keep
                // the server's order
                final boolean visited = visitedAddresses.contains(this.item.getAddress());
                final UILabel visitedLabel = new UILabel((visited ? TextFormatting.GREEN : TextFormatting.DARK_GRAY)
                    + I18n.format(visited ? "sgcraft.gui.edd.label.visited" : "sgcraft.gui.edd.label.notVisited"));
                visitedLabel.setPosition(-8, 3, Anchor.TOP | Anchor.RIGHT);
                // Room for a long name beside it, as for the address line (which keeps its reserve on the right)
                fitAddressLine(this.nameLabel.getX() + this.nameLabel.getWidth() + 6 + visitedLabel.getWidth() + 8 - ENTRY_RIGHT_RESERVE);
                this.update();
                this.add(this.nameLabel, this.addressLabel, visitedLabel, this.lockedStatusContainer);
                return;
            }

            // Move this entry up or down the list; the server swaps its index with its neighbour's
            final List<AddressData> order = clonedList;
            final boolean first = order == null || order.isEmpty() || order.get(0).getUnid() == this.item.getUnid();
            final boolean last = order == null || order.isEmpty() || order.get(order.size() - 1).getUnid() == this.item.getUnid();
            // Not while searching: an entry would swap places with a neighbour that isn't shown
            final boolean searching = !searchText.trim().isEmpty();
            this.moveUpButton = arrowButton("pdd_arrow_up", -1, Anchor.TOP | Anchor.RIGHT, !first && !searching);
            this.moveDownButton = arrowButton("pdd_arrow_down", 1, Anchor.BOTTOM | Anchor.RIGHT, !last && !searching);

            this.update();

            this.add(this.nameLabel, this.addressLabel, this.moveUpButton, this.moveDownButton, this.lockedStatusContainer);
        }

        private UIButton arrowButton(String texture, int direction, int anchor, boolean enabled) {
            // The whole image: the texture size only converts pixels to UVs, unused for 0 to 1
            final UIImage arrow = new UIImage(new GuiIcon(new GuiTexture(SGCraft.mod.resourceLocation("textures/gui/" + texture + ".png"), 1, 1),
                0F, 0F, 1F, 1F));
            arrow.setSize(9, 5);
            return new UIButtonBuilder()
                .image(arrow)
                .size(15, 12)
                .position(-8, 0)
                .anchor(anchor)
                .enabled(enabled)
                .onClick(() -> PddNetworkHandler.sendPddEntryMoveToServer(this.item.getUnid(), direction))
                .build("button.pdd.move." + (direction < 0 ? "up" : "down"));
        }

        public void update() {
            if (explorer) {
                this.lockedStatusContainer.setColor(visitedAddresses.contains(this.item.getAddress()) ? FontColors.GREEN : FontColors.GRAY);
                return;
            }
            this.lockedStatusContainer.setColor(this.item.isLocked() ? FontColors.GRAY : FontColors.GREEN);
        }
    }
}
