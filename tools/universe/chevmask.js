// Measures where the Destiny gate reference model's chevron lights up: its emissive parts, seen
// from its symbol side (the gate's front; the model turned round, x and z negated), laid out over
// the chevron in (degrees clockwise from its center, radius). Writes a grayscale mask, white where
// it glows: left to right from -D_HALF to D_HALF degrees, top to bottom from R1 down to R0.
// jjs chevmask.js -- <scratch> <glb> <texdir> <out png> [width]
var file = arguments[1], out = arguments[0];
load(arguments[0] + "/glbcore.js");
var texdir = arguments[2], outPng = arguments[3], W = +(arguments[4] || 1024), SS = 3;
var D_HALF = 17, R0 = 2.60, R1 = 3.40, H = W / 2;
var prim = json.meshes[0].primitives[0];
var P = accessor(prim.attributes.POSITION), UV = accessor(prim.attributes.TEXCOORD_0), I = accessor(prim.indices);
var emit = ImageIO.read(new File(texdir + "/CAP Chevrons Edited_Emission_0.png"));
var w = W * SS, h = H * SS;
var depth = new Float64Array(w * h), lit = new Uint8Array(w * h);
for (var i = 0; i < w * h; i++) depth[i] = -1e9;
for (var t = 0; t < I.length; t += 3) {
    var px = [], py = [], pz = [], pu = [], pv = [], ok = true, x2 = [], y2 = [];
    for (var e = 0; e < 3; e++) {
        var k = I[t + e], x = -P[3 * k], y = P[3 * k + 1], z = -P[3 * k + 2];
        var a = Math.atan2(y, x) * 180 / Math.PI, d = 90 - a;
        if (Math.abs(d) > D_HALF + 3) { ok = false; break; }
        px.push((d + D_HALF) / (2 * D_HALF) * w);
        py.push((R1 - Math.sqrt(x * x + y * y)) / (R1 - R0) * h);
        pz.push(z); pu.push(UV[2 * k]);
        // The model's texture coordinates run up from the bottom of its textures
        pv.push(1 - UV[2 * k + 1]); x2.push(x); y2.push(y);
    }
    if (!ok) continue;
    // only faces turned to the front
    if ((x2[1] - x2[0]) * (y2[2] - y2[0]) - (y2[1] - y2[0]) * (x2[2] - x2[0]) <= 0) continue;
    var minx = Math.max(0, Math.floor(Math.min(px[0], px[1], px[2]))), maxx = Math.min(w - 1, Math.ceil(Math.max(px[0], px[1], px[2])));
    var miny = Math.max(0, Math.floor(Math.min(py[0], py[1], py[2]))), maxy = Math.min(h - 1, Math.ceil(Math.max(py[0], py[1], py[2])));
    if (minx > maxx || miny > maxy) continue;
    var den = (py[1] - py[2]) * (px[0] - px[2]) + (px[2] - px[1]) * (py[0] - py[2]);
    if (Math.abs(den) < 1e-12) continue;
    for (var yy = miny; yy <= maxy; yy++) for (var xx = minx; xx <= maxx; xx++) {
        var X = xx + 0.5, Y = yy + 0.5;
        var w0 = ((py[1] - py[2]) * (X - px[2]) + (px[2] - px[1]) * (Y - py[2])) / den;
        var w1 = ((py[2] - py[0]) * (X - px[2]) + (px[0] - px[2]) * (Y - py[2])) / den;
        var w2 = 1 - w0 - w1;
        if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
        var z = w0 * pz[0] + w1 * pz[1] + w2 * pz[2];
        if (z <= depth[yy * w + xx]) continue;
        depth[yy * w + xx] = z;
        var u = w0 * pu[0] + w1 * pu[1] + w2 * pu[2], v = w0 * pv[0] + w1 * pv[1] + w2 * pv[2];
        u -= Math.floor(u); v -= Math.floor(v);
        var em = emit.getRGB(Math.min(emit.getWidth() - 1, Math.floor(u * emit.getWidth())), Math.min(emit.getHeight() - 1, Math.floor(v * emit.getHeight()))) & 0xff;
        lit[yy * w + xx] = em;
    }
}
var img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
for (var y = 0; y < H; y++) for (var x = 0; x < W; x++) {
    var sum = 0;
    for (var sy = 0; sy < SS; sy++) for (var sx = 0; sx < SS; sx++) sum += lit[(y * SS + sy) * w + x * SS + sx];
    var g = Math.round(sum / (SS * SS));
    img.setRGB(x, y, (g << 16) | (g << 8) | g);
}
ImageIO.write(img, "png", new File(outPng));
print("written " + outPng);
