package gcewing.sg.features.configurator.client.gui;

import static gcewing.sg.tileentity.SGBaseTE.sendBasicMsg;
import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;

import com.google.common.eventbus.Subscribe;
import gcewing.sg.SGCraft;
import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.tileentity.data.PlayerAccessData;
import gcewing.sg.util.GateUtil;
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

public class PlayerAccessScreen extends BasicScreen {
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private boolean isAdmin;
    private boolean delayedUpdate = false;
    private BasicForm form;
    private BasicContainer<?> playerContainer, optionsContainer;
    private UIButton buttonClose, addPlayerButton, editPlayerButton, deletePlayerButton, saveOptionsButton, saveDefaultOptionsButton;
    private UILabel addressListLabel, gateDefaultAccessLabel, gateDefaultIrisControllerLabel, optionsLabel, accessLabel, irisControllerLabel, gateDefaultAdminLabel, adminLabel;
    private UICheckBox defaultAllowAccessCheckbox, defaultDenyAccessCheckbox, defaultAllowIrisCheckbox, defaultDenyIrisCheckbox, defaultAllowAdminCheckbox, defaultDenyAdminCheckbox;
    private UICheckBox allowAccessCheckbox, denyAccessCheckbox, allowIrisControllerCheckbox, denyIrisControllerCheckbox, allowAdminCheckbox, denyAdminCheckbox;
    private BlockPos location;
    private BlockPos gatePos;
    private World world;
    private EntityPlayer player;
    private SGBaseTE localGate;
    private List<PlayerAccessData> clonedList;
    private BasicList<PlayerAccessData> playerAccessList;

