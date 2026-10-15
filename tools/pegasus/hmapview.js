// Draws a ninth of the Pegasus height maps (from heightmap.js), front and back, shaded as lit from
// the upper left and unwrapped (angle across, radius up), and prints the typical profile: the
// median height at each radius over the angles away from the chevrons.
// jjs hmapview.js -- <hmap.bin> <out.png>
var Files = Java.type("java.nio.file.Files"), Paths = Java.type("java.nio.file.Paths");
var ByteBuffer = Java.type("java.nio.ByteBuffer"), BufferedImage = Java.type("java.awt.image.BufferedImage");
var ImageIO = Java.type("javax.imageio.ImageIO"), File = Java.type("java.io.File");
var bb = ByteBuffer.wrap(Files.readAllBytes(Paths.get(arguments[0])));
var W = bb.getInt(), H = bb.getInt(), DA = bb.getFloat(), DR = bb.getFloat(), R0 = 2.20;
var FloatArray = Java.type("float[]");
var front = new FloatArray(W * H), back = new FloatArray(W * H);
for (var i = 0; i < W * H; i++) front[i] = bb.getFloat();
for (var i = 0; i < W * H; i++) back[i] = bb.getFloat();

// The ninth from 70 to 110 degrees, the top chevron in the middle
var a0 = Math.round(70 / DA), cols = Math.round(40 / DA), S = 3;
var img = new BufferedImage(cols, H * 2 + 10, BufferedImage.TYPE_INT_RGB);
[front, back].forEach(function (m, side) {
    var sign = side == 0 ? 1 : -1;
    for (var y = 0; y < H; y++) for (var x = 0; x < cols; x++) {
        var k = y * W + a0 + x, z = m[k];
        if (z != z) continue;
        // Height across and up, from the neighbors
        var r = R0 + y * DR, dxw = r * DA * Math.PI / 180;
        var zl = m[y * W + a0 + Math.max(0, x - 1)], zr = m[y * W + a0 + Math.min(cols - 1, x + 1)];
        var zd = m[Math.max(0, y - 1) * W + a0 + x], zu = m[Math.min(H - 1, y + 1) * W + a0 + x];
        if (zl != zl) zl = z; if (zr != zr) zr = z; if (zd != zd) zd = z; if (zu != zu) zu = z;
        // Seen from in front, angle increases to the left
        var gx = -sign * (zl - zr) / (2 * dxw), gy = sign * (zu - zd) / (2 * DR);
        var nx = -gx, ny = -gy, nz = 1, l = Math.sqrt(nx * nx + ny * ny + 1);
        var shade = 0.3 + 0.7 * Math.max(0, (-0.5 * nx + 0.6 * ny + 0.62 * nz) / l);
        var c = Math.min(255, (shade * 230) | 0);
        img.setRGB(side == 0 ? cols - 1 - x : x, side * (H + 10) + (H - 1 - y), c << 16 | c << 8 | c);
    }
});
ImageIO.write(img, "png", new File(arguments[1]));

// The typical profile, away from the chevrons (more than 8 degrees from one)
function median(m, y) {
    var vals = [];
    for (var x = 0; x < W; x++) {
        var a = x * DA, d = ((a - 10) % 40 + 40) % 40;
        if (d < 8 || d > 32) continue;
        var z = m[y * W + x];
        if (z == z) vals.push(z);
    }
    if (vals.length < (W * 0.3)) return NaN;
    vals.sort(function (p, q) { return p - q; });
    return vals[vals.length >> 1];
}
var out = [];
for (var y = 0; y < H; y += 5) out.push((R0 + (y + 0.5) * DR).toFixed(3) + ":" + median(front, y).toFixed(4) + "/" + median(back, y).toFixed(4));
print(out.join("  "));
