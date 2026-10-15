// Builds the Pegasus gate's glyphs from the reference kit, and draws its screens' textures.
//
// The kit's 36 glyphs (SG_Peg_Glyphs, in the top screen, just behind its surface) are each a
// constellation of flat discs: stars, and the small dots of the dotted lines between them. Flat,
// and as small as they are, they read in Minecraft as faint squiggles; so each disc is made a dome
// standing out of the screen, the stars a little larger (STAR_SCALE), the dots more (DOT_SCALE),
// so that each dotted line becomes a string of beads. Written as groups glyph0 to glyph35, in the
// order of the kit's files, in the form of pegasus_gate.txt (see extract.js): corners in the gate's
// frame, with texture coordinates across each dome for star.png.
//
//   star.png                 a dome's texture: bright in the middle, with a crystal grain
//   crystal.png              the screens' blue crystal (Blue_Crystal_Color.jpg, 2 meters square),
//                            made smaller
//
// jjs glyphs.js -- <pegasus_gate.txt> <glyph dir> <crystal color jpg> <glyph model out> <texture dir>
var Files = Java.type("java.nio.file.Files"), Paths = Java.type("java.nio.file.Paths");
var BufferedImage = Java.type("java.awt.image.BufferedImage"), ImageIO = Java.type("javax.imageio.ImageIO"), File = Java.type("java.io.File");
var JMath = Java.type("java.lang.Math");
var modelFile = arguments[0], glyphDir = arguments[1], crystalFile = arguments[2], outFile = arguments[3], texDir = arguments[4];
// Discs larger than STAR_SIZE (radius, meters) are stars
var STAR_SIZE = 0.002, STAR_SCALE = 1.7, DOT_SCALE = 3.2;
// How far the domes stand out, against their radius; how far they sink into the screen, so that
// they meet it without a gap
var DOME_HEIGHT = 0.6, SINK = 0.0005;
// Their facets: round them, and up them
var STAR_SIDES = 10, STAR_RINGS = 2, DOT_SIDES = 6, DOT_RINGS = 1;
// The textures' sizes
var STAR_TEX = 64, CRYSTAL = 512;

// The screens' front, as PegasusGateModel.measure finds it: a line in (radius, z) fitted through
// the corners of the screen group's forward faces
var screenIn = 1e9, screenOut = 0, slope = 0, z0 = 0;
(function () {
    var lines = String(new java.lang.String(Files.readAllBytes(Paths.get(modelFile)), "UTF-8")).split("\n");
    var group = null, corners = [], n = 0, sr = 0, sz = 0, srr = 0, srz = 0;
    lines.forEach(function (l) {
        var f = l.trim().split(/\s+/);
        if (f[0] == "g") { group = f[1]; corners = []; }
        else if (f[0] == "v") corners.push(f.slice(1).map(Number));
        else if (f[0] == "t" && group == "screen") {
            var p = [corners[+f[1]], corners[+f[2]], corners[+f[3]]];
            var ux = p[1][0] - p[0][0], uy = p[1][1] - p[0][1], uz = p[1][2] - p[0][2];
            var vx = p[2][0] - p[0][0], vy = p[2][1] - p[0][1], vz = p[2][2] - p[0][2];
            var nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx, l = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-15 || nz / l < 0.9) return;
            p.forEach(function (q) {
                var r = JMath.hypot(q[0], q[1]);
                screenIn = Math.min(screenIn, r); screenOut = Math.max(screenOut, r);
                n++; sr += r; sz += q[2]; srr += r * r; srz += r * q[2];
            });
        }
    });
    slope = (n * srz - sr * sz) / (n * srr - sr * sr);
    z0 = (sz - slope * sr) / n;
    // The screens are slabs, with forward faces at their back as well as their front: the line
    // moved forward onto the front, through the furthest forward of them
    var lift = -1e9;
    corners = []; group = null;
    lines.forEach(function (l) {
        var f = l.trim().split(/\s+/);
        if (f[0] == "g") { group = f[1]; corners = []; }
        else if (f[0] == "v") corners.push(f.slice(1).map(Number));
        else if (f[0] == "t" && group == "screen") {
            [corners[+f[1]], corners[+f[2]], corners[+f[3]]].forEach(function (q) {
                lift = Math.max(lift, q[2] - (z0 + slope * JMath.hypot(q[0], q[1])));
            });
        }
    });
    z0 += lift;
})();
function screenZ(r) { return z0 + slope * r; }
print("screens from " + screenIn.toFixed(4) + " to " + screenOut.toFixed(4) + " m, sloping " + slope.toFixed(4));

// A glyph file's discs: [x, y, radius], in the gate's frame (the files have their front towards
// -z, so x turned round)
function discsOf(file) {
    var pos = [], faces = [];
    String(new java.lang.String(Files.readAllBytes(Paths.get(file)), "UTF-8")).split(/\r?\n/).forEach(function (l) {
        if (l.charAt(0) == "v" && l.charAt(1) == " ") { var f = l.trim().split(/\s+/); pos.push([-f[1], +f[2]]); }
        else if (l.charAt(0) == "f" && l.charAt(1) == " ") {
            faces.push(l.trim().split(/\s+/).slice(1).map(function (s) { var k = parseInt(s.split("/")[0]); return k < 0 ? pos.length + k : k - 1; }));
        }
    });
    var parent = pos.map(function (_, i) { return i; });
    function find(i) { while (parent[i] != i) i = parent[i] = parent[parent[i]]; return i; }
    faces.forEach(function (f) { for (var k = 1; k < f.length; k++) parent[find(f[k])] = find(f[0]); });
    var parts = {};
    pos.forEach(function (p, i) { var r = find(i); (parts[r] = parts[r] || []).push(p); });
    return Object.keys(parts).map(function (k) {
        var ps = parts[k], cx = 0, cy = 0, rad = 0;
        ps.forEach(function (p) { cx += p[0]; cy += p[1]; });
        cx /= ps.length; cy /= ps.length;
        ps.forEach(function (p) { rad = Math.max(rad, JMath.hypot(p[0] - cx, p[1] - cy)); });
        return [cx, cy, rad];
    });
}

