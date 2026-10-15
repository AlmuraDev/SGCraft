// Compares the front of my Universe gate model with the reference, unwrapped into (angle, radius)
// jjs compare.js -- <scratch> <glb> <mine.bin> a0 a1 r0 r1 W H [back]
var file = arguments[1], out = arguments[0];
load(arguments[0] + "/glbcore.js");
var a0 = +arguments[3], a1 = +arguments[4], r0 = +arguments[5], r1 = +arguments[6], W = +arguments[7], H = +arguments[8];
var back = arguments[9] == "back";

function raster(tris) {
    var hz = new Float64Array(W * H);
    for (var i = 0; i < W * H; i++) hz[i] = -1e9;
    tris(function (ax, ay, az, bx, by, bz, cx, cy, cz) {
        var v = [[ax, ay, az], [bx, by, bz], [cx, cy, cz]], px = [], py = [], pz = [];
        for (var e = 0; e < 3; e++) {
            var x = v[e][0], y = v[e][1], z = v[e][2];
            if (back) { x = -x; z = -z; }
            var a = Math.atan2(y, x) * 180 / Math.PI;
            if (a < a0 - 3 || a > a1 + 3) return;
            px.push((a - a0) / (a1 - a0) * W);
            py.push((r1 - Math.sqrt(x * x + y * y)) / (r1 - r0) * H);
            pz.push(z);
        }
        var minx = Math.max(0, Math.floor(Math.min(px[0], px[1], px[2]))), maxx = Math.min(W - 1, Math.ceil(Math.max(px[0], px[1], px[2])));
        var miny = Math.max(0, Math.floor(Math.min(py[0], py[1], py[2]))), maxy = Math.min(H - 1, Math.ceil(Math.max(py[0], py[1], py[2])));
        if (minx > maxx || miny > maxy) return;
        var den = (py[1] - py[2]) * (px[0] - px[2]) + (px[2] - px[1]) * (py[0] - py[2]);
        if (Math.abs(den) < 1e-12) return;
        for (var y = miny; y <= maxy; y++) for (var x = minx; x <= maxx; x++) {
            var X = x + 0.5, Y = y + 0.5;
            var w0 = ((py[1] - py[2]) * (X - px[2]) + (px[2] - px[1]) * (Y - py[2])) / den;
            var w1 = ((py[2] - py[0]) * (X - px[2]) + (px[0] - px[2]) * (Y - py[2])) / den;
            var w2 = 1 - w0 - w1;
            if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
            var z = w0 * pz[0] + w1 * pz[1] + w2 * pz[2];
            if (z > hz[y * W + x]) hz[y * W + x] = z;
        }
    });
    return hz;
}

var ref = raster(function (f) {
    prims.forEach(function (p) {
        var P = p.pos, I = p.idx;
        for (var t = 0; t < I.length; t += 3) {
            var a = I[t] * 3, b = I[t + 1] * 3, c = I[t + 2] * 3;
            f(P[a], P[a + 1], P[a + 2], P[b], P[b + 1], P[b + 2], P[c], P[c + 1], P[c + 2]);
        }
    });
});
var mbytes = Files.readAllBytes(Paths.get(arguments[2]));
var mb = ByteBuffer.wrap(mbytes);
var mine = raster(function (f) {
    var n = mbytes.length / 40;
    for (var t = 0; t < n; t++) {
        var o = t * 40 + 4;
        f(mb.getFloat(o), mb.getFloat(o + 4), mb.getFloat(o + 8), mb.getFloat(o + 12), mb.getFloat(o + 16), mb.getFloat(o + 20),
          mb.getFloat(o + 24), mb.getFloat(o + 28), mb.getFloat(o + 32));
    }
});

var img = new BufferedImage(W * 3, H, BufferedImage.TYPE_INT_RGB);
var total = 0, sum = 0, bad = 0, worst = 0, missing = 0, extra = 0;
for (var y = 0; y < H; y++) for (var x = 0; x < W; x++) {
    var i = y * W + x, a = ref[i], b = mine[i];
    function gray(z) { if (z < -1e8) return 0; var g = Math.max(0, Math.min(255, Math.floor((z - 0.05) / 0.25 * 230 + 25))); return (g << 16) | (g << 8) | g; }
    img.setRGB(x, y, gray(a));
    img.setRGB(W + x, y, gray(b));
    var c = 0;
    if (a < -1e8 && b < -1e8) c = 0;
    else if (a < -1e8) { c = 0x0000ff; extra++; }
    else if (b < -1e8) { c = 0xff00ff; missing++; }
    else {
        var d = b - a, ad = Math.abs(d);
        total++; sum += ad; worst = Math.max(worst, ad);
        if (ad > 0.002) bad++;
        var k = Math.min(255, Math.floor(ad / 0.01 * 255));
        c = d > 0 ? (k << 16) | 0x101010 : (k << 8) | 0x101010;
    }
    img.setRGB(2 * W + x, y, c);
}
ImageIO.write(img, "png", new File(out + "/ref/compare.png"));
print("pixels compared " + total + "  mean |dz| " + (sum / total).toFixed(5) + "  worst " + worst.toFixed(4) +
      "  off by >0.002: " + (100 * bad / total).toFixed(2) + "%  only in reference: " + missing + "  only in mine: " + extra);
