package gcewing.sg.features.configurator.client.gui;

import static gcewing.sg.tileentity.SGBaseTE.sendBasicMsg;

import com.google.common.eventbus.Subscribe;
import gcewing.sg.SGCraft;
import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.features.pdd.client.gui.PddEntryScreen;
import gcewing.sg.features.pdd.network.PddNetworkHandler;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.tileentity.data.GateAccessData;
import gcewing.sg.util.GateUtil;
import gcewing.sg.util.SGAddressing;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.BasicContainer;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.container.BasicList;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.decoration.UISeparator;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.client.gui.event.ComponentEvent;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GateAddressAccessScreen extends BasicScreen {
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private boolean isAdmin;
    private boolean delayedUpdate = false;
    private BasicForm form;
    private BasicContainer<?> addressContainer, addressOptionsContainer;
    private UIButton buttonClose, addAddressButton, editAddressButton, deleteAddressButton, saveOptionsButton, saveDefaultOptionsButton;
    private UILabel addressListLabel, gateDefaultIncomingLabel, gateDefaultOutgoingLabel, perAddressOptionsLabel, gateIncomingLabel, gateOutgoingLabel;
    private UICheckBox defaultAllowIncomingCheckbox, defaultDenyIncomingCheckbox, defaultAllowOutgoingCheckbox, defaultDenyOutgoingCheckbox;
    private UICheckBox allowIncomingCheckbox, denyIncomingCheckbox, allowOutgoingCheckbox, denyOutgoingCheckbox;
    private BlockPos location;
    private BlockPos gatePos;
    private World world;
    private EntityPlayer player;
    private SGBaseTE localGate;
    private List<GateAccessData> clonedList;
    private BasicList<GateAccessData> gateAccessList;

    public GateAddressAccessScreen(BasicScreen parent, EntityPlayer player, World worldIn, BlockPos gatePos, boolean isAdmin) {
        super(parent, true);
        this.player = player;
        this.isAdmin = false;
        this.world = worldIn;
        this.location = new BlockPos(player.posX, player.posY, player.posZ);
        this.gatePos = gatePos;
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;
        Keyboard.enableRepeatEvents(true);
        final TileEntity localGateTE = this.world.getTileEntity(this.gatePos);
        if (localGateTE instanceof SGBaseTE) {
            localGate = (SGBaseTE) localGateTE;
        }

        // Master Panel
        this.form = new BasicForm(300, 225, TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.gateAddressAccessList"));
        this.form.setMovable(true);
        this.form.setBackgroundAlpha(255);

        // ****************************************************************************************************************************
        // Address List Area
        // ****************************************************************************************************************************
        this.addressContainer = new BasicContainer(100, 145);
        this.addressContainer.setPosition(0, 0, Anchor.LEFT | Anchor.TOP);
        this.addressContainer.setBorder(FontColors.WHITE, 1, 185);
        this.addressContainer.setPadding(0, 3);
        this.addressContainer.setBackgroundAlpha(0);

        this.addressListLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.addressList"));
        this.addressListLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.addressListLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        this.gateAccessList = new BasicList<>(UIComponent.INHERITED, this.addressContainer.getHeight() - 14);
        this.gateAccessList.setPosition(0, 10);
        this.gateAccessList.setItemComponentFactory(GateItemComponent::new);
        this.gateAccessList.setItemComponentSpacing(1);
        this.gateAccessList.setPadding(2);
        this.gateAccessList.setName("List");
        this.gateAccessList.register(this);
        this.gateAccessList.setBorder(FontColors.WHITE, 1, 185);
        this.gateAccessList.setBorders(FontColors.WHITE, 185, 0, 1, 0, 0);

        this.addressContainer.add(this.addressListLabel, this.gateAccessList);

        // ****************************************************************************************************************************

        this.perAddressOptionsLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.perAddressOptions"));
        this.perAddressOptionsLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.perAddressOptionsLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        final UISeparator optionsSeparator = new UISeparator(false);
        optionsSeparator.setSize(this.perAddressOptionsLabel.getWidth() - 10, 1);
        optionsSeparator.setPosition(0, 10, Anchor.TOP | Anchor.CENTER);

        this.addressOptionsContainer = new BasicContainer(150, 145);
        this.addressOptionsContainer.setPosition(0, 0, Anchor.RIGHT | Anchor.TOP);
        this.addressOptionsContainer.setBorder(FontColors.WHITE, 1, 185);
        this.addressOptionsContainer.setPadding(0, 3);
        this.addressOptionsContainer.setBackgroundAlpha(0);

        this.gateIncomingLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.incoming")+":");
        this.gateIncomingLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.gateIncomingLabel.setPosition(5, 20, Anchor.TOP| Anchor.LEFT);

        this.allowIncomingCheckbox = new UICheckBox();
        this.allowIncomingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.allowIncomingCheckbox.setPosition(this.gateIncomingLabel.getX() + this.gateIncomingLabel.getWidth() + 10, this.gateIncomingLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.allowIncomingCheckbox.setName("checkbox.allowincoming");
        this.allowIncomingCheckbox.setChecked(localGate.defaultAllowIncoming);
        this.allowIncomingCheckbox.register(this);

        this.denyIncomingCheckbox = new UICheckBox();
        this.denyIncomingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.denyIncomingCheckbox.setPosition(this.gateIncomingLabel.getX() + this.gateIncomingLabel.getWidth() + 10 + this.allowIncomingCheckbox.getWidth() + 10, this.gateIncomingLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.denyIncomingCheckbox.setName("checkbox.denyincoming");
        this.denyIncomingCheckbox.setChecked(!localGate.defaultAllowIncoming);
        this.denyIncomingCheckbox.register(this);

        this.gateOutgoingLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.outgoing")+":");
        this.gateOutgoingLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.gateOutgoingLabel.setPosition(5, this.gateIncomingLabel.getY() + 15, Anchor.TOP | Anchor.LEFT);

        this.allowOutgoingCheckbox = new UICheckBox();
        this.allowOutgoingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.allowOutgoingCheckbox.setPosition(this.gateIncomingLabel.getX() + this.gateIncomingLabel.getWidth() + 10, this.gateOutgoingLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.allowOutgoingCheckbox.setName("checkbox.allowoutgoing");
        this.allowOutgoingCheckbox.setChecked(localGate.defaultAllowOutgoing);
        this.allowOutgoingCheckbox.register(this);

        this.denyOutgoingCheckbox = new UICheckBox();
        this.denyOutgoingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.denyOutgoingCheckbox.setPosition(this.gateOutgoingLabel.getX() + this.gateIncomingLabel.getWidth() + 10 + this.allowIncomingCheckbox.getWidth() + 10, this.gateOutgoingLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.denyOutgoingCheckbox.setName("checkbox.denyoutgoing");
        this.denyOutgoingCheckbox.setChecked(!localGate.defaultAllowOutgoing);
        this.denyOutgoingCheckbox.register(this);

        this.saveOptionsButton = new UIButtonBuilder()
                .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.save"))
                .onClick(() -> {
                    if (this.gateAccessList.getSize() > 0 && this.gateAccessList.getSelectedItem() != null) {
                        if (!this.gateAccessList.getSelectedItem().getAddress().isEmpty()) {
                            ConfiguratorNetworkHandler.sendGateAddressAccessInputToServer(localGate, this.gateAccessList.getSelectedItem().getAddress().toUpperCase(),false, false,  this.allowIncomingCheckbox.isChecked(), this.allowOutgoingCheckbox.isChecked());
                            sendBasicMsg(player, "changesSaved");
                        }
                    }
                })
                .anchor(Anchor.BOTTOM | Anchor.RIGHT)
                .position(-5,0)
                .build("button.save");

        this.addressOptionsContainer.add(this.perAddressOptionsLabel, optionsSeparator, this.gateIncomingLabel, this.allowIncomingCheckbox, this.denyIncomingCheckbox, gateOutgoingLabel, this.allowOutgoingCheckbox, this.denyOutgoingCheckbox, this.saveOptionsButton);

        // ****************************************************************************************************************************

        this.addAddressButton = new UIButtonBuilder()
                .text(TextFormatting.GREEN + "+")
                .onClick(() -> {
                    if (!(localGate.getGateAccessData() != null && localGate.getGateAccessData().stream().filter(g -> g.getAddress().equalsIgnoreCase(SGAddressing.formatAddress(localGate.homeAddress, "-", "-"))).findFirst().isPresent())) {
                        new GateAddressAccessEntryScreen(this, player, localGate, "", SGAddressing.formatAddress(localGate.homeAddress, "-", "-"), 1).display();
                    } else {
                        new GateAddressAccessEntryScreen(this, player, localGate, "", "Address Here", 1).display();
                    }
                })
                .anchor(Anchor.TOP | Anchor.LEFT)
                .position(this.addressContainer.getWidth() + 17, 40)
                //.visible(this.canAdd)
                .build("button.add");

        this.editAddressButton = new UIButtonBuilder()
                .text(TextFormatting.YELLOW + "?")
                .onClick(() -> {
                    new GateAddressAccessEntryScreen(this, player, localGate,this.gateAccessList.getSelectedItem().getAddress(), "", 2).display();
                })
                .anchor(Anchor.TOP | Anchor.LEFT)
                .position(this.addAddressButton.getX(), this.addAddressButton.getY() + 20)
                //.visible(this.canModify)
                .build("button.details");

        this.deleteAddressButton = new UIButtonBuilder()
                .text(TextFormatting.RED + "-")
                .onClick(() -> {
                    new GateAddressAccessEntryScreen(this, player, localGate,this.gateAccessList.getSelectedItem().getAddress(), "", 3).display();
                })
                .anchor(Anchor.TOP | Anchor.LEFT)
                .position(this.addAddressButton.getX(), this.editAddressButton.getY() + 20)
                .build("button.remove");

        // ****************************************************************************************************************************

        final UISeparator defaultsSeparator = new UISeparator(false);
        defaultsSeparator.setSize(this.form.getWidth() - 10, 1);
        defaultsSeparator.setPosition(0, -50, Anchor.BOTTOM | Anchor.CENTER);

        this.gateDefaultIncomingLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.defaultIncoming"));
        this.gateDefaultIncomingLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.gateDefaultIncomingLabel.setPosition(5, -35, Anchor.BOTTOM | Anchor.LEFT);

        this.defaultAllowIncomingCheckbox = new UICheckBox();
        this.defaultAllowIncomingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.defaultAllowIncomingCheckbox.setPosition(this.gateDefaultIncomingLabel.getX() + this.gateDefaultIncomingLabel.getWidth() + 10, this.gateDefaultIncomingLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultAllowIncomingCheckbox.setName("checkbox.defaultallowincoming");
        this.defaultAllowIncomingCheckbox.setChecked(localGate.defaultAllowIncoming);
        this.defaultAllowIncomingCheckbox.register(this);

        this.defaultDenyIncomingCheckbox = new UICheckBox();
        this.defaultDenyIncomingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.defaultDenyIncomingCheckbox.setPosition(this.gateDefaultIncomingLabel.getX() + this.gateDefaultIncomingLabel.getWidth() + 10 + this.defaultAllowIncomingCheckbox.getWidth() + 10, this.gateDefaultIncomingLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultDenyIncomingCheckbox.setName("checkbox.defaultdenyincoming");
        this.defaultDenyIncomingCheckbox.setChecked(!localGate.defaultAllowIncoming);
        this.defaultDenyIncomingCheckbox.register(this);

        this.gateDefaultOutgoingLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.defaultOutgoing"));
        this.gateDefaultOutgoingLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.gateDefaultOutgoingLabel.setPosition(5, -20, Anchor.BOTTOM | Anchor.LEFT);

        this.defaultAllowOutgoingCheckbox = new UICheckBox();
        this.defaultAllowOutgoingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.defaultAllowOutgoingCheckbox.setPosition(this.gateDefaultIncomingLabel.getX() + this.gateDefaultIncomingLabel.getWidth() + 10, this.gateDefaultOutgoingLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultAllowOutgoingCheckbox.setName("checkbox.defaultallowoutgoing");
        this.defaultAllowOutgoingCheckbox.setChecked(localGate.defaultAllowOutgoing);
        this.defaultAllowOutgoingCheckbox.register(this);

        this.defaultDenyOutgoingCheckbox = new UICheckBox();
        this.defaultDenyOutgoingCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.defaultDenyOutgoingCheckbox.setPosition(this.gateDefaultIncomingLabel.getX() + this.gateDefaultIncomingLabel.getWidth() + 10 + this.defaultAllowIncomingCheckbox.getWidth() + 10, this.gateDefaultOutgoingLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultDenyOutgoingCheckbox.setName("checkbox.defaultdenyoutgoing");
        this.defaultDenyOutgoingCheckbox.setChecked(!localGate.defaultAllowOutgoing);
        this.defaultDenyOutgoingCheckbox.register(this);

        buttonClose = new UIButtonBuilder()
                .width(40)
                .anchor(Anchor.BOTTOM | Anchor.RIGHT)
                .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.close"))
                .onClick(this::close)
                .build("button.close");

        this.saveDefaultOptionsButton = new UIButtonBuilder()
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.save"))
            .onClick(() -> {
                ConfiguratorNetworkHandler.sendGateAddressAccessInputToServer(localGate, "", this.defaultAllowIncomingCheckbox.isChecked(), this.defaultAllowOutgoingCheckbox.isChecked(), this.allowIncomingCheckbox.isChecked(), this.allowOutgoingCheckbox.isChecked());
                sendBasicMsg(player, "changesSaved");
                this.close();
            })
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .position(-(buttonClose.getX() + buttonClose.getWidth() + 5),0)
            .build("button.savedefaults");

        this.form.add(this.addressContainer, defaultsSeparator, this.addAddressButton, this.editAddressButton, this.deleteAddressButton, this.gateDefaultIncomingLabel, this.gateDefaultOutgoingLabel, buttonClose);
        this.form.add(this.addressOptionsContainer, saveDefaultOptionsButton);
        this.form.add(this.defaultAllowIncomingCheckbox, this.defaultDenyIncomingCheckbox, this.defaultAllowOutgoingCheckbox, this.defaultDenyOutgoingCheckbox);
        addToScreen(this.form);

        loadData();
    }

    @Subscribe
    public void onListChange(BasicList.SelectEvent<GateAccessData> event) {
        boolean firstClick = (event.getOldValue() == null);
        final GateAccessData entry = event.getNewValue();
        if (localGate.getGateAccessData().size() > 0 && entry != null) {
            // The entry's own settings, as listed for the gate
            this.allowIncomingCheckbox.setChecked(entry.hasIncomingAccess());
            this.denyIncomingCheckbox.setChecked(!entry.hasIncomingAccess());
            this.allowOutgoingCheckbox.setChecked(entry.hasOutgoingAccess());
            this.denyOutgoingCheckbox.setChecked(!entry.hasOutgoingAccess());
            this.addressOptionsContainer.setEnabled(true);
        } else {
            this.addressOptionsContainer.setEnabled(false);
        }
    }

    @Subscribe
    public void onValueChange(ComponentEvent.ValueChange event) {

        switch (event.getComponent().getName()) {
            case "checkbox.defaultallowincoming":
                this.defaultDenyIncomingCheckbox.setChecked(this.defaultAllowIncomingCheckbox.isChecked());
                break;
            case "checkbox.defaultdenyincoming":
                this.defaultAllowIncomingCheckbox.setChecked(this.defaultDenyIncomingCheckbox.isChecked());
                break;
            case "checkbox.defaultallowoutgoing":
                this.defaultDenyOutgoingCheckbox.setChecked(this.defaultAllowOutgoingCheckbox.isChecked());
                break;
            case "checkbox.defaultdenyoutgoing":
                this.defaultAllowOutgoingCheckbox.setChecked(this.defaultDenyOutgoingCheckbox.isChecked());
                break;
            case "checkbox.allowincoming":
                this.denyIncomingCheckbox.setChecked(this.allowIncomingCheckbox.isChecked());
                break;
            case "checkbox.denyincoming":
                this.allowIncomingCheckbox.setChecked(this.denyIncomingCheckbox.isChecked());
                break;
            case "checkbox.allowoutgoing":
                this.denyOutgoingCheckbox.setChecked(this.allowOutgoingCheckbox.isChecked());
                break;
            case "checkbox.denyoutgoing":
                this.allowOutgoingCheckbox.setChecked(this.denyOutgoingCheckbox.isChecked());
                break;
        }
    }

    @Override
    public void update() {
        super.update();
        if (unlockMouse && this.lastUpdate == 25) {
            Mouse.setGrabbed(false); // Force the mouse to be visible even though Mouse.isGrabbed() is false.  //#BugsUnited.
            unlockMouse = false; // Only unlock once per session.
        }

        if (this.lastUpdate == 20) {
            this.editAddressButton.setEnabled(this.gateAccessList.getSize() > 0);
            this.deleteAddressButton.setEnabled(this.gateAccessList.getSize() > 0);
            if (!localGate.getGateAccessData().equals(this.clonedList)) {
                this.loadData();
            }
        }

        if (++this.lastUpdate > 60) {
            this.lastUpdate = 0;
        }
    }

    public void delayedUpdate() {
        this.lastUpdate = 0;
        this.delayedUpdate = true;
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

    private void loadData() {
        if (localGate != null) {
            if (localGate.getGateAccessData() != null) {
                this.gateAccessList.setItems(localGate.getGateAccessData());
                this.gateAccessList.setSelectedItem(this.gateAccessList.getItems().stream().findFirst().orElse(null));
                this.clonedList = new ArrayList<>(localGate.getGateAccessData());
            }
        }
    }

    private class GateItemComponent extends BasicList.ItemComponent<GateAccessData> {

        private UILabel addressLabel;

        public GateItemComponent(final MalisisGui gui, final BasicList<GateAccessData> parent, final GateAccessData item) {
            super(gui, parent, item);
        }

        @Override
        protected void construct(final MalisisGui gui) {
            super.construct(gui);

            this.setHeight(15);
            this.setPadding(1);

            this.addressLabel = new UILabel(TextFormatting.WHITE + this.item.getAddress());
            this.addressLabel.setAnchor(Anchor.CENTER | Anchor.MIDDLE);


            this.add(this.addressLabel);
        }

        public void update() {

        }
    }
}
