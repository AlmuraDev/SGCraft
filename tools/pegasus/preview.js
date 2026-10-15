// Draws SGCraft's Pegasus gate model (pegasus_gate.txt) straight on from the front or back, its
// groups turned into place as the mod does, lit from the upper left, with the fixed ring's faces
// textured with front.png and back.png, to check it against the reference.
// jjs preview.js -- <pegasus_gate.txt> <texture dir> <out.png> <size> [front|back] [cx cy halfWidth [tilt]]
// (tilt: degrees the gate leans back about the view's center, to see its faces at a slant)
var Files = Java.type("java.nio.file.Files"), Paths = Java.type("java.nio.file.Paths");
var BufferedImage = Java.type("java.awt.image.BufferedImage"), ImageIO = Java.type("javax.imageio.ImageIO"), File = Java.type("java.io.File");
var lines = String(new java.lang.String(Files.readAllBytes(Paths.get(arguments[0])), "UTF-8")).split("\n");
var texDir = arguments[1], outFile = arguments[2], S = +arguments[3], back = arguments[4] == "back";
var cx = arguments.length > 5 ? +arguments[5] : 0, cy = arguments.length > 6 ? +arguments[6] : 0, half = arguments.length > 7 ? +arguments[7] : 3.2;
var tilt = (arguments.length > 8 ? +arguments[8] : 0) * Math.PI / 180, ct = Math.cos(tilt), st = Math.sin(tilt);
var groups = {}, cur = null, corners = null;
lines.forEach(function (l) {
    var f = l.trim().split(/\s+/);
    if (f[0] == "g") { cur = groups[f[1]] = []; corners = []; }
    else if (f[0] == "v") corners.push(f.slice(1).map(Number));
    else if (f[0] == "t") cur.push([corners[+f[1]], corners[+f[2]], corners[+f[3]]]);
});
var tex = {};
["front", "back", "outer"].forEach(function (n) { tex[n] = ImageIO.read(new File(texDir + "/" + n + ".png")); });
var color = {rim: 0xa8acb0, wall: 0xa8acb0, separator: 0xa8acb0, screen: 0x223050, chevron: 0xb0b4b8, chevron_dark: 0x505258,
    light_block: 0x4aa0e0, light_bars: 0xc8d0d8};
var zb = new Float64Array(S * S), img = new BufferedImage(S, S, BufferedImage.TYPE_INT_RGB);
for (var i = 0; i < S * S; i++) zb[i] = -1e9;
var L = [-0.45, 0.55, 0.7], ll = Math.sqrt(0.45 * 0.45 + 0.55 * 0.55 + 0.49);
Object.keys(groups).forEach(function (name) {
    var ring = name == "front" || name == "back" || name == "outer" || name == "rim" || name == "wall", slot = name == "screen" || name == "separator";
    var copies = ring ? 9 : slot ? 36 : 9, step = ring || !slot ? 40 : 10;
    for (var k = 0; k < copies; k++) {
        var a = k * step * Math.PI / 180, c = Math.cos(a), s = Math.sin(a);
        groups[name].forEach(function (t) {
            var p = t.map(function (q) { return [q[0] * c - q[1] * s, q[0] * s + q[1] * c, q[2], q[3], q[4]]; });
            p = p.map(function (q) { var y = q[1] - cy; return [q[0], cy + y * ct - q[2] * st, y * st + q[2] * ct, q[3], q[4]]; });
            var ux = p[1][0] - p[0][0], uy = p[1][1] - p[0][1], uz = p[1][2] - p[0][2];
            var vx = p[2][0] - p[0][0], vy = p[2][1] - p[0][1], vz = p[2][2] - p[0][2];
            var nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx, nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nl < 1e-15) return;
            nx /= nl; ny /= nl; nz /= nl;
            // Seen from the back, turned half a turn about y; facing away, not drawn
            if (back) { nx = -nx; nz = -nz; }
            if (nz < 0) return;
            var shade = 0.3 + 0.7 * Math.max(0, (L[0] * nx + L[1] * ny + L[2] * nz) / ll);
            var sx = [], sy = [], sz = [];
            p.forEach(function (q) {
                var x = back ? -q[0] : q[0], z = back ? -q[2] : q[2];
                sx.push((x - cx + half) / (2 * half) * S); sy.push((cy + half - q[1]) / (2 * half) * S); sz.push(z);
            });
            var minx = Math.max(0, Math.floor(Math.min.apply(null, sx))), maxx = Math.min(S - 1, Math.ceil(Math.max.apply(null, sx)));
            var miny = Math.max(0, Math.floor(Math.min.apply(null, sy))), maxy = Math.min(S - 1, Math.ceil(Math.max.apply(null, sy)));
            var den = (sy[1] - sy[2]) * (sx[0] - sx[2]) + (sx[2] - sx[1]) * (sy[0] - sy[2]);
            if (Math.abs(den) < 1e-12) return;
            for (var y = miny; y <= maxy; y++) for (var x = minx; x <= maxx; x++) {
                var X = x + 0.5, Y = y + 0.5;
                var w0 = ((sy[1] - sy[2]) * (X - sx[2]) + (sx[2] - sx[1]) * (Y - sy[2])) / den;
                var w1 = ((sy[2] - sy[0]) * (X - sx[2]) + (sx[0] - sx[2]) * (Y - sy[2])) / den, w2 = 1 - w0 - w1;
                if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
                var z = w0 * sz[0] + w1 * sz[1] + w2 * sz[2];
                if (z <= zb[y * S + x]) continue;
                zb[y * S + x] = z;
                var col = color[name] || 0x808080;
                if (tex[name]) {
                    var u = w0 * p[0][3] + w1 * p[1][3] + w2 * p[2][3], v = w0 * p[0][4] + w1 * p[1][4] + w2 * p[2][4];
                    var T = tex[name], tx = Math.min(T.getWidth() - 1, Math.max(0, Math.floor(u * T.getWidth()))), ty = Math.min(T.getHeight() - 1, Math.max(0, Math.floor(v * T.getHeight())));
                    col = T.getRGB(tx, ty);
                }
                var r = ((col >> 16 & 255) * shade) | 0, g = ((col >> 8 & 255) * shade) | 0, b = ((col & 255) * shade) | 0;
                img.setRGB(x, y, r << 16 | g << 8 | b);
            }
        });
    }
});
ImageIO.write(img, "png", new File(outFile));
