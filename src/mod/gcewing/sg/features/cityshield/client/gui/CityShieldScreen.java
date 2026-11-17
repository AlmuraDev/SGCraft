package gcewing.sg.features.cityshield.client.gui;

import gcewing.sg.features.cityshield.CityShieldTE;
import gcewing.sg.features.cityshield.network.CityShieldNetworkHandler;
import gcewing.sg.features.cityshield.network.CityShieldStatus;
import gcewing.sg.util.TimeFormat;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.container.UIInventory;
import net.malisis.core.client.gui.component.container.UIPlayerInventory;
import net.malisis.core.inventory.MalisisInventory;
import net.malisis.core.inventory.MalisisInventoryContainer;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.text.DecimalFormat;

// The City Shield Generator's screen: the shield's size, what it costs to run, and buttons to raise and lower it
public class CityShieldScreen extends BasicScreen {

    private static final DecimalFormat whole = new DecimalFormat("###,###,###,###,##0");
    private static final DecimalFormat fraction = new DecimalFormat("###,###,##0.##");

    private final EntityPlayer player;
    private CityShieldStatus status;
    private UITextField widthField, aboveField, belowField;
    private UILabel statusLabel, volumeLabel, costLabel, energyLabel, runTimeLabel;
    private UIButton applyButton, activateButton, deactivateButton;
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private long lastRequest = 0;

    // The generator's own ZPM slots, shown with the player's inventory
    private final MalisisInventory zpms;

