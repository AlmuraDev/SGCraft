// Draws the textures of the Universe (Destiny) gate from scratch, so they are original to SGCraft.
// Run from the project root with a JDK:
//
//   javac -d build/tools tools/UniverseTextures.java && java -cp build/tools UniverseTextures
//
// It writes into src/resources/assets/sgcraft/textures:
//   tileentity/universe/stargate.png   ring tiles and the 36 symbol strip (same layout as the other gates)
//   tileentity/universe/iris.png
//   tileentity/universe/{ring,frame,panel,detail,chevron}.png   repeating metals for the gate's model (see
//                                                   UniverseGateRenderer.renderUniverseRing)
// Its symbols, tileentity/universe/glyphs.png, are measured from the Destiny gate model by
// tools/universe/glyphsheet.js.
//   blocks/gatespawner_universe_{stargate,supergate}.png   (the Pegasus ones, recolored)
//   items/universe_upgrade.png                              (the Pegasus one, recolored)

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class UniverseTextures {

    static final String ROOT = "src/resources/assets/sgcraft/textures/";
    // One tile of the gate sheet is 16 units, drawn at this many pixels each
    static final int UNIT = 8;
    static final int TILE = 16 * UNIT;
    static final int SYMBOLS = 36;

    public static void main(String[] args) throws Exception {
        new File(ROOT + "tileentity/universe").mkdirs();
        gateSheet();
        iris();
        // As on the model, all close grays: the ring, gunmetal mottled with black; the frames round
        // its arches and the posts between them, much the same; the panels inside the arches, where
        // the symbols are, a little darker; its chevrons, a little paler; its pipes and lattice,
        // dark bronze
        repeating("ring", 61, 0.32, 0.10, 0.4, new double[] {0.96, 1.0, 1.07});
        repeating("frame", 64, 0.30, 0.07, 0.3, new double[] {0.97, 1.0, 1.05});
        repeating("panel", 65, 0.26, 0.06, 0.3, new double[] {0.97, 1.0, 1.04});
        repeating("detail", 62, 0.30, 0.08, 0.2, new double[] {1.10, 0.93, 0.76});
        repeating("chevron", 63, 0.33, 0.07, 0.25, new double[] {0.98, 1.0, 1.04});
        recolor("blocks/gatespawner_pegasus_stargate.png", "blocks/gatespawner_universe_stargate.png");
        recolor("blocks/gatespawner_pegasus_supergate.png", "blocks/gatespawner_universe_supergate.png");
        recolor("items/pegasus_upgrade.png", "items/universe_upgrade.png");
    }

    // ---- Gate sheet ----------------------------------------------------------------------

    static void gateSheet() throws Exception {
        BufferedImage img = new BufferedImage(4096, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        ringTile(img, 0);
        ringFaceTile(img, 1);
        chevronLitTile(img, 2);
        chevronTile(img, 3);
        symbolStrip(img, g);
        g.dispose();
        ImageIO.write(img, "png", new File(ROOT + "tileentity/universe/stargate.png"));
    }

    // How much of a symbol cell's width, and of the height of the strip above the bevel, a glyph
    // spans
    static final double GLYPH_WIDTH = 0.34, GLYPH_HEIGHT = 0.42;


    // Gunmetal gray with a fine brushed grain, mottled with patches of black, as Destiny's gates are
    static int metal(Random r, double base, double grain, int x, int y, long seed) {
        double n = noise(x * 0.04, y * 0.9, seed) * 0.6 + noise(x * 0.3, y * 0.3, seed + 7) * 0.4;
        double v = base + grain * (n - 0.5) + (r.nextDouble() - 0.5) * 0.025;
        // Blotches of black worked into the metal, soft-edged
        double m = noise(x * 0.025, y * 0.04, seed + 13) * 0.8 + noise(x * 0.1, y * 0.1, seed + 17) * 0.2;
        v *= 1 - 0.4 * smooth(0.3, 0.8, m);
        return rgb(v * 0.96, v * 1.0, v * 1.07);
    }

    static double smooth(double a, double b, double t) {
        double k = Math.max(0, Math.min(1, (t - a) / (b - a)));
        return k * k * (3 - 2 * k);
    }

    // The ring's inner and outer edges: a paler, polished rim
    static void ringTile(BufferedImage img, int index) {
        Random r = new Random(1);
        int ox = index * TILE;
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                img.setRGB(ox + x, y, scale(metal(r, 0.46, 0.10, x, y, 11), 1.0));
            }
        }
    }

    // The ring's face, as on the show: smooth dark metal, worn and speckled, darker than its
    // frames and edges
    static void ringFaceTile(BufferedImage img, int index) {
        Random r = new Random(2);
        int ox = index * TILE;
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double speckle = r.nextDouble() < 0.04 ? 0.8 + 0.4 * r.nextDouble() : 1;
                img.setRGB(ox + x, y, scale(metal(r, 0.3, 0.06, x, y, 21), speckle));
            }
        }
    }

    // The light of a lit or unlit chevron. The brightest band is rows 2 to 10 (lit, white with a
    // warm heart); rows 9 to 16 are the dim, unlit light. Slatted lights use the row 7 to 9 band.
    static void chevronLitTile(BufferedImage img, int index) {
        int ox = index * TILE;
        for (int y = 0; y < TILE; y++) {
            double v = y / (double) UNIT; // 0 to 16
            for (int x = 0; x < TILE; x++) {
                double across = Math.abs(x - TILE / 2.0) / (TILE / 2.0);
                int c;
                if (v < 2) {
                    c = rgb(0.55, 0.55, 0.60);
                } else if (v < 10) {
                    double heat = 1 - Math.abs(v - 7.5) / 5.5;
                    double k = Math.max(0, Math.min(1, heat));
                    double lit = 0.80 + 0.20 * k - 0.12 * across;
                    c = rgb(lit, lit * (0.97 - 0.07 * (1 - k)), lit * (0.92 - 0.2 * (1 - k)));
                } else {
                    double d = 0.16 + 0.05 * Math.sin(v * 1.3) - 0.04 * across;
                    c = rgb(d, d, d * 1.1);
                }
                img.setRGB(ox + x, y, c);
            }
        }
    }

    // The slatted grille along the rim above the symbols, as on the show: bronze slats running
    // round the ring (across the tile), each lit along its top edge, over dark gaps
    static final int GRILLE_SLATS = 12;

    static void chevronTile(BufferedImage img, int index) {
        Random r = new Random(3);
        int ox = index * TILE;
        int pitch = TILE / GRILLE_SLATS;
        for (int y = 0; y < TILE; y++) {
            int k = y % pitch;
            for (int x = 0; x < TILE; x++) {
                int c;
                if (k >= pitch * 0.75) {
                    double d = 0.04 + 0.02 * r.nextDouble();
                    c = rgb(d, d, d * 1.1);
                } else {
                    double v = 0.26 + 0.07 * noise(x * 0.08, y * 0.5, 33) + (r.nextDouble() - 0.5) * 0.03;
                    v *= k < 2 ? 1.45 : k > pitch * 0.6 ? 0.7 : 1;
                    c = rgb(v * 1.06, v * 0.93, v * 0.8);
                }
                img.setRGB(ox + x, y, c);
            }
        }
    }

    // The ring of symbols along the row below the tiles: 36 cells, each holding one engraved
    // glyph, running from the left to the right of the sheet
    static void symbolStrip(BufferedImage img, Graphics2D g) {
        Random r = new Random(4);
        int y0 = TILE;
        int w = img.getWidth();
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < w; x++) {
                // The bottom of the strip is the ring's bevelled inner edge
                double base = y > TILE * 13 / 16 ? 0.15 : 0.23;
                img.setRGB(x, y0 + y, metal(r, base, 0.12, x, y, 41));
            }
        }
        double cell = w / (double) SYMBOLS;
        for (int k = 0; k < SYMBOLS; k++) {
            int x0 = (int) Math.round(k * cell);
            // Divider between cells
            g.setColor(new Color(8, 8, 10));
            g.fillRect(x0 - 2, y0, 4, TILE * 13 / 16);
            g.setColor(new Color(92, 96, 104));
            g.fillRect(x0 + 2, y0, 2, TILE * 13 / 16);
            glyph(g, k, x0 + cell / 2, y0 + TILE * 13 / 32.0, cell * GLYPH_WIDTH, TILE * 13 / 16.0 * GLYPH_HEIGHT, false);
        }
    }

    // Signatures of the glyphs drawn so far, so that no two symbols look alike
    static final java.util.Set<String> seenGlyphs = new java.util.HashSet<>();

    // An engraved glyph: a walk along a grid of points, with a ring at one of them and a dot at
    // another on some. The same for each symbol every run, and different for each.
    static void glyph(Graphics2D g, int k, double cx, double cy, double w, double h, boolean lit) {
        int cols = 3, rows = 4;
        Random r = new Random(1000 + k * 7919L);
        Path2D path;
        int[] walk;
        String signature;
        do {
            int n = 4 + r.nextInt(3);
            walk = new int[n];
            int last = -1;
            for (int i = 0; i < n; i++) {
                int p;
                do {
                    p = r.nextInt(cols * rows);
                } while (p == last);
                walk[i] = last = p;
            }
            int ring = r.nextInt(3) == 0 ? r.nextInt(cols * rows) : -1;
            int dot = r.nextInt(3) == 0 ? r.nextInt(cols * rows) : -1;
            signature = java.util.Arrays.toString(walk) + ring + "/" + dot;
            walk = java.util.Arrays.copyOf(walk, n + 2);
            walk[n] = ring;
            walk[n + 1] = dot;
        } while (!seenGlyphs.add(signature));
        int n = walk.length - 2;
        path = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            double x = cx + (walk[i] % cols / (cols - 1.0) - 0.5) * w;
            double y = cy + (walk[i] / cols / (rows - 1.0) - 0.5) * h;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        double rad = w * 0.13;
        Path2D marks = new Path2D.Double();
        if (walk[n] >= 0) {
            double x = cx + (walk[n] % cols / (cols - 1.0) - 0.5) * w;
            double y = cy + (walk[n] / cols / (rows - 1.0) - 0.5) * h;
            marks.append(new Arc2D.Double(x - rad, y - rad, rad * 2, rad * 2, 0, 360, Arc2D.OPEN), false);
        }
        if (walk[n + 1] >= 0) {
            double x = cx + (walk[n + 1] % cols / (cols - 1.0) - 0.5) * w;
            double y = cy + (walk[n + 1] / cols / (rows - 1.0) - 0.5) * h;
            marks.append(new Arc2D.Double(x - 2, y - 2, 4, 4, 0, 360, Arc2D.OPEN), false);
        }
        if (lit) {
            // Just the cut, in white, a little wider for the glow
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
            g.draw(marks);
            return;
        }
        // Engraved: a pale lip below a dark cut, with a lighter floor in the cut
        BasicStroke stroke = new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke floor = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        for (int pass = 0; pass < 3; pass++) {
            g.setColor(pass == 0 ? new Color(150, 156, 166, 210) : pass == 1 ? new Color(5, 5, 7) : new Color(66, 70, 78));
            int dy = pass == 0 ? 2 : 0;
            g.translate(0, dy);
            g.setStroke(pass == 2 ? floor : stroke);
            g.draw(path);
            g.draw(marks);
            g.translate(0, -dy);
        }
    }


    // ---- Iris --------------------------------------------------------------------------

    static void iris() throws Exception {
        BufferedImage img = new BufferedImage(192, 96, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(5);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, metal(r, 0.28, 0.14, x, y, 51));
            }
        }
        ImageIO.write(img, "png", new File(ROOT + "tileentity/universe/iris.png"));
    }

    // ---- Recoloring an existing image ------------------------------------------------------

    // Swings the blues and greens of an existing image to amber, leaving other colors alone
    static void recolor(String from, String to) throws Exception {
        BufferedImage src = ImageIO.read(new File(ROOT + from));
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                float[] hsb = Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
                float hue = hsb[0] * 360;
                if (hsb[1] > 0.2f && hue > 120 && hue < 280) {
                    // Greens to blues become amber, keeping their relative brightness
                    hsb[0] = (38 + (hue - 120) * 0.05f) / 360f;
                    hsb[1] = Math.min(1f, hsb[1] * 0.85f);
                }
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xFFFFFF;
                out.setRGB(x, y, (argb & 0xFF000000) | rgb);
            }
        }
        ImageIO.write(out, "png", new File(ROOT + to));
    }

    // ---- Helpers -----------------------------------------------------------------------

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static int rgb(double r, double g, double b) {
        return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v * 255)));
    }

    static int scale(int argb, double k) {
        return 0xFF000000 | clamp(((argb >> 16) & 255) / 255.0 * k) << 16
                | clamp(((argb >> 8) & 255) / 255.0 * k) << 8 | clamp((argb & 255) / 255.0 * k);
    }

    // Smooth value noise in 0 to 1
    static double noise(double x, double y, long seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi, seed), b = hash(xi + 1, yi, seed), c = hash(xi, yi + 1, seed), d = hash(xi + 1, yi + 1, seed);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    // A metal that repeats seamlessly every SIZE pixels each way, for the gate model's parts:
    // mottled light and dark, with a fine grain, some of it darkened in soft patches of black
    static final int REPEAT_SIZE = 256;

    static void repeating(String name, long seed, double base, double grain, double black, double[] tint) throws Exception {
        int n = REPEAT_SIZE;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(seed);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                double u = x / (double) n, v = y / (double) n;
                double m = periodicNoise(u, v, 4, seed) * 0.6 + periodicNoise(u, v, 16, seed + 1) * 0.3
                        + periodicNoise(u, v, 64, seed + 2) * 0.1;
                double g = base + grain * (m - 0.5) + (r.nextDouble() - 0.5) * 0.02;
                g *= 1 - black * smooth(0.45, 0.8, periodicNoise(u, v, 3, seed + 3));
                img.setRGB(x, y, rgb(g * tint[0], g * tint[1], g * tint[2]));
            }
        }
        ImageIO.write(img, "png", new File(ROOT + "tileentity/universe/" + name + ".png"));
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