// A disc as a dome on the screen: its corners, each [x, y, z, u, v], and triangles of them,
// anticlockwise seen from outside
function dome(d) {
    var star = d[2] > STAR_SIZE, R = d[2] * (star ? STAR_SCALE : DOT_SCALE), H = R * DOME_HEIGHT;
    var sides = star ? STAR_SIDES : DOT_SIDES, rings = star ? STAR_RINGS : DOT_RINGS;
    var r = JMath.hypot(d[0], d[1]), c = d[0] / r, s = d[1] / r, zc = screenZ(r) - SINK;
    // Along the screen outwards, across it, and out of it
    var k = Math.sqrt(1 + slope * slope);
    var e1 = [c / k, s / k, slope / k], e2 = [-s, c, 0], n = [-slope * c / k, -slope * s / k, 1 / k];
    var verts = [], tris = [];
    function at(cosPhi, sinPhi, a) {
        var x = R * cosPhi * Math.cos(a), y = R * cosPhi * Math.sin(a), h = (H + SINK) * sinPhi;
        verts.push([d[0] + x * e1[0] + y * e2[0] + h * n[0], d[1] + x * e1[1] + y * e2[1] + h * n[1], zc + x * e1[2] + y * e2[2] + h * n[2],
            0.5 + 0.5 * cosPhi * Math.cos(a), 0.5 + 0.5 * cosPhi * Math.sin(a)]);
        return verts.length - 1;
    }
    var grid = [];
    for (var i = 0; i < rings; i++) {
        var phi = Math.PI / 2 * i / rings, row = [];
        for (var j = 0; j < sides; j++) row.push(at(Math.cos(phi), Math.sin(phi), 2 * Math.PI * j / sides));
        grid.push(row);
    }
    var top = at(0, 1, 0);
    for (var i = 0; i < rings; i++) for (var j = 0; j < sides; j++) {
        var a = grid[i][j], b = grid[i][(j + 1) % sides];
        if (i + 1 < rings) {
            var cc = grid[i + 1][(j + 1) % sides], dd = grid[i + 1][j];
            tris.push([a, b, cc], [a, cc, dd]);
        } else {
            tris.push([a, b, top]);
        }
    }
    return {verts: verts, tris: tris, R: R, star: star};
}

var names = java.util.Arrays.asList(new File(glyphDir).list()).stream().filter(function (n) { return /\.obj$/i.test(n); }).sorted().toArray();
if (names.length != 36) throw "expected 36 glyphs, found " + names.length;
var out = [];
for (var i = 0; i < names.length; i++) {
    var discs = discsOf(glyphDir + "/" + names[i]), verts = [], tris = [], stars = 0;
    discs.forEach(function (d) {
        var m = dome(d), base = verts.length;
        Array.prototype.push.apply(verts, m.verts);
        m.tris.forEach(function (t) { tris.push(t.map(function (k) { return k + base; })); });
        if (m.star) stars++;
    });
    out.push("g glyph" + i);
    verts.forEach(function (v) { out.push("v " + v.map(function (x) { return x.toFixed(5); }).join(" ")); });
    tris.forEach(function (t) { out.push("t " + t.join(" ")); });
    print(names[i] + ": " + stars + " stars, " + (discs.length - stars) + " dots; " + tris.length + " triangles");
}
Files.write(Paths.get(outFile), (out.join("\n") + "\n").getBytes("UTF-8"));

// A dome's texture: brightest in the middle, falling off to its edge, with a fine crystal grain
// that catches the light across it
(function () {
    var img = new BufferedImage(STAR_TEX, STAR_TEX, BufferedImage.TYPE_INT_RGB), rnd = new java.util.Random(36);
    for (var y = 0; y < STAR_TEX; y++) for (var x = 0; x < STAR_TEX; x++) {
        var dx = (x + 0.5) / STAR_TEX * 2 - 1, dy = (y + 0.5) / STAR_TEX * 2 - 1, d = Math.min(1, Math.sqrt(dx * dx + dy * dy));
        var v = (0.55 + 0.45 * Math.pow(1 - d, 1.5)) * (0.9 + 0.2 * rnd.nextDouble());
        var c = [v * 0.85, v * 0.95, v].map(function (q) { return Math.min(255, Math.round(q * 255)); });
        img.setRGB(x, y, c[0] << 16 | c[1] << 8 | c[2]);
    }
    ImageIO.write(img, "png", new File(texDir + "/star.png"));
})();

// The crystal, made smaller by averaging
(function () {
    var src = ImageIO.read(new File(crystalFile));
    var img = new BufferedImage(CRYSTAL, CRYSTAL, BufferedImage.TYPE_INT_RGB), gg = img.createGraphics();
    gg.drawImage(src.getScaledInstance(CRYSTAL, CRYSTAL, Java.type("java.awt.Image").SCALE_AREA_AVERAGING), 0, 0, null);
    gg.dispose();
    ImageIO.write(img, "png", new File(texDir + "/crystal.png"));
})();
print("wrote " + outFile + "; drew star.png and crystal.png");
