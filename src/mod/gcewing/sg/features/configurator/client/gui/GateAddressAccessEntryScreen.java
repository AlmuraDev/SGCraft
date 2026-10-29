package gcewing.sg.features.configurator.client.gui;

import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;

import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.features.pdd.network.PddNetworkHandler;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.tileentity.data.GateAccessData;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GateAddressAccessEntryScreen extends BasicScreen {
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private BasicForm form;
    private UITextField addressTextField;
    private EntityPlayer player;
    public int function;
    public String oldAddress, newAddress;
    private SGBaseTE localGate;

    public GateAddressAccessEntryScreen(BasicScreen parent, EntityPlayer player, SGBaseTE localGate, String oldAddress, String newAddress, int function) {
        super(parent, true);
        this.player = player;
        this.oldAddress = oldAddress;
        this.newAddress = newAddress;
        this.function = function;
        this.localGate = localGate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void construct() {
        this.guiscreenBackground = false;
        Keyboard.enableRepeatEvents(true);

        // Master Panel
        this.form = new BasicForm(180, 100, "");
        this.form.setAnchor(Anchor.CENTER | Anchor.MIDDLE);
        this.form.setMovable(true);
        this.form.setClosable(true);
        this.form.setBorder(FontColors.WHITE, 1, 185);
        this.form.setBackgroundAlpha(255);
        this.form.setBottomPadding(3);
        this.form.setRightPadding(3);
        this.form.setTopPadding(20);
        this.form.setLeftPadding(3);

        final UILabel titleLabel = new UILabel("");
        titleLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        titleLabel.setPosition(0, -15, Anchor.CENTER | Anchor.TOP);

        if (function == 1) {
            titleLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.addGateAddress"));
        } else if (function == 2) {
            titleLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.editGateAddress"));
        } else if (function == 3) {
            titleLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.deleteGateAddress"));
        }

        final UILabel addressLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.address")+":");
        addressLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        addressLabel.setPosition(15, 10, Anchor.LEFT | Anchor.TOP);

        this.addressTextField = new UITextField("");
        if (this.oldAddress.isEmpty()) {
            this.addressTextField.setText(this.newAddress);
        } else {
            this.addressTextField.setText(this.oldAddress);
        }
        this.addressTextField.setSize(80, 0);
        this.addressTextField.setPosition(addressLabel.getX() + addressLabel.getWidth() + 10, addressLabel.getY() - 1, Anchor.LEFT | Anchor.TOP);
        this.addressTextField.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(false).build());

        final UILabel addressFormatLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.configurator.label.format")+": XXXX-XXX-XX");
        addressFormatLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).italic(true).shadow(true).scale(0.9F).build());
        addressFormatLabel.setPosition(0, 25, Anchor.CENTER | Anchor.TOP);

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

        // Delete Feature button
        final UIButton buttonDelete = new UIButtonBuilder()
                .width(40)
                .position(-(buttonClose.getX() + buttonClose.getWidth() + 5), 0)
                .visible(function == 3)
                .anchor(Anchor.BOTTOM | Anchor.RIGHT)
                .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.delete"))
                .onClick(() -> {
                    ConfiguratorNetworkHandler.sendGAAEntryUpdateToServer(localGate, this.oldAddress, this.addressTextField.getText().trim(), function);
                    this.close();
                })
                .listener(this)
                .build("button.delete");

        // Save Feature button
        final UIButton buttonSave = new UIButtonBuilder()
                .width(40)
                .position(-(buttonClose.getX() + buttonClose.getWidth() + 5), 0)
                .visible(!(function == 3))
                .anchor(Anchor.BOTTOM | Anchor.RIGHT)
                .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.save"))
                .onClick(this::saveEntry)
                .listener(this)
                .build("button.save");

        this.form.add(titleLabel, addressLabel, addressTextField, addressFormatLabel, buttonDelete, buttonSave, buttonClose);
        addToScreen(this.form);
    }

    @Override
    public void update() {
        super.update();
        if (unlockMouse && this.lastUpdate == 25) {
            Mouse.setGrabbed(false); // Force the mouse to be visible even though Mouse.isGrabbed() is false.  //#BugsUnited.
            unlockMouse = false; // Only unlock once per session.
            this.addressTextField.setFocused(true);
        }

        if (++this.lastUpdate > 30) {
            if (function == 3) {
                this.addressTextField.setEditable(false);
            }
            this.lastUpdate = 0;
        }
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN && function != 3) {
            saveEntry();
        }
        super.keyTyped(keyChar, keyCode);
        this.lastUpdate = 0; // Reset the timer when key is typed.
    }

    // Saves the address, as Save or Enter: a full gate address, with or without its dashes
    private void saveEntry() {
        String address = GateAccessData.formatForList(this.addressTextField.getText());
        if (address != null) {
            ConfiguratorNetworkHandler.sendGAAEntryUpdateToServer(localGate, this.oldAddress, address, function);
            if (parent instanceof GateAddressAccessScreen) {
                ((GateAddressAccessScreen) parent).delayedUpdate();
            }
            this.close();
        } else {
            sendErrorMsg(player, "invalidFormat");
        }
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