    public PlayerAccessScreen(BasicScreen parent, EntityPlayer player, World worldIn, BlockPos gatePos, boolean isAdmin) {
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
        this.form = new BasicForm(350, 225, TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.playerAccessList"));
        this.form.setMovable(true);
        this.form.setBackgroundAlpha(255);

        // ****************************************************************************************************************************
        // Address List Area
        // ****************************************************************************************************************************
        this.playerContainer = new BasicContainer(150, 145);
        this.playerContainer.setPosition(0, 0, Anchor.LEFT | Anchor.TOP);
        this.playerContainer.setBorder(FontColors.WHITE, 1, 185);
        this.playerContainer.setPadding(0, 3);
        this.playerContainer.setBackgroundAlpha(0);

        this.addressListLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.playerList"));
        this.addressListLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.addressListLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        this.playerAccessList = new BasicList<>(UIComponent.INHERITED, this.playerContainer.getHeight() - 14);
        this.playerAccessList.setPosition(0, 10);
        this.playerAccessList.setItemComponentFactory(PlayerItemComponent::new);
        this.playerAccessList.setItemComponentSpacing(1);
        this.playerAccessList.setPadding(2);
        this.playerAccessList.setName("List");
        this.playerAccessList.register(this);
        this.playerAccessList.setBorder(FontColors.WHITE, 1, 185);
        this.playerAccessList.setBorders(FontColors.WHITE, 185, 0, 1, 0, 0);

        this.playerContainer.add(this.addressListLabel, this.playerAccessList);

        // ****************************************************************************************************************************

        this.optionsLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.options"));
        this.optionsLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.optionsLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        final UISeparator optionsSeparator = new UISeparator(false);
        optionsSeparator.setSize(this.optionsLabel.getWidth() - 10, 1);
        optionsSeparator.setPosition(0, 10, Anchor.TOP | Anchor.CENTER);

        this.optionsContainer = new BasicContainer(150, 145);
        this.optionsContainer.setPosition(0, 0, Anchor.RIGHT | Anchor.TOP);
        this.optionsContainer.setBorder(FontColors.WHITE, 1, 185);
        this.optionsContainer.setPadding(0, 3);
        this.optionsContainer.setBackgroundAlpha(0);

        this.accessLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.access")+":");
        this.accessLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.accessLabel.setPosition(5, 20, Anchor.TOP| Anchor.LEFT);

        this.allowAccessCheckbox = new UICheckBox();
        this.allowAccessCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.allowAccessCheckbox.setPosition(this.accessLabel.getX() + this.accessLabel.getWidth() + 10, this.accessLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.allowAccessCheckbox.setName("checkbox.allowincoming");
        this.allowAccessCheckbox.setChecked(localGate.defaultAllowIncoming);
        this.allowAccessCheckbox.register(this);

        this.denyAccessCheckbox = new UICheckBox();
        this.denyAccessCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.denyAccessCheckbox.setPosition(this.accessLabel.getX() + this.accessLabel.getWidth() + 10 + this.allowAccessCheckbox.getWidth() + 10, this.accessLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.denyAccessCheckbox.setName("checkbox.denyincoming");
        this.denyAccessCheckbox.setChecked(!localGate.defaultAllowIncoming);
        this.denyAccessCheckbox.register(this);

        this.irisControllerLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.iris")+":");
        this.irisControllerLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.irisControllerLabel.setPosition(5, this.accessLabel.getY() + 15, Anchor.TOP | Anchor.LEFT);

        this.allowIrisControllerCheckbox = new UICheckBox();
        this.allowIrisControllerCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.allowIrisControllerCheckbox.setPosition(this.accessLabel.getX() + this.accessLabel.getWidth() + 10, this.irisControllerLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.allowIrisControllerCheckbox.setName("checkbox.allowoutgoing");
        this.allowIrisControllerCheckbox.setChecked(localGate.defaultAllowOutgoing);
        this.allowIrisControllerCheckbox.register(this);

        this.denyIrisControllerCheckbox = new UICheckBox();
        this.denyIrisControllerCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.denyIrisControllerCheckbox.setPosition(this.irisControllerLabel.getX() + this.accessLabel.getWidth() + 10 + this.allowAccessCheckbox.getWidth() + 10, this.irisControllerLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.denyIrisControllerCheckbox.setName("checkbox.denyoutgoing");
        this.denyIrisControllerCheckbox.setChecked(!localGate.defaultAllowOutgoing);
        this.denyIrisControllerCheckbox.register(this);

        this.adminLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.admin")+":");
        this.adminLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.adminLabel.setPosition(5, this.irisControllerLabel.getY() + 15, Anchor.TOP | Anchor.LEFT);

        this.allowAdminCheckbox = new UICheckBox();
        this.allowAdminCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.allowAdminCheckbox.setPosition(this.accessLabel.getX() + this.accessLabel.getWidth() + 10, this.adminLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.allowAdminCheckbox.setName("checkbox.allowadmin");
        this.allowAdminCheckbox.setChecked(localGate.defaultAllowOutgoing);
        this.allowAdminCheckbox.register(this);

        this.denyAdminCheckbox = new UICheckBox();
        this.denyAdminCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.denyAdminCheckbox.setPosition(this.irisControllerLabel.getX() + this.accessLabel.getWidth() + 10 + this.allowAccessCheckbox.getWidth() + 10, this.adminLabel.getY(), Anchor.LEFT | Anchor.TOP);
        this.denyAdminCheckbox.setName("checkbox.denyadmin");
        this.denyAdminCheckbox.setChecked(!localGate.defaultAllowOutgoing);
        this.denyAdminCheckbox.register(this);

        this.saveOptionsButton = new UIButtonBuilder()
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.save"))
            .onClick(() -> {
                if (this.playerAccessList.getSize() > 0 && this.playerAccessList.getSelectedItem() != null) {
                    if (!this.playerAccessList.getSelectedItem().getPlayerName().isEmpty()) {
                        ConfiguratorNetworkHandler.sendPlayerAccessInputToServer(localGate, this.playerAccessList.getSelectedItem().getPlayerName(),false, false,  false,this.allowAccessCheckbox.isChecked(), this.allowIrisControllerCheckbox.isChecked(), this.allowAdminCheckbox.isChecked());
                        sendBasicMsg(player, "changesSaved");
                    }
                }
            })
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .position(-5,0)
            .build("button.save");

        this.optionsContainer.add(this.optionsLabel, optionsSeparator, this.accessLabel, this.allowAccessCheckbox, this.denyAccessCheckbox, irisControllerLabel, this.allowIrisControllerCheckbox, this.denyIrisControllerCheckbox, this.adminLabel, this.allowAdminCheckbox, this.denyAdminCheckbox, this.saveOptionsButton);

        // ****************************************************************************************************************************

        this.addPlayerButton = new UIButtonBuilder()
            .text(TextFormatting.GREEN + "+")
            .onClick(() -> {
                if (this.playerAccessList.getSize() == 0) {
                    new PlayerAccessEntryScreen(this, player, localGate, "", "", 1).display();
                } else {
                    new PlayerAccessEntryScreen(this, player, localGate, this.playerAccessList.getSelectedItem().getPlayerName(), this.playerAccessList.getSelectedItem().getPlayerName(), 1).display();
                }
            })
            .anchor(Anchor.TOP | Anchor.LEFT)
            .position(this.playerContainer.getWidth() + 17, 40)
            .build("button.add");

        this.editPlayerButton = new UIButtonBuilder()
            .text(TextFormatting.YELLOW + "?")
            .onClick(() -> {
                new PlayerAccessEntryScreen(this, player, localGate,this.playerAccessList.getSelectedItem().getPlayerName(), this.playerAccessList.getSelectedItem().getPlayerName(), 2).display();
            })
            .anchor(Anchor.TOP | Anchor.LEFT)
            .position(this.addPlayerButton.getX(), this.addPlayerButton.getY() + 20)
            .build("button.details");

        this.deletePlayerButton = new UIButtonBuilder()
            .text(TextFormatting.RED + "-")
            .onClick(() -> {
                new PlayerAccessEntryScreen(this, player, localGate,this.playerAccessList.getSelectedItem().getPlayerName(), this.playerAccessList.getSelectedItem().getPlayerName(), 3).display();
            })
            .anchor(Anchor.TOP | Anchor.LEFT)
            .position(this.addPlayerButton.getX(), this.editPlayerButton.getY() + 20)
            .build("button.remove");

        // ****************************************************************************************************************************

        final UISeparator defaultsSeparator = new UISeparator(false);
        defaultsSeparator.setSize(this.form.getWidth() - 15, 1);
        defaultsSeparator.setPosition(0, -50, Anchor.BOTTOM | Anchor.CENTER);

        this.gateDefaultAccessLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.defaultAccess")+":");
        this.gateDefaultAccessLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.gateDefaultAccessLabel.setPosition(5, -35, Anchor.BOTTOM | Anchor.LEFT);

        this.defaultAllowAccessCheckbox = new UICheckBox();
        this.defaultAllowAccessCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.defaultAllowAccessCheckbox.setPosition(this.gateDefaultAccessLabel.getX() + this.gateDefaultAccessLabel.getWidth() + 10, this.gateDefaultAccessLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultAllowAccessCheckbox.setName("checkbox.defaultallowaccess");
        this.defaultAllowAccessCheckbox.setChecked(localGate.defaultAllowGateAccess);
        this.defaultAllowAccessCheckbox.register(this);

        this.defaultDenyAccessCheckbox = new UICheckBox();
        this.defaultDenyAccessCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.defaultDenyAccessCheckbox.setPosition(this.gateDefaultAccessLabel.getX() + this.gateDefaultAccessLabel.getWidth() + 10 + this.defaultAllowAccessCheckbox.getWidth() + 10, this.gateDefaultAccessLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultDenyAccessCheckbox.setName("checkbox.defaultdenyaccess");
        this.defaultDenyAccessCheckbox.setChecked(!localGate.defaultAllowGateAccess);
        this.defaultDenyAccessCheckbox.register(this);

        this.gateDefaultIrisControllerLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.defaultIris")+":");
        this.gateDefaultIrisControllerLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.gateDefaultIrisControllerLabel.setPosition(5, -20, Anchor.BOTTOM | Anchor.LEFT);

        this.defaultAllowIrisCheckbox = new UICheckBox();
        this.defaultAllowIrisCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.defaultAllowIrisCheckbox.setPosition(this.gateDefaultAccessLabel.getX() + this.gateDefaultAccessLabel.getWidth() + 10, this.gateDefaultIrisControllerLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultAllowIrisCheckbox.setName("checkbox.defaultallowiris");
        this.defaultAllowIrisCheckbox.setChecked(localGate.defaultAllowIrisAccess);
        this.defaultAllowIrisCheckbox.register(this);

        this.defaultDenyIrisCheckbox = new UICheckBox();
        this.defaultDenyIrisCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.defaultDenyIrisCheckbox.setPosition(this.gateDefaultAccessLabel.getX() + this.gateDefaultAccessLabel.getWidth() + 10 + this.defaultAllowAccessCheckbox.getWidth() + 10, this.gateDefaultIrisControllerLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultDenyIrisCheckbox.setName("checkbox.defaultdenyiris");
        this.defaultDenyIrisCheckbox.setChecked(!localGate.defaultAllowIrisAccess);
        this.defaultDenyIrisCheckbox.register(this);

        this.gateDefaultAdminLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.defaultAdmin")+":");
        this.gateDefaultAdminLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.0F).build());
        this.gateDefaultAdminLabel.setPosition(5, -5, Anchor.BOTTOM | Anchor.LEFT);

        this.defaultAllowAdminCheckbox = new UICheckBox();
        this.defaultAllowAdminCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.allow"));
        this.defaultAllowAdminCheckbox.setPosition(this.gateDefaultAdminLabel.getX() + this.gateDefaultAccessLabel.getWidth() + 10, this.gateDefaultAdminLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultAllowAdminCheckbox.setName("checkbox.defaultallowadmin");
        this.defaultAllowAdminCheckbox.setChecked(localGate.defaultAllowAdminAccess);
        this.defaultAllowAdminCheckbox.register(this);

        this.defaultDenyAdminCheckbox = new UICheckBox();
        this.defaultDenyAdminCheckbox.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deny"));
        this.defaultDenyAdminCheckbox.setPosition(this.gateDefaultAdminLabel.getX() + this.gateDefaultAccessLabel.getWidth() + 10 + this.defaultAllowAccessCheckbox.getWidth() + 10, this.gateDefaultAdminLabel.getY(), Anchor.LEFT | Anchor.BOTTOM);
        this.defaultDenyAdminCheckbox.setName("checkbox.defaultdenyadmin");
        this.defaultDenyAdminCheckbox.setChecked(!localGate.defaultAllowAdminAccess);
        this.defaultDenyAdminCheckbox.register(this);

        buttonClose = new UIButtonBuilder()
                .width(40)
                .anchor(Anchor.BOTTOM | Anchor.RIGHT)
                .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.close"))
                .onClick(this::close)
                .build("button.close");

        this.saveDefaultOptionsButton = new UIButtonBuilder()
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.save"))
            .onClick(() -> {
                ConfiguratorNetworkHandler.sendPlayerAccessInputToServer(localGate, "",this.defaultAllowAccessCheckbox.isChecked(), this.defaultAllowIrisCheckbox.isChecked(),  this.defaultAllowAdminCheckbox.isChecked(),this.allowAccessCheckbox.isChecked(), this.allowIrisControllerCheckbox.isChecked(), this.allowAdminCheckbox.isChecked());
                sendBasicMsg(player, "changesSaved");
                this.close();
            })
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .position(-(buttonClose.getX() + buttonClose.getWidth() + 5),0)
            .build("button.savedefaults");

        this.form.add(this.playerContainer, defaultsSeparator, this.addPlayerButton, this.editPlayerButton, this.deletePlayerButton, this.gateDefaultAccessLabel, this.gateDefaultIrisControllerLabel, buttonClose);
        this.form.add(this.optionsContainer, saveDefaultOptionsButton);
        this.form.add(this.defaultAllowAccessCheckbox, this.defaultDenyAccessCheckbox, this.defaultAllowIrisCheckbox, this.defaultDenyIrisCheckbox, this.gateDefaultAdminLabel, this.defaultAllowAdminCheckbox, this.defaultDenyAdminCheckbox);
        addToScreen(this.form);

        loadData();
    }

    @Subscribe
    public void onListChange(BasicList.SelectEvent<PlayerAccessData> event) {
        boolean firstClick = (event.getOldValue() == null);
        final PlayerAccessData entry = event.getNewValue();
        if (localGate.getPlayerAccessData().size() > 0 && entry != null) {
            // The entry's own settings, as listed for the gate
            this.allowAccessCheckbox.setChecked(entry.hasGateAccess());
            this.denyAccessCheckbox.setChecked(!entry.hasGateAccess());
            this.allowIrisControllerCheckbox.setChecked(entry.hasIrisAccess());
            this.denyIrisControllerCheckbox.setChecked(!entry.hasIrisAccess());
            this.allowAdminCheckbox.setChecked(entry.isAdmin());
            this.denyAdminCheckbox.setChecked(!entry.isAdmin());
            this.optionsContainer.setEnabled(true);
        } else {
            this.optionsContainer.setEnabled(false);
        }
    }

    @Subscribe
    public void onValueChange(ComponentEvent.ValueChange event) {

        switch (event.getComponent().getName()) {
            case "checkbox.defaultallowaccess":
                this.defaultDenyAccessCheckbox.setChecked(this.defaultAllowAccessCheckbox.isChecked());
                break;
            case "checkbox.defaultdenyaccess":
                this.defaultAllowAccessCheckbox.setChecked(this.defaultDenyAccessCheckbox.isChecked());
                break;
            case "checkbox.defaultallowiris":
                this.defaultDenyIrisCheckbox.setChecked(this.defaultAllowIrisCheckbox.isChecked());
                break;
            case "checkbox.defaultdenyiris":
                this.defaultAllowIrisCheckbox.setChecked(this.defaultDenyIrisCheckbox.isChecked());
                break;
            case "checkbox.defaultallowadmin":
                this.defaultDenyAdminCheckbox.setChecked(this.defaultAllowAdminCheckbox.isChecked());
                break;
            case "checkbox.defaultdenyadmin":
                this.defaultAllowAdminCheckbox.setChecked(this.defaultDenyAdminCheckbox.isChecked());
                break;
            case "checkbox.allowincoming":
                this.denyAccessCheckbox.setChecked(this.allowAccessCheckbox.isChecked());
                break;
            case "checkbox.denyincoming":
                this.allowAccessCheckbox.setChecked(this.denyAccessCheckbox.isChecked());
                break;
            case "checkbox.allowoutgoing":
                this.denyIrisControllerCheckbox.setChecked(this.allowIrisControllerCheckbox.isChecked());
                break;
            case "checkbox.denyoutgoing":
                this.allowIrisControllerCheckbox.setChecked(this.denyIrisControllerCheckbox.isChecked());
                break;
            case "checkbox.allowadmin":
                this.denyAdminCheckbox.setChecked(this.allowAdminCheckbox.isChecked());
                break;
            case "checkbox.denyadmin":
                this.allowAdminCheckbox.setChecked(this.denyAdminCheckbox.isChecked());
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
            this.editPlayerButton.setEnabled(this.playerAccessList.getSize() > 0);
            this.deletePlayerButton.setEnabled(this.playerAccessList.getSize() > 0);
            this.optionsContainer.setEnabled(this.playerAccessList.getSize() > 0);
            if (!localGate.getPlayerAccessData().equals(this.clonedList)) {
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
                this.playerAccessList.setItems(localGate.getPlayerAccessData());
                this.playerAccessList.setSelectedItem(this.playerAccessList.getItems().stream().findFirst().orElse(null));
                this.clonedList = new ArrayList<>(localGate.getPlayerAccessData());
            }
        }
    }

    private class PlayerItemComponent extends BasicList.ItemComponent<PlayerAccessData> {

        private UILabel playerNameLabel;

        public PlayerItemComponent(final MalisisGui gui, final BasicList<PlayerAccessData> parent, final PlayerAccessData item) {
            super(gui, parent, item);
        }

        @Override
        protected void construct(final MalisisGui gui) {
            super.construct(gui);

            this.setHeight(15);
            this.setPadding(1);

            this.playerNameLabel = new UILabel(TextFormatting.WHITE + this.item.getPlayerName());
            this.playerNameLabel.setAnchor(Anchor.LEFT | Anchor.MIDDLE);
            this.playerNameLabel.setPosition(5, 0);


            this.add(this.playerNameLabel);
        }

        public void update() {

        }
    }
}
