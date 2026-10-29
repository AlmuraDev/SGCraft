package gcewing.sg.features.pdd.client.gui;

import gcewing.sg.features.pdd.network.PddNetworkHandler;
import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.BasicScreen;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.BasicForm;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.component.interaction.button.builder.UIButtonBuilder;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.FontColors;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;

public class PddEntryScreen extends BasicScreen {
    private int lastUpdate = 0;
    private boolean unlockMouse = true;
    private BasicForm form;
    private UIButton localIrisOpenButton, localGateCloseButton, localIrisCloseButton, remoteIrisOpenButton, remoteGateCloseButton, remoteIrisCloseButton;
    private UICheckBox autoCloseChk;
    private UITextField nameTextField, addressTextField, indexTextField;
    private EntityPlayer player;
    public boolean isLocked, autoClose, delete;
    public String name;
    public String address;
    public int index, unid, type;

    public PddEntryScreen(BasicScreen parent, EntityPlayer player, String name, String address, int index, int unid, boolean isLocked, boolean autoClose, boolean delete, int type) {
        super(parent, true);
        this.player = player;
        this.name = name;
        this.address = address;
        this.index = index;
        this.unid = unid;
        this.isLocked = isLocked;
        this.autoClose = autoClose;
        this.delete = delete;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void construct() {
        this.guiscreenBackground = false;
        Keyboard.enableRepeatEvents(true);

        // Master Panel
        this.form = new BasicForm(250, 100, "");
        this.form.setAnchor(Anchor.CENTER | Anchor.MIDDLE);
        this.form.setMovable(true);
        this.form.setClosable(true);
        this.form.setBorder(FontColors.WHITE, 1, 185);
        this.form.setBackgroundAlpha(255);
        this.form.setBottomPadding(3);
        this.form.setRightPadding(3);
        this.form.setTopPadding(20);
        this.form.setLeftPadding(3);

        final UILabel titleLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.pddEntry"));
        titleLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        titleLabel.setPosition(0, -15, Anchor.CENTER | Anchor.TOP);
        if (type == 4) {
            titleLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.edd.label.nameEntry"));
        } else if (type == 1) {
            titleLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.addPddEntry"));
        } else {
            if (!delete) {
                titleLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.editPddEntry"));
            } else {
                titleLabel.setText(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.deletePddEntry"));
            }
        }

        final UILabel nameLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.name")+":");
        nameLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        nameLabel.setPosition(0, 10, Anchor.LEFT | Anchor.TOP);

        this.nameTextField = new UITextField("");
        this.nameTextField.setText(this.name);
        this.nameTextField.setEditable(!this.isLocked);
        this.nameTextField.setSize(180, 0);
        this.nameTextField.setPosition(60, 10, Anchor.LEFT | Anchor.TOP);
        this.nameTextField.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(false).build());

        final UILabel addressLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.address")+":");
        addressLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        addressLabel.setPosition(0, 25, Anchor.LEFT | Anchor.TOP);

        this.addressTextField = new UITextField("");
        this.addressTextField.setText(this.address);
        this.addressTextField.setEditable(!this.isLocked && !isExplorerEntry());
        this.addressTextField.setSize(180, 0);
        this.addressTextField.setPosition(60, 25, Anchor.LEFT | Anchor.TOP);
        this.addressTextField.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(false).build());

        final UILabel indexLabel = new UILabel(TextFormatting.WHITE + I18n.format("sgcraft.gui.pdd.label.index")+":");
        indexLabel.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(true).scale(1.1F).build());
        indexLabel.setPosition(0, 40, Anchor.LEFT | Anchor.TOP);

        this.indexTextField = new UITextField("");
        this.indexTextField.setText(String.valueOf(this.index));
        this.indexTextField.setSize(180, 0);
        this.indexTextField.setPosition(60, 40, Anchor.LEFT | Anchor.TOP);
        this.indexTextField.setFontOptions(FontOptions.builder().from(FontColors.WHITE_FO).shadow(false).build());

        // ****************************************************************************************************************************

        this.autoCloseChk = new UICheckBox(TextFormatting.WHITE + "Auto Close on Connect");
        this.autoCloseChk.setAnchor(Anchor.BOTTOM | Anchor.LEFT);
        this.autoCloseChk.setPosition(0, -2);
        this.autoCloseChk.setChecked(autoClose);

        // A generated gate's entry only takes a name: its address is the gate's, and the list keeps its own order
        if (isExplorerEntry()) {
            indexLabel.setVisible(false);
            this.indexTextField.setVisible(false);
            this.autoCloseChk.setVisible(false);
        }

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
            .visible(this.delete)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.delete"))
            .onClick(() -> {
                PddNetworkHandler.sendPddEntryUpdateToServer(this.nameTextField.getText().trim(), this.addressTextField.getText().trim(), -1, this.unid, this.isLocked, this.autoCloseChk.isChecked());
                this.close();

            })
            .listener(this)
            .build("button.delete");

        // Save Feature button
        final UIButton buttonSave = new UIButtonBuilder()
            .width(40)
            .position(-(buttonClose.getX() + buttonClose.getWidth() + 5), 0)
            .visible(!this.delete)
            .anchor(Anchor.BOTTOM | Anchor.RIGHT)
            .text(TextFormatting.WHITE + I18n.format("sgcraft.gui.button.save"))
            .onClick(() -> {
                if (isExplorerEntry()) {
                    PddNetworkHandler.sendExplorerNameToServer(this.address, this.nameTextField.getText().trim());
                    this.close();
                    return;
                }
                if (this.addressTextField.getText().length() == 11 && this.addressTextField.getText().substring(4,5).equalsIgnoreCase("-") && this.addressTextField.getText().substring(8,9).equalsIgnoreCase("-")) {
                    PddNetworkHandler.sendPddEntryUpdateToServer(this.nameTextField.getText().trim(), this.addressTextField.getText().trim(), Integer.valueOf(this.indexTextField.getText()), this.unid, this.isLocked, this.autoCloseChk.isChecked());
                    this.close();
                } else {
                    sendErrorMsg(player, "invalidFormat");
                }
            })
            .listener(this)
            .build("button.save");

        this.form.add(titleLabel, nameLabel, nameTextField, addressLabel, addressTextField, indexLabel, autoCloseChk, indexTextField, buttonDelete, buttonSave, buttonClose);
        addToScreen(this.form);
    }

    // Naming one of the server's generated gates, from the Explorer Dialer Device
    private boolean isExplorerEntry() {
        return this.type == 4;
    }

    @Override
    public void update() {
        super.update();
        if (unlockMouse && this.lastUpdate == 25) {
            Mouse.setGrabbed(false); // Force the mouse to be visible even though Mouse.isGrabbed() is false.  //#BugsUnited.
            unlockMouse = false; // Only unlock once per session.
        }

        if (++this.lastUpdate > 30) {
            if (delete) {
                this.nameTextField.setEditable(false);
                this.addressTextField.setEditable(false);
                this.indexTextField.setEditable(false);
            }
            this.lastUpdate = 0;
        }
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) {
        if (keyCode == Keyboard.KEY_TAB && isExplorerEntry()) {
            this.nameTextField.setFocused(true); // the only field that can be edited
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            if (this.nameTextField.isFocused()) {
                this.addressTextField.setFocused(true);
                return;
            }
            if (this.addressTextField.isFocused()) {
                this.indexTextField.setFocused(true);
                return;
            }
            if (this.indexTextField.isFocused()) {
                this.nameTextField.setFocused(true);
                return;
            }
        }
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
