package gcewing.sg.features.obelisk.client;

import com.google.common.eventbus.Subscribe;
import gcewing.sg.features.obelisk.ObeliskAccess;
import gcewing.sg.features.obelisk.ObeliskConfig;
import gcewing.sg.features.obelisk.ObeliskNetworkHandler;
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
import net.malisis.core.client.gui.component.interaction.UIRadioButton;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

// An Asgard obelisk's configurator screen (opened by clicking it with the Configurator): where it's linked to, and who
// may use it. By default everyone may, save players listed as denied; or it can be only its owner and players listed as
// allowed. Listed players can also be made admins, who may configure it.
public class ObeliskConfiguratorScreen extends BasicScreen {

    private ObeliskConfig config;
    private static final DecimalFormat whole = new DecimalFormat("###,###,###,##0");
    // The list as edited here, until saved
    private final List<ObeliskAccess> access = new ArrayList<>();

    private UILabel linkLabel, ownerLabel, powerLabel, naquadahLabel, selectedLabel;
    private UIButton unlinkButton, removeButton;
    private BasicList<ObeliskAccess> playerList;
    private UITextField nameField;
    private UIRadioButton useAllowRadio, useDenyRadio, adminAllowRadio, adminDenyRadio, everyoneRadio, listedOnlyRadio;
    private int lastUpdate = 0;
    private boolean unlockMouse = true;

    public ObeliskConfiguratorScreen(ObeliskConfig config) {
        this.config = config;
        this.access.addAll(config.access);
    }

    // Opens the screen, or brings one already open on that obelisk up to date (after saving or unlinking)
    public static void show(EntityPlayer player, ObeliskConfig config) {
        if (Minecraft.getMinecraft().currentScreen instanceof ObeliskConfiguratorScreen) {
            ObeliskConfiguratorScreen screen = (ObeliskConfiguratorScreen) Minecraft.getMinecraft().currentScreen;
            if (screen.config.pos.equals(config.pos)) {
                screen.setConfig(config);
                return;
            }
        }
        new ObeliskConfiguratorScreen(config).display();
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;
        Keyboard.enableRepeatEvents(true);

        // A transporter with power (ring platforms) has a line for it under the owner, and the rest moves down to make room
        final int powerRoom = this.config.hasPower ? 22 : 0;
        final BasicForm form = new BasicForm(360, 224 + powerRoom, TextFormatting.WHITE + I18n.format(this.config.titleKey));
        form.setMovable(true);
        form.setBackgroundAlpha(255);

        // ---- Where it's linked to ----
        this.linkLabel = new UILabel("");
        this.linkLabel.setPosition(5, 2, Anchor.LEFT | Anchor.TOP);
        this.ownerLabel = new UILabel("");
        this.ownerLabel.setPosition(5, 12, Anchor.LEFT | Anchor.TOP);
        this.powerLabel = new UILabel("");
        this.powerLabel.setPosition(5, 22, Anchor.LEFT | Anchor.TOP);
        this.naquadahLabel = new UILabel("");
        this.naquadahLabel.setPosition(5, 32, Anchor.LEFT | Anchor.TOP);
        this.unlinkButton = new UIButtonBuilder()
            .width(50)
            .anchor(Anchor.TOP | Anchor.RIGHT)
            .position(-3, 2)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.obelisk.button.unlink"))
            .onClick(() -> ObeliskNetworkHandler.sendUnlinkToServer(this.config.pos))
            .listener(this)
            .build("button.obelisk.unlink");

        // ---- The players ----
        final BasicContainer players = new BasicContainer(165, 160);
        players.setPosition(0, 23 + powerRoom, Anchor.LEFT | Anchor.TOP);
        players.setBorder(FontColors.WHITE, 1, 185);
        players.setPadding(0, 3);
        players.setBackgroundAlpha(0);

        final UILabel playersLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.playerList"));
        playersLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        this.playerList = new BasicList<>(UIComponent.INHERITED, 112);
        this.playerList.setPosition(0, 10);
        this.playerList.setItemComponentFactory(PlayerItem::new);
        this.playerList.setItemComponentSpacing(1);
        this.playerList.setPadding(2);
        this.playerList.setName("List");
        this.playerList.register(this);
        this.playerList.setBorder(FontColors.WHITE, 1, 185);

        final UILabel nameLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.obelisk.label.typeName"));
        nameLabel.setPosition(3, -17, Anchor.LEFT | Anchor.BOTTOM);

        this.nameField = new UITextField("");
        this.nameField.setSize(100, 0);
        this.nameField.setPosition(3, -3, Anchor.LEFT | Anchor.BOTTOM);
        this.nameField.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(false).build());

        final UIButton addButton = new UIButtonBuilder()
            .width(22)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .position(-30, -2)
            .text(TextFormatting.GREEN + "+")
            .onClick(this::addPlayer)
            .listener(this)
            .build("button.obelisk.add");
        this.removeButton = new UIButtonBuilder()
            .width(22)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .position(-4, -2)
            .text(TextFormatting.RED + "-")
            .onClick(this::removePlayer)
            .listener(this)
            .build("button.obelisk.remove");
        players.add(playersLabel, this.playerList, nameLabel, this.nameField, addButton, this.removeButton);

