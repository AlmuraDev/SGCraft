package gcewing.sg.features.configurator.client.gui;

import gcewing.sg.features.zpm.link.ILinkableZpm;
import gcewing.sg.features.zpm.link.ZpmLinkNetworkHandler;
import gcewing.sg.tileentity.SGBaseTE;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The ZPM devices linked to a gate (see ZpmLinks), from its Configurator screen: what each is, where, and how far from
// the gate's base, with an X on its left to unlink it
public class LinkedZpmsScreen extends BasicScreen {

    // As many as fit; any more are counted on a last line
    private static final int maxRows = 12, rowHeight = 14, minWidth = 200;

    private final BasicScreen parentScreen;
    private final World world;
    private final SGBaseTE gate;
    // Devices unlinked from here, left out until the gate's next update from the server leaves them out too
    private final Set<BlockPos> unlinked;

    public LinkedZpmsScreen(BasicScreen parent, World world, SGBaseTE gate) {
        this(parent, world, gate, new HashSet<>());
    }

    private LinkedZpmsScreen(BasicScreen parent, World world, SGBaseTE gate, Set<BlockPos> unlinked) {
        super(parent, true);
        this.parentScreen = parent;
        this.world = world;
        this.gate = gate;
        this.unlinked = unlinked;
    }

    private static final class Row {
        final BlockPos pos;
        final String text;

        Row(BlockPos pos, String text) {
            this.pos = pos;
            this.text = text;
        }
    }

    @Override
    public void construct() {
        this.guiscreenBackground = false;

        List<Row> rows = new ArrayList<>();
        BlockPos g = this.gate.getPos();
        for (BlockPos p : this.gate.linkedZpmPositions()) {
            if (this.unlinked.contains(p)) {
                continue;
            }
            String name;
            if (!this.world.isBlockLoaded(p)) {
                name = TextFormatting.GRAY + I18n.format("sgcraft.gui.linkedZpms.label.notLoaded");
            } else {
                // The gate's own list is what counts (the server keeps it tidy); a device's copy here may be behind
                TileEntity te = this.world.getTileEntity(p);
                if (te instanceof ILinkableZpm) {
                    name = TextFormatting.WHITE + te.getBlockType().getLocalizedName()
                        + (((ILinkableZpm) te).hasNoZpm() ? TextFormatting.RED + " (" + I18n.format("sgcraft.gui.linkedZpms.label.empty") + ")" : "");
                } else {
                    name = TextFormatting.RED + I18n.format("sgcraft.gui.linkedZpms.label.gone");
                }
            }
            String distance = I18n.format("sgcraft.gui.zpmLink.label.blocks", (int) Math.round(Math.sqrt(p.distanceSq(g))));
            rows.add(new Row(p, name + TextFormatting.GRAY + "  " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " - " + distance));
        }

        int shown = Math.min(rows.size(), maxRows);
        int lines = rows.isEmpty() ? 3 : shown + (rows.size() > maxRows ? 1 : 0);

        // The lines first, so the window can be as wide as the widest of them (a label is as wide as its text)
        List<UIComponent> content = new ArrayList<>();
        int widest = 0;
        if (rows.isEmpty()) {
            UILabel none = label(TextFormatting.GRAY + I18n.format("sgcraft.gui.linkedZpms.label.none"), 5, 2);
            content.add(none);
            content.add(hint("sgcraft.gui.zpmLink.label.hint1", 16));
            content.add(hint("sgcraft.gui.zpmLink.label.hint2", 25));
            for (UIComponent c : content) {
                widest = Math.max(widest, 5 + c.getWidth());
            }
        } else {
            for (int i = 0; i < shown; i++) {
                Row row = rows.get(i);
                int y = 3 + i * rowHeight;
                // An X on its left unlinks it
                content.add(new UIButtonBuilder()
                    .size(12, 12)
                    .anchor(Anchor.TOP | Anchor.LEFT)
                    .position(3, y - 2)
                    .text(TextFormatting.RED + "x")
                    .onClick(() -> unlink(row.pos))
                    .listener(this)
                    .build("button.linkedZpms.unlink." + i));
                UILabel text = label(row.text, 19, y);
                content.add(text);
                widest = Math.max(widest, 19 + text.getWidth());
            }
            if (rows.size() > maxRows) {
                UILabel more = label(TextFormatting.GRAY + I18n.format("sgcraft.gui.linkedZpms.label.more", rows.size() - maxRows),
                    19, 3 + shown * rowHeight);
                content.add(more);
                widest = Math.max(widest, 19 + more.getWidth());
            }
        }

        // A little room either side of the widest line, and never narrower than its title and Close button need
        final String title = I18n.format("sgcraft.gui.linkedZpms.label.title", rows.size());
        int width = Math.max(Math.max(widest + 10, minWidth), Minecraft.getMinecraft().fontRenderer.getStringWidth(title) + 60);
        final BasicForm form = new BasicForm(width, 40 + lines * rowHeight, TextFormatting.WHITE + title);
        form.setMovable(true);
        form.setBackgroundAlpha(255);
        for (UIComponent c : content) {
            form.add(c);
        }

        final UIButton closeButton = new UIButtonBuilder()
            .width(40)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.close"))
            .onClick(this::close)
            .listener(this)
            .build("button.linkedZpms.close");
        form.add(closeButton);
        addToScreen(form);
    }

    // Asks the server to unlink the device, and shows the list without it at once
    private void unlink(BlockPos devicePos) {
        ZpmLinkNetworkHandler.sendGateUnlinkToServer(this.gate.getPos(), devicePos);
        this.unlinked.add(devicePos);
        new LinkedZpmsScreen(this.parentScreen, this.world, this.gate, this.unlinked).display();
    }

    private UILabel label(String text, int x, int y) {
        final UILabel label = new UILabel(text);
        label.setPosition(x, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    private UILabel hint(String key, int y) {
        final UILabel label = new UILabel(TextFormatting.GRAY + I18n.format(key));
        label.setFontOptions(FontOptions.builder().from(FontColors.GRAY_FO).scale(0.8F).build());
        label.setPosition(5, y, Anchor.LEFT | Anchor.TOP);
        return label;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
