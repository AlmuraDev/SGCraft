//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate controller gui screen
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.client.gui;

import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.util.GateType;
import gcewing.sg.util.SGAddressing;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.SGCraft;
import gcewing.sg.util.SGState;
import gcewing.sg.network.SGChannel;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import static gcewing.sg.BaseBlockUtils.getWorldTileEntity;
import static org.lwjgl.opengl.GL11.*;

public class DHDScreen extends SGScreen {

    private final int dhdWidth = 320;
    private final int dhdHeight = 120;
    private final double dhdRadius1 = dhdWidth * 0.1;
    private final double dhdRadius2 = dhdWidth * 0.275;
    private final double dhdRadius3 = dhdWidth * 0.45;

    private World world;
    private BlockPos pos;

    private DHDTE cte;

    private int dhdTop, dhdCentreX, dhdCentreY;
    private int closingDelay = 0;
    private int addressLength;

    private boolean debugDialing = false;
    
    public DHDScreen(EntityPlayer player, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        this.cte = getControllerTE();
        SGBaseTE te = getStargateTE();
        if (te != null)
            this.addressLength = te.getNumChevrons();
    }
    
    private SGBaseTE getStargateTE() {
        return this.cte != null ? this.cte.getLinkedStargateTE() : null;
    }
    
    private DHDTE getControllerTE() {
        TileEntity te = getWorldTileEntity(this.world, this.pos);
        return te instanceof DHDTE ? (DHDTE) te : null;
    }
    
    private String getEnteredAddress() {
        return this.cte.enteredAddress;
    }
    
    private void setEnteredAddress(String address) {
        this.cte.enteredAddress = address;
    }