    public CityShieldScreen(EntityPlayer player, CityShieldStatus status, MalisisInventoryContainer container, MalisisInventory zpms) {
        this.player = player;
        this.status = status;
        this.zpms = zpms;
        setInventoryContainer(container);
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;
        Keyboard.enableRepeatEvents(true);

        final BasicForm form = new BasicForm(320, 300, "");
        form.setAnchor(Anchor.CENTER | Anchor.MIDDLE);
        form.setMovable(true);
        form.setClosable(true);
        form.setBorder(FontColors.WHITE, 1, 185);
        form.setBackgroundAlpha(215);
        form.setBottomPadding(3);
        form.setRightPadding(3);
        form.setTopPadding(20);
        form.setLeftPadding(3);

        final UILabel titleLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.cityshield.label.title"));
        titleLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        titleLabel.setPosition(0, -15, Anchor.CENTER | Anchor.TOP);

        this.statusLabel = new UILabel("");
        this.statusLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.2F).build());
        this.statusLabel.setPosition(0, 0, Anchor.CENTER | Anchor.TOP);

        final UILabel sizeLabel = new UILabel(TextFormatting.GRAY + I18n.format("sgcraft.gui.cityshield.label.size", this.status.maxRadius));
        sizeLabel.setPosition(0, 18, Anchor.LEFT | Anchor.TOP);

        this.widthField = sizeField("width", this.status.width, 32);
        this.aboveField = sizeField("above", this.status.above, 47);
        this.belowField = sizeField("below", this.status.below, 62);

        this.volumeLabel = infoLabel(82);
        this.costLabel = infoLabel(94);
        this.energyLabel = infoLabel(106);
        this.runTimeLabel = infoLabel(118);
        // The buttons, between the read-out and the player's inventory
        final int buttonsY = 138;

        // The generator's own ZPMs (it draws on any nearby too), and the player's inventory to fill them from
        final UIInventory zpmSlots = new UIInventory(TextFormatting.WHITE + I18n.format("sgcraft.gui.cityshield.label.zpms"), this.zpms, CityShieldTE.numZpmSlots);
        zpmSlots.setPosition(-15, 32, Anchor.TOP | Anchor.RIGHT);
        final UIPlayerInventory playerInventory = new UIPlayerInventory(this.inventoryContainer.getPlayerInventory());
        playerInventory.setPosition(0, -6, Anchor.BOTTOM | Anchor.CENTER);

        this.applyButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.TOP | Anchor.CENTER)
            .position(0, buttonsY)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.apply"))
            .onClick(() -> send(this.status.active))
            .listener(this)
            .build("button.cityshield.apply");

        this.activateButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.TOP | Anchor.LEFT)
            .position(0, buttonsY)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.activateShield"))
            .onClick(() -> send(true))
            .listener(this)
            .build("button.cityshield.activate");

        this.deactivateButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.TOP | Anchor.RIGHT)
            .position(0, buttonsY)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.deactivateShield"))
            .onClick(() -> send(false))
            .listener(this)
            .build("button.cityshield.deactivate");

        form.add(titleLabel, this.statusLabel, sizeLabel);
        addSizeRow(form, "width", this.widthField, 32);
        addSizeRow(form, "above", this.aboveField, 47);
        addSizeRow(form, "below", this.belowField, 62);
        form.add(zpmSlots, playerInventory);
        form.add(this.volumeLabel, this.costLabel, this.energyLabel, this.runTimeLabel, this.applyButton, this.activateButton, this.deactivateButton);
        addToScreen(form);
        refresh();
    }

    private UITextField sizeField(String name, int value, int y) {
        final UITextField field = new UITextField("");
        field.setText(String.valueOf(value));
        field.setSize(50, 0);
        field.setPosition(135, y, Anchor.LEFT | Anchor.TOP);
        field.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(false).build());
        field.setName("field.cityshield." + name);
        return field;
    }

    private void addSizeRow(BasicForm form, String name, UITextField field, int y) {
        final UILabel label = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.cityshield.label." + name) + ":");
        label.setPosition(10, y + 1, Anchor.LEFT | Anchor.TOP);
        form.add(label, field);
    }

    private UILabel infoLabel(int y) {
        final UILabel label = new UILabel("");
        label.setPosition(10, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    // A size field's value, kept within what the server allows; an unreadable one counts as the current size
    private int sizeOf(UITextField field, int current) {
        try {
            return Math.max(CityShieldTE.MIN_RADIUS, Math.min(this.status.maxRadius, Integer.parseInt(field.getText().trim())));
        } catch (NumberFormatException e) {
            return current;
        }
    }

    private void send(boolean active) {
        CityShieldNetworkHandler.sendInputToServer(this.status.pos, sizeOf(this.widthField, this.status.width),
            sizeOf(this.aboveField, this.status.above), sizeOf(this.belowField, this.status.below), active);
    }

    public void setStatus(CityShieldStatus status) {
        if (status.pos.equals(this.status.pos)) {
            this.status = status;
            refresh();
        }
    }

    // Shows the generator's state, and what the size in the fields would cost
    private void refresh() {
        this.statusLabel.setText(this.status.active
            ? TextFormatting.AQUA + I18n.format("sgcraft.gui.shieldconsole.label.active")
            : TextFormatting.GRAY + I18n.format("sgcraft.gui.shieldconsole.label.inactive"));

        int width = sizeOf(this.widthField, this.status.width);
        int above = sizeOf(this.aboveField, this.status.above);
        int below = sizeOf(this.belowField, this.status.below);
        double volume = CityShieldTE.volume(width, above, below);
        double perTick = volume * this.status.energyPerBlockPerTick;

        this.volumeLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.cityshield.label.volume") + ": "
            + TextFormatting.WHITE + whole.format(volume));
        this.costLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.cityshield.label.cost") + ": "
            + TextFormatting.YELLOW + fraction.format(perTick));
        this.energyLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.cityshield.label.energy") + ": "
            + (this.status.availableEnergy > 0 ? TextFormatting.GREEN : TextFormatting.RED) + whole.format(this.status.availableEnergy));
        this.runTimeLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.cityshield.label.runTime") + ": "
            + TextFormatting.WHITE + TimeFormat.runTime(perTick > 0 ? this.status.availableEnergy / perTick : 0));

        this.activateButton.setEnabled(!this.status.active);
        this.deactivateButton.setEnabled(this.status.active);
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
        if (System.currentTimeMillis() > this.lastRequest + 1000) {
            this.lastRequest = System.currentTimeMillis();
            CityShieldNetworkHandler.requestStatusFromServer(this.status.pos);
        }
        refresh(); // follows the size being typed
    }

    // Tab moves between the size fields, width to above to below and round again (shift-tab goes back)
    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        if (keyCode == Keyboard.KEY_TAB) {
            UITextField[] fields = {this.widthField, this.aboveField, this.belowField};
            int step = isShiftKeyDown() ? fields.length - 1 : 1;
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isFocused()) {
                    fields[(i + step) % fields.length].setFocused(true);
                    return;
                }
            }
            this.widthField.setFocused(true);
            return;
        }
        super.keyTyped(keyChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
