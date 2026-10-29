package gcewing.sg.features.rings.client;

import gcewing.sg.features.rings.RingPlatformTE;
import gcewing.sg.features.rings.RingTransports;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.container.UIInventory;
import net.malisis.core.client.gui.component.container.UIPlayerInventory;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.inventory.MalisisInventory;
import net.malisis.core.inventory.MalisisInventoryContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;

import java.text.DecimalFormat;

// A ring platform's fuel screen, with MalisisCore: its energy, its naquadah and what it adds up to, where it's linked
// to, its fuel slots and the player's inventory to fill them from
public class RingFuelMalisisScreen extends BasicScreen {

    private static final DecimalFormat whole = new DecimalFormat("###,###,###,##0");

    private final RingPlatformTE te;
    private final MalisisInventory fuel;
    private UILabel energyLabel, naquadahLabel, usesLabel, linkLabel;
    private int lastUpdate = 0;
    private boolean unlockMouse = true;

    public RingFuelMalisisScreen(EntityPlayer player, RingPlatformTE te, MalisisInventoryContainer container, MalisisInventory fuel) {
        this.te = te;
        this.fuel = fuel;
        setInventoryContainer(container);
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;

        final BasicForm form = new BasicForm(184, 196, TextFormatting.WHITE + I18n.format("sgcraft.gui.rings.label.title"));
        form.setMovable(true);
        form.setBackgroundAlpha(255);

        this.energyLabel = label(2);
        this.naquadahLabel = label(14);
        this.usesLabel = label(26);
        this.linkLabel = label(38);

        final UIInventory fuelSlots = new UIInventory(TextFormatting.WHITE + I18n.format("sgcraft.gui.rings.label.fuel"),
            this.fuel, RingPlatformTE.numFuelSlots);
        fuelSlots.setPosition(0, 52, Anchor.TOP | Anchor.CENTER);
        final UIPlayerInventory playerInventory = new UIPlayerInventory(this.inventoryContainer.getPlayerInventory());
        playerInventory.setPosition(0, -3, Anchor.BOTTOM | Anchor.CENTER);

        form.add(this.energyLabel, this.naquadahLabel, this.usesLabel, this.linkLabel, fuelSlots, playerInventory);
        addToScreen(form);
        refresh();
    }

    private UILabel label(int y) {
        final UILabel label = new UILabel("");
        label.setPosition(5, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    private void refresh() {
        this.energyLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.rings.label.energy") + ": " + TextFormatting.WHITE
            + whole.format(this.te.energy) + TextFormatting.GRAY + " / " + whole.format(RingTransports.energyCapacity) + " FE");
        int naquadah = this.te.naquadahCount();
        this.naquadahLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.rings.label.naquadah") + ": " + TextFormatting.WHITE
            + whole.format(naquadah) + TextFormatting.GRAY + " (" + whole.format((long) naquadah * RingTransports.energyPerNaquadah) + " FE)");
        this.usesLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.rings.label.uses") + ": " + TextFormatting.YELLOW
            + whole.format(this.te.energyAvailable() / Math.max(1, RingTransports.energyPerUse)));
        this.linkLabel.setText(TextFormatting.GRAY + I18n.format("sgcraft.gui.rings.label.linked") + ": " + (this.te.linked
            ? TextFormatting.AQUA + (this.te.linkPos.getX() + ", " + this.te.linkPos.getY() + ", " + this.te.linkPos.getZ())
            : TextFormatting.RED + I18n.format("sgcraft.gui.rings.label.notLinked")));
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
        refresh(); // follows the naquadah put in, and the energy as the server sends it
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
