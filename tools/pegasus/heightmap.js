// Rasterises the Pegasus gate reference model's fixed ring (everything but its chevrons and its
// inner ring's screens) into height maps over (angle, radius): for the front, how far forward the
// surface is (largest z), and for the back, how far back (smallest z). Writes them as big-endian
// floats, row by row (radius, out from R0) of columns (angle, anticlockwise from +x), with NaN
// where there is nothing; then, the same way, how pale the surface seen is (its material's color,
// 0 to 1), front and back.
// Materials named in the optional last argument (comma separated) are taken as polished metal: the
// fine lines scratched across its faces, which break up into grain at any distance in Minecraft.
// jjs heightmap.js -- <scratch> <obj> <out.bin> <angle step> <radius step> [materials]
var file = arguments[1];
var leaveOut = {};
(arguments[5] || "").split(",").forEach(function (m) { if (m) leaveOut[m] = true; });
load(arguments[0] + "/objcore.js");
var keepPart = function (p) { return p == "static"; };
load(arguments[0] + "/pegcore.js");
var outFile = arguments[2], DA = +arguments[3], DR = +arguments[4];
var R0 = 2.20, R1 = 3.12;
var W = Math.round(360 / DA), H = Math.round((R1 - R0) / DR);
var FloatArray = Java.type("float[]");
var front = new FloatArray(W * H), back = new FloatArray(W * H);
var frontPale = new FloatArray(W * H), backPale = new FloatArray(W * H);
// How pale each material is, from the reference's own material file
var pale = {};
(function () {
    var mtl = String(new java.lang.String(Files.readAllBytes(Paths.get(String(file).replace(/\.obj$/i, ".mtl"))), "UTF-8")).split(/\r?\n/), name = null;
    mtl.forEach(function (l) {
        var f = l.trim().split(/\s+/);
        if (f[0] == "newmtl") name = f[1];
        if (f[0] == "Kd" && name) pale[name] = (+f[1] + +f[2] + +f[3]) / 3;
    });
})();
for (var i = 0; i < W * H; i++) { front[i] = -1e9; back[i] = 1e9; }

parts.static.forEach(function (v) {
    var a = v.map(angleOf), r = v.map(radius);
    // Unwrap round the seam at 180 degrees
    if (Math.max.apply(null, a) - Math.min.apply(null, a) > 180) a = a.map(function (x) { return x < 0 ? x + 360 : x; });
    var px = a.map(function (x) { return (x + 360) / DA; }), py = r.map(function (x) { return (x - R0) / DR; });
    var pz = v.map(function (q) { return q[2]; });
    var p = leaveOut[v.name] ? pale.Main_Ring : pale[v.name] === undefined ? 0.5 : pale[v.name];
    var minx = Math.floor(Math.min(px[0], px[1], px[2])), maxx = Math.ceil(Math.max(px[0], px[1], px[2]));
    var miny = Math.max(0, Math.floor(Math.min(py[0], py[1], py[2]))), maxy = Math.min(H - 1, Math.ceil(Math.max(py[0], py[1], py[2])));
    var den = (py[1] - py[2]) * (px[0] - px[2]) + (px[2] - px[1]) * (py[0] - py[2]);
    if (Math.abs(den) < 1e-12) {
        // Edge on in this view (a wall): mark its corners and the middle of its edges
        for (var e = 0; e < 3; e++) for (var s = 0; s <= 4; s++) {
            var f = s / 4, q = (e + 1) % 3;
            var X = Math.round(px[e] + (px[q] - px[e]) * f), Y = Math.round(py[e] + (py[q] - py[e]) * f), Z = pz[e] + (pz[q] - pz[e]) * f;
            if (Y < 0 || Y >= H) continue;
            var k = Y * W + ((X % W) + W) % W;
            if (Z > front[k]) { front[k] = Z; frontPale[k] = p; }
            if (Z < back[k]) { back[k] = Z; backPale[k] = p; }
        }
        return;
    }
    for (var y = miny; y <= maxy; y++) for (var x = minx; x <= maxx; x++) {
        var X = x + 0.5, Y = y + 0.5;
        var w0 = ((py[1] - py[2]) * (X - px[2]) + (px[2] - px[1]) * (Y - py[2])) / den;
        var w1 = ((py[2] - py[0]) * (X - px[2]) + (px[0] - px[2]) * (Y - py[2])) / den;
        var w2 = 1 - w0 - w1;
        if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
        var z = w0 * pz[0] + w1 * pz[1] + w2 * pz[2];
        var k = y * W + ((x % W) + W) % W;
        if (z > front[k]) { front[k] = z; frontPale[k] = p; }
        if (z < back[k]) { back[k] = z; backPale[k] = p; }
    }
});
var bb = ByteBuffer.allocate(16 + 16 * W * H);
bb.putInt(W).putInt(H).putFloat(DA).putFloat(DR);
for (var i = 0; i < W * H; i++) bb.putFloat(front[i] < -1e8 ? NaN : front[i]);
for (var i = 0; i < W * H; i++) bb.putFloat(back[i] > 1e8 ? NaN : back[i]);
for (var i = 0; i < W * H; i++) bb.putFloat(frontPale[i]);
for (var i = 0; i < W * H; i++) bb.putFloat(backPale[i]);
Files.write(Paths.get(outFile), bb.array());
print("height maps " + W + " x " + H + " written");
