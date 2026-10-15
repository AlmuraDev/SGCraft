// Rasterises the outer edge of the Pegasus gate reference model's fixed ring (everything beyond
// RIM_FROM meters out) into a map over (angle, z): how far out the surface is there (largest
// radius), and how pale it is. Seen from outside, the ring's outer edge (the step down at the edge
// of its face, the lip below it and the rim itself) is a surface over (angle, z), which its faces'
// height maps (heightmap.js) can't show. Writes them as big-endian floats as heightmap.js does,
// rows going from Z0 forwards.
// jjs rimmap.js -- <scratch> <obj> <out.bin> <angle step> <z step>
var file = arguments[1];
load(arguments[0] + "/objcore.js");
var keepPart = function (p) { return p == "static"; };
load(arguments[0] + "/pegcore.js");
var outFile = arguments[2], DA = +arguments[3], DZ = +arguments[4];
var RIM_FROM = 2.98, Z0 = -0.22, Z1 = 0.22;
var W = Math.round(360 / DA), H = Math.round((Z1 - Z0) / DZ);
var FloatArray = Java.type("float[]");
var out = new FloatArray(W * H), pale = new FloatArray(W * H);
for (var i = 0; i < W * H; i++) out[i] = -1e9;
var paleOf = {};
(function () {
    var mtl = String(new java.lang.String(Files.readAllBytes(Paths.get(String(file).replace(/\.obj$/i, ".mtl"))), "UTF-8")).split(/\r?\n/), name = null;
    mtl.forEach(function (l) {
        var f = l.trim().split(/\s+/);
        if (f[0] == "newmtl") name = f[1];
        if (f[0] == "Kd" && name) paleOf[name] = (+f[1] + +f[2] + +f[3]) / 3;
    });
})();

parts.static.forEach(function (v) {
    var r = v.map(radius);
    if (Math.max(r[0], r[1], r[2]) < RIM_FROM) return;
    var a = v.map(angleOf);
    if (Math.max.apply(null, a) - Math.min.apply(null, a) > 180) a = a.map(function (x) { return x < 0 ? x + 360 : x; });
    var px = a.map(function (x) { return (x + 360) / DA; }), py = v.map(function (q) { return (q[2] - Z0) / DZ; });
    var p = paleOf[v.name] === undefined ? 0.5 : paleOf[v.name];
    var minx = Math.floor(Math.min(px[0], px[1], px[2])), maxx = Math.ceil(Math.max(px[0], px[1], px[2]));
    var miny = Math.max(0, Math.floor(Math.min(py[0], py[1], py[2]))), maxy = Math.min(H - 1, Math.ceil(Math.max(py[0], py[1], py[2])));
    var den = (py[1] - py[2]) * (px[0] - px[2]) + (px[2] - px[1]) * (py[0] - py[2]);
    if (Math.abs(den) < 1e-12) return;
    for (var y = miny; y <= maxy; y++) for (var x = minx; x <= maxx; x++) {
        var X = x + 0.5, Y = y + 0.5;
        var w0 = ((py[1] - py[2]) * (X - px[2]) + (px[2] - px[1]) * (Y - py[2])) / den;
        var w1 = ((py[2] - py[0]) * (X - px[2]) + (px[0] - px[2]) * (Y - py[2])) / den;
        var w2 = 1 - w0 - w1;
        if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
        var rr = w0 * r[0] + w1 * r[1] + w2 * r[2];
        if (rr < RIM_FROM) continue;
        var k = y * W + ((x % W) + W) % W;
        if (rr > out[k]) { out[k] = rr; pale[k] = p; }
    }
});
var bb = ByteBuffer.allocate(16 + 8 * W * H);
bb.putInt(W).putInt(H).putFloat(DA).putFloat(DZ);
for (var i = 0; i < W * H; i++) bb.putFloat(out[i] < -1e8 ? NaN : out[i]);
for (var i = 0; i < W * H; i++) bb.putFloat(pale[i]);
Files.write(Paths.get(outFile), bb.array());
// The typical radius at each height, over all angles away from the chevrons
var line = [];
for (var y = 0; y < H; y += 4) {
    var vals = [];
    for (var x = 0; x < W; x++) {
        var d = ((x * DA - 10) % 40 + 40) % 40;
        if (d < 8 || d > 32) continue;
        var rr = out[y * W + x];
        if (rr > -1e8) vals.push(rr);
    }
    vals.sort(function (p, q) { return p - q; });
    line.push((Z0 + (y + 0.5) * DZ).toFixed(3) + ":" + (vals.length > W / 4 ? vals[vals.length >> 1].toFixed(4) : "-"));
}
print("rim map " + W + " x " + H + "; typical radius by z: " + line.join(" "));
