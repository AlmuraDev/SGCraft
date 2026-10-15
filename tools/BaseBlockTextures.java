// Draws the fronts of the stargate base blocks for each type of gate but the Milky Way, from the
// Milky Way one (blocks/stargatebase_front.png), recolored to show which gate the base builds.
// Run from the project root with a JDK:
//
//   javac -d build/tools tools/BaseBlockTextures.java && java -cp build/tools BaseBlockTextures
//
// It writes blocks/stargatebase_front_<type>.png into src/resources/assets/sgcraft/textures.

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class BaseBlockTextures {

    static final String DIR = "src/resources/assets/sgcraft/textures/blocks/";

    public static void main(String[] args) throws Exception {
        BufferedImage milkyWay = ImageIO.read(new File(DIR + "stargatebase_front.png"));
        // As on the show: a Pegasus gate's chevrons glow blue, and its ring is a cooler, bluer
        // silver
        write(recolor(milkyWay, 210, 1.0f, 1.1f, 215, 0.18f, 1.05f), "pegasus");
        // A Universe gate's chevrons are white lights in its dark, almost black metal
        write(recolor(milkyWay, 45, 0.12f, 1.35f, 30, 0.04f, 0.62f), "universe");
    }

    // The picture with its orange chevron lights turned to the given hue, saturation (times) and
    // brightness (times), and its gray metal tinted the given hue and saturation, brightness times
    // the given amount. The blue of the event horizon in the middle is left as it is.
    static BufferedImage recolor(BufferedImage src, float lightHue, float lightSat, float lightBright,
            float metalHue, float metalSat, float metalBright) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                float[] hsb = Color.RGBtoHSB(argb >> 16 & 255, argb >> 8 & 255, argb & 255, null);
                float hue = hsb[0] * 360;
                if (hsb[1] > 0.25f && (hue < 70 || hue > 340)) {
                    // The chevrons' lights
                    hsb[0] = lightHue / 360;
                    hsb[1] = Math.min(1, hsb[1] * lightSat);
                    hsb[2] = Math.min(1, hsb[2] * lightBright);
                } else if (hsb[1] < 0.2f) {
                    // The gray metal of the ring
                    hsb[0] = metalHue / 360;
                    hsb[1] = metalSat;
                    hsb[2] = Math.min(1, hsb[2] * metalBright);
                }
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xFFFFFF;
                out.setRGB(x, y, (argb & 0xFF000000) | rgb);
            }
        }
        return out;
    }

    static void write(BufferedImage img, String type) throws Exception {
        ImageIO.write(img, "png", new File(DIR + "stargatebase_front_" + type + ".png"));
    }
}
