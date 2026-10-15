// Finds which of the mod's symbols (gui/symbols48.png) each glyph round the Milky Way gate
// reference model's symbol ring is, by comparing their shapes, and draws them side by side to
// check. Slot k is k glyphs clockwise from the top, seen from in front, with the ring at rest; slot
// 0 is the point of origin.
// jjs glyphmatch.js -- <scratch> <fbx> <symbols48.png> <out.png>
var file = arguments[1];
load(arguments[0] + "/fbxcore.js");
load(arguments[0] + "/mwcore.js");
var symbolsFile = arguments[2], outFile = arguments[3];
var SLOTS = 39, SLOT = 360 / SLOTS, N = 48;
var RING = [[4.8436, 0.0663], [5.4408, 0.2243]];
var slope = (RING[1][1] - RING[0][1]) / (RING[1][0] - RING[0][0]);
function coneZ(r) { return RING[0][1] + (r - RING[0][0]) * slope; }

// Fits a list of filled points into an N x N mask, scaled evenly to fill it, centered
function fit(points) {
    var x0 = 1e9, x1 = -1e9, y0 = 1e9, y1 = -1e9;
    points.forEach(function (p) { x0 = Math.min(x0, p[0]); x1 = Math.max(x1, p[0]); y0 = Math.min(y0, p[1]); y1 = Math.max(y1, p[1]); });
    var s = (N - 6) / Math.max(x1 - x0, y1 - y0, 1e-9), ox = (N - (x1 - x0) * s) / 2, oy = (N - (y1 - y0) * s) / 2;
    var m = new IntArray(N * N);
    points.forEach(function (p) {
        var x = Math.floor((p[0] - x0) * s + ox), y = Math.floor((p[1] - y0) * s + oy);
        if (x >= 0 && y >= 0 && x < N && y < N) m[y * N + x] = 1;
    });
    return m;
}

// The reference's glyphs: where the ring stands out of its cone, looked at from in front
var glyphs = [];
for (var k = 0; k < SLOTS; k++) {
    var center = 90 - k * SLOT, pts = [];
    parts.ring.forEach(function (v) {
        var c = [(v[0][0] + v[1][0] + v[2][0]) / 3, (v[0][1] + v[1][1] + v[2][1]) / 3, (v[0][2] + v[1][2] + v[2][2]) / 3];
        var da = ((angleOf(c) - center) % 360 + 540) % 360 - 180;
        // Short of the dividers between slots
        if (Math.abs(da) > SLOT * 0.4 || c[2] - coneZ(radius(c)) < 0.006) return;
        // Fill the triangle, in (across to the right, outwards)
        var q = v.map(function (p) {
            var a = (center - angleOf(p)) * Math.PI / 180, r = radius(p);
            return [r * Math.sin(a), -r * Math.cos(a)];
        });
        for (var i = 0; i <= 12; i++) for (var j = 0; i + j <= 12; j++) {
            var u = i / 12, w = j / 12;
            pts.push([q[0][0] + (q[1][0] - q[0][0]) * u + (q[2][0] - q[0][0]) * w, q[0][1] + (q[1][1] - q[0][1]) * u + (q[2][1] - q[0][1]) * w]);
        }
    });
    glyphs.push(pts.length ? fit(pts) : null);
}

var sheet = ImageIO.read(new File(symbolsFile));
var symbols = [];
for (var s = 0; s < 37; s++) {
    var pts = [];
    // Each pixel as a little grid of points, so that scaled up it stays solid
    for (var y = 0; y < 48; y++) for (var x = 0; x < 48; x++) {
        var px = (s % 10) * 48 + x, py = Math.floor(s / 10) * 48 + y;
        if (((sheet.getRGB(px, py) >>> 24) & 255) > 128) {
            for (var i = 0; i < 4; i++) for (var j = 0; j < 4; j++) pts.push([x + i / 4, y + j / 4]);
        }
    }
    symbols.push(fit(pts));
}

// Overlap of two masks, each grown a little to forgive small differences
function grow(m) {
    var g = new IntArray(N * N);
    for (var y = 0; y < N; y++) for (var x = 0; x < N; x++) {
        if (!m[y * N + x]) continue;
        for (var dy = -1; dy <= 1; dy++) for (var dx = -1; dx <= 1; dx++) {
            var X = x + dx, Y = y + dy;
            if (X >= 0 && Y >= 0 && X < N && Y < N) g[Y * N + X] = 1;
        }
    }
    return g;
}
function score(a, b) {
    var ga = grow(a), gb = grow(b), both = 0, either = 0;
    for (var i = 0; i < N * N; i++) { if (ga[i] && gb[i]) both++; if (ga[i] || gb[i]) either++; }
    return both / either;
}
var scores = glyphs.map(function (g) { return symbols.map(function (s) { return g ? score(g, s) : 0; }); });
// Best first, each symbol once
var pairs = [];
scores.forEach(function (row, k) { row.forEach(function (v, s) { pairs.push([v, k, s]); }); });
pairs.sort(function (a, b) { return b[0] - a[0]; });
var slotOf = {}, symbolOf = {};
pairs.forEach(function (p) {
    if (slotOf[p[2]] !== undefined || symbolOf[p[1]] !== undefined) return;
    slotOf[p[2]] = p[1]; symbolOf[p[1]] = p[2];
});
for (var k = 0; k < SLOTS; k++) {
    var row = scores[k].slice().sort(function (a, b) { return b - a; });
    print("slot " + k + ": symbol " + (symbolOf[k] === undefined ? "-" : symbolOf[k]) + " score " +
        (symbolOf[k] === undefined ? "-" : scores[k][symbolOf[k]].toFixed(3)) + " (best " + row[0].toFixed(3) + ", next " + row[1].toFixed(3) + ")");
}
var table = [];
for (var s = 0; s < 37; s++) table.push(slotOf[s]);
print("slot of each symbol: " + table.join(", "));

// Thirteen slots a row, each glyph over the symbol it was taken for, labelled slot:symbol
var PER = 13, C = 2 * N + 8, img = new BufferedImage(PER * C, 3 * (4 * N + 30), BufferedImage.TYPE_INT_RGB);
var gr = img.createGraphics();
gr.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
for (var k = 0; k < SLOTS; k++) {
    var g = glyphs[k], s = symbolOf[k] === undefined ? null : symbols[symbolOf[k]];
    var X = (k % PER) * C, Y = Math.floor(k / PER) * (4 * N + 30);
    for (var y = 0; y < 2 * N; y++) for (var x = 0; x < 2 * N; x++) {
        var i = (y >> 1) * N + (x >> 1);
        if (g && g[i]) img.setRGB(X + x, Y + 16 + y, 0xffffff);
        if (s && s[i]) img.setRGB(X + x, Y + 16 + 2 * N + 4 + y, 0x80b0ff);
    }
    gr.setColor(java.awt.Color.YELLOW);
    gr.drawString(k + ":" + (symbolOf[k] === undefined ? "-" : symbolOf[k]), X + 2, Y + 13);
}
ImageIO.write(img, "png", new File(outFile));