    @Override
    public void initGui() {
        this.dhdTop = this.height - this.dhdHeight;
        this.dhdCentreX = this.width / 2;
        this.dhdCentreY = this.dhdTop + this.dhdHeight / 2;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.closingDelay > 0) {
            if (--this.closingDelay == 0) {
                this.cte.enteredAddress = "";
                SGChannel.sendClearAddressToServer(this.cte);
                close();
            }
        }
    }

    @Override
    protected void mousePressed(int x, int y, int mouseButton) {
        //System.out.printf("DHDScreen.mousePressed: %d, %d, %d\n", x, y, mouseButton);
        if (mouseButton == 0) {
            int i = findDHDButton(x, y);
            if (i >= 0) {
                dhdButtonPressed(i);
            }
        }
    }
    
    private void closeAfterDelay(int ticks) {
        this.closingDelay = ticks;
    }
    
    private int findDHDButton(int mx, int my) {
        //System.out.printf("DHDScreen.findDHDButton: mx = %d, my = %d, cx = %d, cy = %d\n",
        //  mx, my, dhdCentreX, dhdCentreY);
        int x = -(mx - this.dhdCentreX);
        int y = -(my - this.dhdCentreY);
        // Check top half of orange dome
        if (y > 0 && Math.hypot(x, y) <= this.dhdRadius1)
            return 0;
        // Scale to circular coords and check rest of buttons
        y = y * this.dhdWidth / this.dhdHeight;
        //System.out.printf("DHDScreen.findDHDButton: x = %d, y = %d\n", x, y);
        double r = Math.hypot(x, y);
        if (r > this.dhdRadius3)
            return -1;
        if (r <= this.dhdRadius1)
            return 0;
        double a = Math.toDegrees(Math.atan2(y, x));
        //System.out.printf("DHDScreen.findDHDButton: a = %s\n", a);
        if (a < 0)
            a += 360;
        //int i0 = (r > dhdRadius2) ? 1 : 15;
        //return i0 + (int)Math.floor(a * 14 / 360);
        int i0, nb;
        if (r > this.dhdRadius2) {
            i0 = 1; nb = 26;
        }
        else {
            i0 = 27; nb = 11;
        }
        int i = i0 + (int)Math.floor(a * nb / 360);
        System.out.printf("DHDScreen.findDHDButton: i = %d\n", i);
        return i;
    }
    
    private void dhdButtonPressed(int i) {
        //System.out.printf("DHDScreen.dhdButtonPressed: %d\n", i);
        if (i == 0) {
            dial();
        } else if (i >= 37) {
            erase();
        } else {
            chevron(SGBaseTE.symbolToChar(i - 1));
        }
    }
    
    private void buttonSound(SoundEvent sound) {
        EntityPlayer player = this.mc.player;
        ISound s = new PositionedSoundRecord(sound, SoundCategory.BLOCKS,1F * SGBaseTE.soundVolume, 1F, (float)player.posX, (float)player.posY, (float)player.posZ);
        this.mc.getSoundHandler().playSound(s);
    }

    @Override
    protected void keyTyped(char c, int key) {
        switch (key) {
            case Keyboard.KEY_ESCAPE:
                close();
                break;
           // Experimental -> Auto Dial address with instant open, Knox's anyone?
            case Keyboard.KEY_F12:
                //SGBaseTE te = getStargateTE();
                //sendConnectOrDisconnect(te, "ABKD7M6");
                break;
            // End Experiment.
            case Keyboard.KEY_BACK:
            case Keyboard.KEY_DELETE:
                erase();
                break;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                dial();
                break;
            default:
                String C = String.valueOf(c).toUpperCase();
                if (SGAddressing.isValidSymbolChar(C))
                    chevron(C.charAt(0));
                break;
        }
    }
    
    private void dial() {
        SGBaseTE te = getStargateTE();
        if (te != null) {
            buttonSound(te.getGateType().select(SGBaseTE.m_dhdDialSound, SGBaseTE.p_dhdDialSound));
            sendConnectOrDisconnect(te, te.state == SGState.Idle ? getEnteredAddress() : "");

        }
    }
    
    private void sendConnectOrDisconnect(SGBaseTE te, String address) {
        if (debugDialing) {
            System.out.println("dial address: " + address);
        }
        SGChannel.sendConnectOrDisconnectToServer(te, address);
        closeAfterDelay(10);
    }
        
    private void erase() {
        if (stargateIsIdle()) {
            buttonSound(SoundEvents.UI_BUTTON_CLICK);
            String enteredAddress = getEnteredAddress();
            if (!enteredAddress.isEmpty()) {
                cte.unsetSymbol(this.mc.player);
                SGChannel.sendUnsetSymbolToServer(cte);
            }
        }
    }
    
    private void chevron(char c) {
        if (stargateIsIdle()) {
            buttonSound(this.cte.getLinkedStargateTE().getGateType().select(SGBaseTE.m_dhdPressSound, SGBaseTE.p_dhdPressSound));
            String a = getEnteredAddress();
            int n = a.length();
            if (n < this.addressLength) {
                this.cte.enterSymbol(null, c);
                SGChannel.sendEnterSymbolToServer(this.cte, c);
            }
        }
    }
    
    private boolean stargateIsIdle() {
        SGBaseTE te = getStargateTE();
        return (te != null && te.state == SGState.Idle);
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        SGBaseTE te = getStargateTE();
        glPushAttrib(GL_ENABLE_BIT|GL_COLOR_BUFFER_BIT);
        glEnable(GL11.GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_ALPHA_TEST);
        drawBackgroundImage();
        drawButton();
        if (te != null) {
            if (te.state == SGState.Idle) {
                drawEnteredSymbols();
                drawEnteredString();
            }
        }
        glPopAttrib();
    }

    private void drawBackgroundImage() {
        bindTexture(SGCraft.mod.resourceLocation("textures/gui/dhd_gui.png"));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        drawTexturedRect((width - dhdWidth) / 2, height - dhdHeight, dhdWidth, dhdHeight);
    }
    
    private void drawButton() {
        SGBaseTE te = getStargateTE();
        boolean connected = te != null && te.isActive();
        GateType type = te.getGateType();
        bindTexture(SGCraft.mod.resourceLocation("textures/gui/" + type.assetName + "_dhd_centre.png"), 128, 64);
        if (te == null || !te.isMerged)
            setColor(0.2, 0.2, 0.2); // grey
        else if (connected)
            if (type == GateType.PEGASUS) {
                setColor(0.0, 0.45, 1.0);
            } else {
                setColor(1.0, 0.5, 0.0);
            }
        else
            if (type == GateType.PEGASUS) {
                setColor(0.0, 0.45, 0.8);
            } else {
                setColor(0.5, 0.25, 0.0);
            }
        double rx = this.dhdWidth * 48 / 512.0;
        double ry = this.dhdHeight * 48 / 256.0;
        drawTexturedRect(this.dhdCentreX - rx, this.dhdCentreY - ry - 6, 2 * rx, 1.5 * ry,
            64, 0, 64, 48);
        resetColor();
        if (connected) {
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            double d = 5;
            drawTexturedRect(this.dhdCentreX - rx - d, this.dhdCentreY - ry - d - 6, 2 * (rx + d), ry + d,
                0, 0, 64, 32);
            drawTexturedRect(this.dhdCentreX - rx - d, this.dhdCentreY - 6, 2 * (rx + d), 0.5 * ry + d,
                0, 32, 64, 32);
           GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
    
    private void drawEnteredSymbols() {
        drawAddressSymbols(this.width / 2, this.dhdTop - 80, getEnteredAddress());
    }
    
    private void drawEnteredString() {
        String address = SGAddressing.padAddress(getEnteredAddress(), "|", this.addressLength);
        drawAddressString(this.width / 2, this.dhdTop - 20, address);
    }

}
