// Draws the texture of the blue light above a Pegasus gate's chevrons, as on the show: not a flat
// color, but lit from within, a pale spot of light towards its wide outer edge, fading
// out to deep blue at its edges. Laid across the light from in front (see
// SGBaseTERenderer.pegasusLists), u across it and v down it.
// jjs chevronlight.js -- <out.png>
var BufferedImage = Java.type("java.awt.image.BufferedImage"), ImageIO = Java.type("javax.imageio.ImageIO"), File = Java.type("java.io.File");
var N = 128;
// Where the spot is (u, v), and how far it spreads (fraction of the light's width)
var SPOT = [0.5, 0.32], SPREAD = 0.42;
// Its color in the middle, and at the edges
var CORE = [0.85, 0.95, 1.0], EDGE = [0.05, 0.22, 0.62];
var img = new BufferedImage(N, N, BufferedImage.TYPE_INT_RGB);
for (var y = 0; y < N; y++) for (var x = 0; x < N; x++) {
    var dx = (x + 0.5) / N - SPOT[0], dy = ((y + 0.5) / N - SPOT[1]) * 1.3, d = Math.sqrt(dx * dx + dy * dy) / SPREAD;
    // A bright core, and a softer glow round it
    var f = Math.exp(-d * d * 2.2) * 0.75 + Math.exp(-d * d * 0.6) * 0.25;
    var c = [0, 1, 2].map(function (i) { return Math.min(255, Math.round((EDGE[i] + (CORE[i] - EDGE[i]) * f) * 255)); });
    img.setRGB(x, y, c[0] << 16 | c[1] << 8 | c[2]);
}
ImageIO.write(img, "png", new File(arguments[0]));
print("drew " + arguments[0]);
