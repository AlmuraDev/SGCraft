// Draws SGCraft's Milky Way gate model (as dumped by DumpModel) beside the reference, seen
// straight on from the front or back, each lit from the upper left, with a third picture showing
// where their surfaces differ in depth: red where mine stands further out, blue further in, black
// where only one of them has something. Prints how much differs.
// jjs compare.js -- <scratch> <fbx> <mine.bin> <out.png> <size> [front|back] [cx cy halfWidth]
var file = arguments[1];
load(arguments[0] + "/fbxcore.js");
load(arguments[0] + "/mwcore.js");
var mineFile = arguments[2], outFile = arguments[3], S = +arguments[4], back = arguments[5] == "back";
var cx = arguments.length > 6 ? +arguments[6] : 0, cy = arguments.length > 7 ? +arguments[7] : 0;
var half = arguments.length > 8 ? +arguments[8] : 6.2;
var L = [-0.45, 0.55, 0.7], ll = Math.sqrt(L[0] * L[0] + L[1] * L[1] + L[2] * L[2]);
L = L.map(function (q) { return q / ll; });
var colors = [[200, 200, 200], [190, 200, 190], [200, 205, 215], [215, 220, 230], [170, 170, 170], [255, 140, 40]];

// Rasters triangles given by each(f), f(part, a, b, c), into depth and color, culling those
// facing away as the game does (corners anticlockwise seen from in front)
function raster(each, cull) {
    var zb = new DoubleArray(S * S), col = new IntArray(S * S);
    for (var i = 0; i < S * S; i++) zb[i] = -1e18;
    each(function (part, a, b, c) {
        var v = [a, b, c], sx = [], sy = [], sz = [];
        for (var e = 0; e < 3; e++) {
            var x = back ? -v[e][0] : v[e][0], d = back ? -v[e][2] : v[e][2];
            sx.push((x - cx + half) / (2 * half) * S);
            sy.push((cy + half - v[e][1]) / (2 * half) * S);
            sz.push(d);
        }
        var n = triNormal([[back ? -a[0] : a[0], a[1], back ? -a[2] : a[2]], [back ? -b[0] : b[0], b[1], back ? -b[2] : b[2]], [back ? -c[0] : c[0], c[1], back ? -c[2] : c[2]]]);
        if (!n) return;
        // Seen from the back it is turned half a turn about y, so what faces the viewer faces +z
        if (cull && n[2] < -1e-9) return;
        var nz = Math.abs(n[2]), shade = 0.25 + 0.75 * Math.max(0, (n[2] < 0 ? -1 : 1) * (n[0] * L[0] + n[1] * L[1] + n[2] * L[2]));
        var minx = Math.max(0, Math.floor(Math.min(sx[0], sx[1], sx[2]))), maxx = Math.min(S - 1, Math.ceil(Math.max(sx[0], sx[1], sx[2])));
        var miny = Math.max(0, Math.floor(Math.min(sy[0], sy[1], sy[2]))), maxy = Math.min(S - 1, Math.ceil(Math.max(sy[0], sy[1], sy[2])));
        if (minx > maxx || miny > maxy) return;
        var den = (sy[1] - sy[2]) * (sx[0] - sx[2]) + (sx[2] - sx[1]) * (sy[0] - sy[2]);
        if (Math.abs(den) < 1e-12) return;
        var cc = colors[part];
        var rgb = ((cc[0] * shade) | 0) << 16 | ((cc[1] * shade) | 0) << 8 | ((cc[2] * shade) | 0);
        for (var y = miny; y <= maxy; y++) for (var x = minx; x <= maxx; x++) {
            var X = x + 0.5, Y = y + 0.5;
            var w0 = ((sy[1] - sy[2]) * (X - sx[2]) + (sx[2] - sx[1]) * (Y - sy[2])) / den;
            var w1 = ((sy[2] - sy[0]) * (X - sx[2]) + (sx[0] - sx[2]) * (Y - sy[2])) / den;
            var w2 = 1 - w0 - w1;
            if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
            var z = w0 * sz[0] + w1 * sz[1] + w2 * sz[2];
            if (z <= zb[y * S + x]) continue;
            zb[y * S + x] = z;
            col[y * S + x] = rgb;
        }
    });
    return {z: zb, c: col};
}

var partOf = {static: 0, ring: 3, chevrons: 4, lights: 5};
var ref = raster(function (f) {
    Object.keys(parts).forEach(function (k) {
        parts[k].forEach(function (v) { f(partOf[k], v[0], v[1], v[2]); });
    });
}, false);
var mb = ByteBuffer.wrap(Files.readAllBytes(Paths.get(mineFile)));
var mine = raster(function (f) {
    for (var o = 0; o + 40 <= mb.capacity(); o += 40) {
        f(mb.getInt(o), [mb.getFloat(o + 4), mb.getFloat(o + 8), mb.getFloat(o + 12)],
            [mb.getFloat(o + 16), mb.getFloat(o + 20), mb.getFloat(o + 24)], [mb.getFloat(o + 28), mb.getFloat(o + 32), mb.getFloat(o + 36)]);
    }
}, true);

var img = new BufferedImage(S * 3, S, BufferedImage.TYPE_INT_RGB);
var both = 0, only = 0, off = 0, sum = 0;
for (var i = 0; i < S * S; i++) {
    var x = i % S, y = (i / S) | 0, hr = ref.z[i] > -1e17, hm = mine.z[i] > -1e17;
    img.setRGB(x, y, ref.c[i]);
    img.setRGB(S + x, y, mine.c[i]);
    var d = 0;
    if (hr && hm) {
        both++;
        var dz = mine.z[i] - ref.z[i];
        sum += Math.abs(dz);
        if (Math.abs(dz) > 0.003) off++;
        var k = Math.min(255, Math.abs(dz) / 0.02 * 255) | 0;
        d = dz > 0 ? (k << 16) | 0x202020 : k | 0x202020;
    } else if (hr || hm) {
        only++;
        d = hr ? 0x00ff00 : 0xff00ff;
    }
    img.setRGB(2 * S + x, y, d);
}
ImageIO.write(img, "png", new File(outFile));
print("pixels on both " + both + ", mean |dz| " + (sum / both).toFixed(5) + " m, differing by over 3 mm " + (100 * off / both).toFixed(2) +
    "%, on one only " + only + " (green: reference only, magenta: mine only)");
