// Measures the symbols carved into the arches on the back of the Destiny gate reference model
// into a glyph sheet: a 6 by 6 grid of cells, the symbol of arch k (clockwise from the top
// chevron, four after each chevron) in cell k, white where it is cut into the ring's face, on
// black. Each cell covers the arch's symbol panel: from GLYPH_HALF degrees anticlockwise of the
// arch's center (left) to as far clockwise (right), and from R1 (top) down to R0 (bottom). The back
// is turned round to face front, so each symbol reads as it does to someone looking at it.
// jjs glyphsheet.js -- <scratch> <glb> <out png> [cell pixels]
var file = arguments[1], out = arguments[0];
load(arguments[0] + "/glbcore.js");
var outPng = arguments[2], C = +(arguments[3] || 128), SS = 3;
var GLYPH_HALF = 2.0, R0 = 2.7424, R1 = 3.1404;
function face(r) {
    var p = [[2.6699, 0.1403], [2.7414, 0.1482], [3.1400, 0.1864], [3.2618, 0.1969]];
    if (r <= p[0][0]) return p[0][1];
    for (var i = 0; i + 1 < p.length; i++) if (r <= p[i + 1][0]) return p[i][1] + (p[i + 1][1] - p[i][1]) * (r - p[i][0]) / (p[i + 1][0] - p[i][0]);
    return p[3][1];
}
var prim = prims[1], P = prim.pos, I = prim.idx;
// The back's triangles, turned round to face front, with their angles
var tris = [];
for (var t = 0; t < I.length; t += 3) {
    var v = [0, 1, 2].map(function (e) { var k = I[t + e] * 3; return [-P[k], P[k + 1], -P[k + 2]]; });
    if (v.some(function (q) { return q[2] <= 0; })) continue;
    var a = v.map(function (q) { return Math.atan2(q[1], q[0]) * 180 / Math.PI; });
    var r = v.map(function (q) { return Math.sqrt(q[0] * q[0] + q[1] * q[1]); });
    if (Math.max.apply(null, r) < R0 - 0.01 || Math.min.apply(null, r) > R1 + 0.01) continue;
    tris.push({a: a, r: r, z: v.map(function (q) { return q[2]; })});
}
var W = C * SS, H = C * SS;
var img = new BufferedImage(6 * C, 6 * C, BufferedImage.TYPE_INT_RGB);
for (var k = 0; k < 36; k++) {
    var sec = Math.floor(k / 4), j = k % 4, center = 90 - (sec * 40 + 8 * (j + 1));
    var hz = new Float64Array(W * H);
    for (var i = 0; i < W * H; i++) hz[i] = -1e9;
    tris.forEach(function (tr) {
        var px = [], py = [];
        var d0 = ((center - tr.a[0]) % 360 + 540) % 360 - 180;
        if (Math.abs(d0) > 90) return;
        for (var e = 0; e < 3; e++) {
            // round from the first corner the short way, so a triangle never wraps
            var d = d0 + ((tr.a[0] - tr.a[e]) % 360 + 540) % 360 - 180;
            px.push((d + GLYPH_HALF) / (2 * GLYPH_HALF) * W);
            py.push((R1 - tr.r[e]) / (R1 - R0) * H);
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
            var z = w0 * tr.z[0] + w1 * tr.z[1] + w2 * tr.z[2];
            if (z > hz[y * W + x]) hz[y * W + x] = z;
        }
    });
    var ox = (k % 6) * C, oy = Math.floor(k / 6) * C;
    for (var y = 0; y < C; y++) for (var x = 0; x < C; x++) {
        var cut = 0;
        for (var sy = 0; sy < SS; sy++) for (var sx = 0; sx < SS; sx++) {
            var X = x * SS + sx, Y = y * SS + sy, r = R1 - (Y + 0.5) / H * (R1 - R0);
            if (hz[Y * W + X] > -1e8 && hz[Y * W + X] < face(r) - 0.003) cut++;
        }
        var g = Math.round(255 * cut / (SS * SS));
        img.setRGB(ox + x, oy + y, (g << 16) | (g << 8) | g);
    }
}
ImageIO.write(img, "png", new File(outPng));
print("written " + outPng);