        // ---- The selected player's options, and the default ----
        final BasicContainer options = new BasicContainer(180, 160);
        options.setPosition(0, 23 + powerRoom, Anchor.RIGHT | Anchor.TOP);
        options.setBorder(FontColors.WHITE, 1, 185);
        options.setPadding(0, 3);
        options.setBackgroundAlpha(0);

        final UILabel optionsLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.options"));
        optionsLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);
        // A line under the heading, the panel's full width, drawn as the player list's outline is (a separator draws thicker)
        final BasicContainer optionsLine = new BasicContainer(options.getWidth(), 1);
        optionsLine.setBorders(FontColors.WHITE, 185, 0, 1, 0, 0);
        optionsLine.setBackgroundAlpha(0);
        optionsLine.setPosition(0, 10, Anchor.TOP | Anchor.CENTER);
        this.selectedLabel = new UILabel("");
        this.selectedLabel.setPosition(5, 14, Anchor.LEFT | Anchor.TOP);

        final UILabel useLabel = label("access", 32);
        final String useGroup = group("use");
        this.useAllowRadio = radio(useGroup, "sgcraft.gui.configurator.label.allow", 70, 32);
        this.useDenyRadio = radio(useGroup, "sgcraft.gui.configurator.label.deny", 120, 32);
        final UILabel adminLabel = label("admin", 48);
        final String adminGroup = group("admin");
        this.adminAllowRadio = radio(adminGroup, "sgcraft.gui.configurator.label.allow", 70, 48);
        this.adminDenyRadio = radio(adminGroup, "sgcraft.gui.configurator.label.deny", 120, 48);

        final UISeparator defaultSeparator = new UISeparator(false);
        defaultSeparator.setSize(160, 1);
        defaultSeparator.setPosition(0, 72, Anchor.TOP | Anchor.CENTER);
        final UILabel defaultLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.defaultAccess") + ":");
        defaultLabel.setPosition(5, 80, Anchor.LEFT | Anchor.TOP);
        final String defaultGroup = group("default");
        this.everyoneRadio = radio(defaultGroup, "sgcraft.gui.obelisk.label.everyone", 10, 96);
        this.listedOnlyRadio = radio(defaultGroup, "sgcraft.gui.obelisk.label.listedOnly", 10, 112);
        final UILabel defaultHint = new UILabel(TextFormatting.GRAY + I18n.format("sgcraft.gui.obelisk.label.defaultHint"));
        defaultHint.setFontOptions(FontOptions.builder().from(FontColors.GRAY_FO).scale(0.8F).build());
        defaultHint.setPosition(5, 130, Anchor.LEFT | Anchor.TOP);

        options.add(optionsLabel, optionsLine, this.selectedLabel, useLabel, this.useAllowRadio, this.useDenyRadio, adminLabel,
            this.adminAllowRadio, this.adminDenyRadio, defaultSeparator, defaultLabel, this.everyoneRadio, this.listedOnlyRadio, defaultHint);

        // ---- Saving ----
        final UIButton saveButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .position(-45, 0)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.save"))
            .onClick(() -> ObeliskNetworkHandler.sendConfigToServer(this.config.pos, this.everyoneRadio.isSelected(), this.access))
            .listener(this)
            .build("button.obelisk.save");
        final UIButton closeButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.close"))
            .onClick(this::close)
            .listener(this)
            .build("button.obelisk.close");

        form.add(this.linkLabel, this.ownerLabel, this.powerLabel, this.naquadahLabel, this.unlinkButton, players, options, saveButton, closeButton);
        addToScreen(form);
        setConfig(this.config);
    }

    // MalisisCore groups radio buttons by name in a map that is never cleared, so each screen needs its own groups
    private String group(String name) {
        return "radio.obelisk." + name + "." + System.identityHashCode(this);
    }

    private UIRadioButton radio(String group, String langKey, int x, int y) {
        final UIRadioButton radio = new UIRadioButton(group, TextFormatting.WHITE + I18n.format(langKey));
        radio.setPosition(x, y, Anchor.LEFT | Anchor.TOP);
        radio.register(this);
        return radio;
    }

    private UILabel label(String langKey, int y) {
        final UILabel label = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label." + langKey) + ":");
        label.setPosition(5, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    // Shows the obelisk's settings as the server has them
    public void setConfig(ObeliskConfig config) {
        this.config = config;
        this.access.clear();
        this.access.addAll(config.access);
        if (config.linked) {
            this.linkLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.obelisk.label.linkedTo") + ": " + TextFormatting.AQUA
                + config.linkWorld + TextFormatting.GRAY + "  (" + config.linkPos.getX() + ", " + config.linkPos.getY() + ", "
                + config.linkPos.getZ() + ")");
        } else {
            this.linkLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.obelisk.label.notLinked"));
        }
        this.ownerLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.obelisk.label.owner") + ": "
            + (config.ownerName.isEmpty() ? "-" : config.ownerName));
        this.unlinkButton.setEnabled(config.linked);
        if (config.hasPower) {
            // On two lines, which one would overrun the window
            this.powerLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.rings.label.energy") + ": " + TextFormatting.WHITE
                + whole.format(config.energy) + TextFormatting.GRAY + " / " + whole.format(config.capacity) + " FE    "
                + I18n.format("sgcraft.gui.rings.label.uses") + ": " + TextFormatting.YELLOW + whole.format(config.uses));
            this.naquadahLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.rings.label.naquadah") + ": " + TextFormatting.WHITE
                + whole.format(config.naquadah) + TextFormatting.GRAY + " (" + whole.format(config.naquadahEnergy) + " FE)");
        }
        (config.allowEveryone ? this.everyoneRadio : this.listedOnlyRadio).setSelected();
        refreshList(null);
    }

    private void refreshList(ObeliskAccess select) {
        this.playerList.setItems(new ArrayList<>(this.access));
        this.playerList.setSelectedItem(select != null ? select : this.access.isEmpty() ? null : this.access.get(0));
        showSelected();
    }

    // The selected player's options, or none
    private void showSelected() {
        ObeliskAccess selected = this.playerList.getSelectedItem();
        boolean any = selected != null;
        this.selectedLabel.setText(any ? TextFormatting.AQUA + selected.name : TextFormatting.GRAY + I18n.format("sgcraft.gui.obelisk.label.noneSelected"));
        this.useAllowRadio.setEnabled(any);
        this.useDenyRadio.setEnabled(any);
        this.adminAllowRadio.setEnabled(any);
        this.adminDenyRadio.setEnabled(any);
        this.removeButton.setEnabled(any);
        if (any) {
            (selected.use ? this.useAllowRadio : this.useDenyRadio).setSelected();
            (selected.admin ? this.adminAllowRadio : this.adminDenyRadio).setSelected();
        }
    }

    private void addPlayer() {
        String name = this.nameField.getText().trim();
        if (name.isEmpty() || this.access.stream().anyMatch(e -> e.name.equalsIgnoreCase(name))) {
            return;
        }
        ObeliskAccess entry = new ObeliskAccess(null, name, true, false);
        this.access.add(entry);
        this.nameField.setText("");
        refreshList(entry);
    }

    private void removePlayer() {
        ObeliskAccess selected = this.playerList.getSelectedItem();
        if (selected != null) {
            this.access.remove(selected);
            refreshList(null);
        }
    }

    @Subscribe
    public void onListSelect(BasicList.SelectEvent<ObeliskAccess> event) {
        // Fired before the selection changes, so look again once it has
        Minecraft.getMinecraft().addScheduledTask(this::showSelected);
    }

    // Fired as a radio button is clicked, before it is selected
    @Subscribe
    public void onRadioSelect(UIRadioButton.SelectEvent event) {
        ObeliskAccess selected = this.playerList.getSelectedItem();
        if (selected == null) {
            return;
        }
        UIRadioButton clicked = event.getComponent();
        if (clicked == this.useAllowRadio || clicked == this.useDenyRadio) {
            selected.use = clicked == this.useAllowRadio;
        } else if (clicked == this.adminAllowRadio || clicked == this.adminDenyRadio) {
            selected.admin = clicked == this.adminAllowRadio;
        }
        // Its tags in the list show the change
        Minecraft.getMinecraft().addScheduledTask(() -> refreshList(selected));
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && this.nameField.isFocused()) {
            addPlayer(); // Enter in the name field adds the player
            return;
        }
        super.keyTyped(keyChar, keyCode);
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

    // A listed player: their name, and whether they may use it and are an admin
    private class PlayerItem extends BasicList.ItemComponent<ObeliskAccess> {

        PlayerItem(final MalisisGui gui, final BasicList<ObeliskAccess> parent, final ObeliskAccess item) {
            super(gui, parent, item);
        }

        @Override
        protected void construct(final MalisisGui gui) {
            super.construct(gui);
            this.setHeight(15);
            this.setPadding(1);
            final UILabel name = new UILabel(TextFormatting.WHITE + this.item.name);
            name.setAnchor(Anchor.LEFT | Anchor.MIDDLE);
            name.setPosition(5, 0);
            final UILabel tags = new UILabel((this.item.use ? TextFormatting.GREEN + I18n.format("sgcraft.gui.obelisk.label.tagUse")
                : TextFormatting.RED + I18n.format("sgcraft.gui.obelisk.label.tagDenied"))
                + (this.item.admin ? TextFormatting.GOLD + " " + I18n.format("sgcraft.gui.obelisk.label.tagAdmin") : ""));
            tags.setAnchor(Anchor.RIGHT | Anchor.MIDDLE);
            tags.setPosition(-5, 0);
            this.add(name, tags);
        }
    }
}
