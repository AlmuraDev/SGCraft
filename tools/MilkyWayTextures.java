// Draws the metals of the Milky Way (SG-1) gate's model from scratch, so they are original to
// SGCraft. Run from the project root with a JDK:
//
//   javac -d build/tools tools/MilkyWayTextures.java && java -cp build/tools MilkyWayTextures
//
// It writes tileentity/milkyway/{ring,detail,symbols,glyphs,chevron,light,slats}.png into
// src/resources/assets/sgcraft/textures: repeating metals for the gate's model (see
// MilkyWayGateRenderer.renderRings), and the glass of its chevrons' lights.

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class MilkyWayTextures {

    static final String DIR = "src/resources/assets/sgcraft/textures/tileentity/milkyway/";
    static final int SIZE = 256;

    public static void main(String[] args) throws Exception {
        new File(DIR).mkdirs();
        // As on the show, a weathered dark gray, like dark stone, with soft patches of darker: the
        // fixed ring; the detail on it, much the same; the symbol ring and its glyphs, a little
        // paler and cooler; the chevrons, a little darker
        repeating("ring", 71, 0.22, 0.07, 0.12, new double[] {0.99, 1.0, 0.98});
        repeating("detail", 72, 0.21, 0.06, 0.12, new double[] {0.99, 1.0, 0.98});
        repeating("symbols", 73, 0.24, 0.06, 0.1, new double[] {0.97, 1.0, 1.02});
        repeating("glyphs", 74, 0.25, 0.05, 0.08, new double[] {0.97, 1.0, 1.02});
        repeating("chevron", 75, 0.19, 0.05, 0.1, new double[] {1.0, 1.0, 0.99});
        light();
        slats();
    }

    // The glass of a chevron's lights, lit, laid over the whole chevron (see
    // MilkyWayGateRenderer.lightTriangles), which spans the middle of it: the triangle of the
    // chevron's light, as on the show, glowing pale yellow-orange at its heart, deepening to
    // orange and a red-orange at its edges, a little mottled
    static void light() throws Exception {
        double[][] ramp = {{1.0, 0.88, 0.55}, {1.0, 0.66, 0.25}, {1.0, 0.45, 0.12}, {0.85, 0.28, 0.06}};
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double u = (x + 0.5) / SIZE, v = (y + 0.5) / SIZE;
                double d = Math.hypot((u - 0.5) / 0.21, (v - 0.3) / 0.5);
                d += (periodicNoise(u, v, 12, 77) - 0.5) * 0.12;
                img.setRGB(x, y, along(ramp, d));
            }
        }
        ImageIO.write(img, "png", new File(DIR + "light.png"));
    }

    // The glass of the slatted lights beside the chevron's V, lit, laid over the chevron in the
    // same way: as on the show, a row of glowing red-orange beads along each slat, paler at their
    // hearts, with darker glass between them
    static void slats() throws Exception {
        double[][] ramp = {{1.0, 0.58, 0.28}, {1.0, 0.40, 0.14}, {0.55, 0.13, 0.04}};
        int beads = 22;
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double u = (x + 0.5) / SIZE;
                double b = 0.5 + 0.5 * Math.cos(2 * Math.PI * u * beads);
                img.setRGB(x, y, along(ramp, 1 - Math.pow(b, 0.6)));
            }
        }
        ImageIO.write(img, "png", new File(DIR + "slats.png"));
    }

    // The color the given fraction (0 to 1) of the way along a ramp of colors
    static int along(double[][] ramp, double d) {
        double k = Math.max(0, Math.min(1, d)) * (ramp.length - 1);
        int i = Math.min(ramp.length - 2, (int) k);
        double f = k - i;
        return rgb(ramp[i][0] + (ramp[i + 1][0] - ramp[i][0]) * f,
                ramp[i][1] + (ramp[i + 1][1] - ramp[i][1]) * f, ramp[i][2] + (ramp[i + 1][2] - ramp[i][2]) * f);
    }

    // A metal that repeats seamlessly every SIZE pixels each way: mottled light and dark, with a
    // fine grain and pitting, some of it darkened in soft patches
    static void repeating(String name, long seed, double base, double grain, double black, double[] tint) throws Exception {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(seed);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double u = x / (double) SIZE, v = y / (double) SIZE;
                double m = periodicNoise(u, v, 4, seed) * 0.5
                        + periodicNoise(u, v, 16, seed + 1) * 0.35
                        + periodicNoise(u, v, 64, seed + 2) * 0.15;
                double g = base + grain * (m - 0.5) + (r.nextDouble() - 0.5) * 0.03;
                // Small dark pits
                if (r.nextDouble() < 0.015) {
                    g *= 0.8;
                }
                g *= 1 - black * smooth(0.5, 0.85, periodicNoise(u, v, 3, seed + 3));
                img.setRGB(x, y, rgb(g * tint[0], g * tint[1], g * tint[2]));
            }
        }
        ImageIO.write(img, "png", new File(DIR + name + ".png"));
    }

    static double smooth(double a, double b, double t) {
        double k = Math.max(0, Math.min(1, (t - a) / (b - a)));
        return k * k * (3 - 2 * k);
    }

    static int rgb(double r, double g, double b) {
        return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v * 255)));
    }

    // Value noise over the unit square that wraps round at its edges, with the given number of
    // cells each way
    static double periodicNoise(double u, double v, int cells, long seed) {
        double x = u * cells, y = v * cells;
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        int x0 = Math.floorMod(xi, cells), x1 = Math.floorMod(xi + 1, cells);
        int y0 = Math.floorMod(yi, cells), y1 = Math.floorMod(yi + 1, cells);
        double a = hash(x0, y0, seed), b = hash(x1, y0, seed), c = hash(x0, y1, seed), d = hash(x1, y1, seed);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    static double hash(int x, int y, long seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }
}
