package gcewing.sg.features.obelisk.client;

import static org.lwjgl.opengl.GL11.*;

import gcewing.sg.features.obelisk.AsgardObeliskBlock;
import gcewing.sg.features.obelisk.AsgardObeliskTE;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;

// The shine of an Asgard obelisk's red gem as it beams someone: a glow growing brighter and wider over the gem until the
// beam, then dying away. The gem itself is part of the block's model.
public class AsgardObeliskRenderer extends TileEntitySpecialRenderer<AsgardObeliskTE> {

    // The gem's middle across a north-facing obelisk; its height and depth depend on the kind of obelisk
    private static final double gemX = 0.5;
    private static final int segments = 24;

    @Override
    public void render(AsgardObeliskTE te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        float shine = te.shine(partialTicks);
        if (shine <= 0) {
            return;
        }
        IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!(state.getBlock() instanceof AsgardObeliskBlock)) {
            return;
        }
        EnumFacing facing = state.getValue(AsgardObeliskBlock.FACING);
        AsgardObeliskBlock.Type type = ((AsgardObeliskBlock) state.getBlock()).type;
        double gemY = 1 + type.gemY, gemZ = type.gemZ - 0.1 / 16;

        float lastX = OpenGlHelper.lastBrightnessX, lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        glPushMatrix();
        glTranslated(x + 0.5, y, z + 0.5);
        glRotatef(-facing.getHorizontalAngle() + 180, 0, 1, 0); // as the block model turns: north faces -z
        glTranslated(gemX - 0.5, gemY, gemZ - 0.5);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glDepthMask(false);
        glShadeModel(GL_SMOOTH);
        // The discs fade to nothing at their edges: with the alpha test on (as the world draws tile entities) their faint
        // outer parts were cut off, leaving a hard edged disc that jumped wider instead of a glow spreading softly
        boolean alphaTest = glIsEnabled(GL_ALPHA_TEST);
        glDisable(GL_ALPHA_TEST);

        // A wide soft red glow, and a bright core that turns white as it peaks
        disc(0.06 + 0.28 * shine, 1F, 0.15F, 0.1F, 0.55F * shine);
        disc(0.03 + 0.08 * shine, 1F, 0.55F + 0.45F * shine, 0.5F + 0.5F * shine, 0.9F * shine);

        if (alphaTest) {
            glEnable(GL_ALPHA_TEST);
        }
        glShadeModel(GL_FLAT);
        glDepthMask(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
        glPopMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
    }

    // A disc facing out of the obelisk (towards -z), solid in the middle and fading to nothing at the edge
    private static void disc(double radius, float r, float g, float b, float a) {
        glBegin(GL_TRIANGLE_FAN);
        glColor4f(r, g, b, a);
        glVertex3d(0, 0, -0.01);
        glColor4f(r, g, b, 0);
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            glVertex3d(radius * Math.cos(angle), radius * Math.sin(angle), -0.01);
        }
        glEnd();
    }
}
