package gcewing.sg.features.naquadahgenerator.client.gui;

import gcewing.sg.SGCraft;
import gcewing.sg.features.naquadahgenerator.NaquadahGeneratorTE;
import gcewing.sg.features.naquadahgenerator.network.GeneratorStatus;
import gcewing.sg.features.naquadahgenerator.network.NaquadahGeneratorNetworkHandler;
import gcewing.sg.util.TimeFormat;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.container.UIInventory;
import net.malisis.core.client.gui.component.container.UIPlayerInventory;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.inventory.MalisisInventory;
import net.malisis.core.inventory.MalisisInventoryContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;

import java.text.DecimalFormat;

// A Naquadah generator's screen: whether it's running, the energy it holds and makes, how long its fuel will last, its
// fuel slots, and switches to turn it on and off
public class NaquadahGeneratorScreen extends BasicScreen {

    private static final DecimalFormat whole = new DecimalFormat("###,###,###,##0");

    private final EntityPlayer player;
    private final NaquadahGeneratorTE generator;
    private final MalisisInventory fuel;
    private GeneratorStatus status;
    private UILabel statusLabel, storedLabel, outputLabel, fuelLabel;
    private UIButton onButton, offButton;
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private long lastRequest = 0;

    public NaquadahGeneratorScreen(EntityPlayer player, NaquadahGeneratorTE generator, MalisisInventoryContainer container,
            MalisisInventory fuel) {
        this.player = player;
        this.generator = generator;
        this.fuel = fuel;
        // Until the server answers, what the client knows
        this.status = new GeneratorStatus();
        this.status.pos = generator.getPos();
        this.status.switchedOn = generator.switchedOn;
        this.status.running = generator.running;
        this.status.energy = generator.energy;
        this.status.capacity = generator.mark().capacity;
        this.status.energyPerTick = generator.mark().energyPerTick;
        setInventoryContainer(container);
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;

        final String title = I18n.format("sgcraft.gui.naquadahGenerator.label.title." + this.generator.mark().key());
        final BasicForm form = new BasicForm(184, 208, TextFormatting.WHITE + title);
        form.setMovable(true);
        form.setBackgroundAlpha(255);

        // ---- Its state, and the switches ----
        this.statusLabel = new UILabel("");
        this.statusLabel.setPosition(5, 2, Anchor.LEFT | Anchor.TOP);
        this.onButton = new UIButtonBuilder()
            .width(36)
            .anchor(Anchor.TOP | Anchor.RIGHT)
            .position(-41, 0)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.naquadahGenerator.button.on"))
            .onClick(() -> NaquadahGeneratorNetworkHandler.sendSwitchToServer(this.status.pos, true))
            .listener(this)
            .build("button.generator.on");
        this.offButton = new UIButtonBuilder()
            .width(36)
            .anchor(Anchor.TOP | Anchor.RIGHT)
            .position(-3, 0)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.naquadahGenerator.button.off"))
            .onClick(() -> NaquadahGeneratorNetworkHandler.sendSwitchToServer(this.status.pos, false))
            .listener(this)
            .build("button.generator.off");

        // ---- Its power ----
        this.storedLabel = infoLabel(24);
        this.outputLabel = infoLabel(36);
        this.fuelLabel = infoLabel(48);

        // ---- Its fuel, and the player's inventory to fill it from ----
        final UIInventory fuelSlots = new UIInventory(TextFormatting.WHITE + I18n.format("sgcraft.gui.naquadahGenerator.label.fuel"),
            this.fuel, NaquadahGeneratorTE.numFuelSlots);
        fuelSlots.setPosition(0, 62, Anchor.TOP | Anchor.CENTER);
        final UIPlayerInventory playerInventory = new UIPlayerInventory(this.inventoryContainer.getPlayerInventory());
        playerInventory.setPosition(0, -3, Anchor.BOTTOM | Anchor.CENTER);

        form.add(this.statusLabel, this.onButton, this.offButton, this.storedLabel, this.outputLabel, this.fuelLabel, fuelSlots,
            playerInventory);
        addToScreen(form);
        refresh();
    }

    private UILabel infoLabel(int y) {
        final UILabel label = new UILabel("");
        label.setPosition(5, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    public void setStatus(GeneratorStatus status) {
        if (status.pos.equals(this.status.pos)) {
            this.status = status;
            refresh();
        }
    }

    private void refresh() {
        String state;
        if (!this.status.switchedOn) {
            state = TextFormatting.GRAY + I18n.format("sgcraft.gui.naquadahGenerator.label.off");
        } else if (this.status.running) {
            state = TextFormatting.AQUA + I18n.format("sgcraft.gui.naquadahGenerator.label.running");
        } else {
            state = TextFormatting.RED + I18n.format("sgcraft.gui.naquadahGenerator.label.noFuel");
        }
        this.statusLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.naquadahGenerator.label.status") + ": " + state);

        this.storedLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.naquadahGenerator.label.stored") + ": "
            + TextFormatting.WHITE + whole.format(this.status.energy) + TextFormatting.GRAY + " / " + whole.format(this.status.capacity));
        this.outputLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.naquadahGenerator.label.output") + ": "
            + TextFormatting.YELLOW + whole.format(this.status.energyPerTick) + TextFormatting.GRAY + " ("
            + whole.format(this.status.energyPerTick * SGCraft.FPPerSGEnergyUnit) + " FE)");
        double ticks = this.status.energyPerTick > 0 ? this.status.fuelEnergy / this.status.energyPerTick : 0;
        this.fuelLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.naquadahGenerator.label.fuelTime") + ": "
            + (ticks > 0 ? TextFormatting.WHITE + TimeFormat.runTime(ticks) : TextFormatting.RED + I18n.format("sgcraft.gui.naquadahGenerator.label.empty")));

        this.onButton.setEnabled(!this.status.switchedOn);
        this.offButton.setEnabled(this.status.switchedOn);
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
            NaquadahGeneratorNetworkHandler.requestStatusFromServer(this.status.pos);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
