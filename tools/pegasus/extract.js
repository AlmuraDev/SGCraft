// Measures the Pegasus gate reference model into the data the mod builds its own model from (see
// PegasusGateModel), and draws the textures of its fixed ring. The data is groups of triangles:
//
//   g <name>                 the start of a group; the triangles after it are in it
//   v x y z u v              a corner, in the gate's frame (meters), with its texture coordinates
//   t a b c                  a triangle of the group's corners (counting from 0 in the group),
//                            anticlockwise seen from outside
//
// The groups are:
//   front, back              the fixed ring's faces, a ninth of them, round the top chevron (70 to
//                            110 degrees), from its height maps (heightmap.js), simplified
//                            (hmapmesh.js); textured with front.png and back.png, drawn here
//   outer                    its outer edge (the step down at the edge of its face, the lip below
//                            it and the rim), turned from its profile; textured with outer.png,
//                            drawn here from rimmap.js's map of it
//   rim, wall                its bore, and the steep parts of its faces, the ninth of them
//   screen, separator        one of the inner ring's 36 crystal screens, round the top (85 to 95
//                            degrees), and the divider beside it
//   chevron, chevron_dark    the top chevron's metal, polished and engraved, simplified; with the
//                            rim and the dividers, textured with metal.png, drawn here
//   light_block, light_bars  its lights: the blue block above it, and the bars and lower part
//
// jjs extract.js -- <scratch> <obj> <hmap.bin> <out file> <texture dir> <rim.bin>
var file = arguments[1], hmapFile = arguments[2], outFile = arguments[3], texDir = arguments[4], rimFile = arguments[5];
// The faces stop here, short of the outer edge, which is turned from its own profile (OUTER)
var FACE_TO = 2.997;
var keep = function (name) { return /^Chevron|^Inner_Ring/.test(name); };
load(arguments[0] + "/objcore.js");
load(arguments[0] + "/pegcore.js");
load(arguments[0] + "/decimate.js");
load(arguments[0] + "/hmapmesh.js");

// The fixed ring's faces: their broad shapes (see hmapmesh.js) simplified to within TOL, in cells
// no smaller than MIN_CELL of their height maps', which keeps the walls of the recesses round the
// chevrons straight
var TOL = 0.007, MIN_CELL = 2;
// The chevron and its lights, simplified to this many triangles
var CHEVRON_TRIS = 9000, LIGHT_TRIS = 1200;
var TEX = 1024;
// Faces of the fixed ring steeper than this (the z of their normal) are walls, in plain metal rather
// than the faces' textures, which would be stretched down them
var WALL = 0.6;
// Though a step no deeper than SHORT_WALL (as the edges of the recesses round the chevrons) stays
// in the face's texture, the same as the face either side of it
var SHORT_WALL = 0.025;

var out = [];
function group(name, tris, uv) {
    // tris: [[x, y, z] x 3]; uv(q) gives a corner's texture coordinates
    out.push("g " + name);
    var index = {}, n = 0, verts = [], lines = [];
    tris.forEach(function (t) {
        var ids = t.map(function (q) {
            var w = uv(q), k = q.map(function (x) { return x.toFixed(4); }).join(" ") + " " + w[0].toFixed(4) + " " + w[1].toFixed(4);
            if (index[k] === undefined) { index[k] = n++; verts.push("v " + k); }
            return index[k];
        });
        if (ids[0] != ids[1] && ids[1] != ids[2] && ids[0] != ids[2]) lines.push("t " + ids.join(" "));
    });
    Array.prototype.push.apply(out, verts);
    Array.prototype.push.apply(out, lines);
    print(name + ": " + verts.length + " corners, " + lines.length + " triangles");
}
// Puts a triangle's corners in the order that faces it along the given direction
function facing(t, n) {
    var nn = triNormal(t);
    if (!nn) return null;
    return nn[0] * n[0] + nn[1] * n[1] + nn[2] * n[2] >= 0 ? t : [t[0], t[2], t[1]];
}
// Metal laid flat on whichever of the gate's planes the triangle faces most
function planar(t) {
    var n = triNormal(t) || [0, 0, 1], ax = Math.abs(n[0]), ay = Math.abs(n[1]), az = Math.abs(n[2]);
    return function (q) {
        var k = 1 / 1.2;
        return az >= ax && az >= ay ? [q[0] * k, q[1] * k] : ax >= ay ? [q[1] * k, q[2] * k] : [q[0] * k, q[2] * k];
    };
}
function groupPlanar(name, tris) {
    // Each triangle its own projection: corners are shared only where they come out the same
    group(name, tris.map(function (t) {
        var f = planar(t);
        return t.map(function (q) { var c = q.slice(0, 3); c.uv = f(q); return c; });
    }), function (q) { return q.uv; });
}

// ---- The fixed ring ----------------------------------------------------------------------

var colDeg = HDA, rowM = HDR;
function at(c, r, z) {
    var a = (70 + c * colDeg) * Math.PI / 180, rad = HR0 + r * rowM;
    return [rad * Math.cos(a), rad * Math.sin(a), z];
}
function texUV(c, r) { return [c / SECW, 1 - (r - ROW0) / (ROW1 - ROW0)]; }
var faces = {};
var wallTris = [];
["front", "back"].forEach(function (side) {
    var m = meshFace(side, TOL, MIN_CELL), dir = side == "front" ? [0, 0, 1] : [0, 0, -1];
    var tris = [];
    m.tris.forEach(function (t) {
        var q = t.map(function (i) { var v = m.verts[i], p = at(v[0], v[1], v[2]); p.cr = [v[0], v[1]]; return p; });
        q = facing(q, dir);
        if (!q) return;
        var dz = Math.max(q[0][2], q[1][2], q[2][2]) - Math.min(q[0][2], q[1][2], q[2][2]);
        if (Math.abs(triNormal(q)[2]) < WALL && dz > SHORT_WALL) wallTris.push(q); else tris.push(q);
    });
    faces[side] = m;
    group(side, tris, function (q) { return texUV(q.cr[0], q.cr[1]); });
});

// The bore: strips joining the front's and the back's inner edges, at every column either has a
// corner on that edge; and the outer edge, the same way, from the front's outer edge down its
// profile and back up to the back's
var rimTris = [], outerTris = [];
// The outer edge's profile, (radius, z): the step down at the edge of the front, a groove at its
// foot, the lip sloping out to the rim, and the back the same, as measured by rimmap.js
var OUTER = [[2.9979, 0.101], [2.9853, 0.097], [2.9944, 0.092], [3.0098, 0.083], [3.0405, 0.072], [3.0663, 0.059],
    [3.0712, 0.050], [3.0735, 0.040]];
OUTER = OUTER.concat(OUTER.slice().reverse().map(function (p) { return [p[0], -p[1]]; }));
var RZ0 = -0.22, RZ1 = 0.22;
[[ROW1, 1], [ROW0, -1]].forEach(function (edge) {
    var row = edge[0], cols = {};
    ["front", "back"].forEach(function (side) {
        faces[side].verts.forEach(function (v) { if (v[1] == row) cols[v[0]] = 1; });
    });
    var list = Object.keys(cols).map(Number).sort(function (a, b) { return a - b; });
    function zAt(side, c) {
        // Along that side's edge, between its corners on it
        var vs = faces[side].verts.filter(function (v) { return v[1] == row; }).sort(function (a, b) { return a[0] - b[0]; });
        for (var i = 0; i + 1 < vs.length; i++) {
            if (c >= vs[i][0] && c <= vs[i + 1][0]) {
                var f = (c - vs[i][0]) / Math.max(1e-9, vs[i + 1][0] - vs[i][0]);
                return vs[i][2] + (vs[i + 1][2] - vs[i][2]) * f;
            }
        }
        return vs.length ? vs[0][2] : 0;
    }
    if (edge[1] > 0) {
        // Round the outer edge, at least every half degree, so that it stays round
        for (var c = 0; c <= SECW; c += Math.round(0.5 / colDeg)) cols[c] = 1;
        list = Object.keys(cols).map(Number).sort(function (a, b) { return a - b; });
        var rEdge = HR0 + row * rowM;
        function profile(c) {
            return [[rEdge, zAt("front", c)]].concat(OUTER, [[rEdge, zAt("back", c)]]);
        }
        function atR(c, rad, z) {
            var a = (70 + c * colDeg) * Math.PI / 180, p = [rad * Math.cos(a), rad * Math.sin(a), z];
            p.uv = [c / SECW, (RZ1 - z) / (RZ1 - RZ0)];
            return p;
        }
        for (var i = 0; i + 1 < list.length; i++) {
            var c0 = list[i], c1 = list[i + 1], p0 = profile(c0), p1 = profile(c1);
            var mid = (70 + (c0 + c1) / 2 * colDeg) * Math.PI / 180;
            for (var j = 0; j + 1 < p0.length; j++) {
                var a = atR(c0, p0[j][0], p0[j][1]), b = atR(c1, p1[j][0], p1[j][1]);
                var c = atR(c1, p1[j + 1][0], p1[j + 1][1]), d = atR(c0, p0[j + 1][0], p0[j + 1][1]);
                // Outwards: to the right of the profile going from the front round to the back
                var dr = p0[j + 1][0] - p0[j][0], dz = p0[j + 1][1] - p0[j][1];
                var n = [-dz * Math.cos(mid), -dz * Math.sin(mid), dr];
                [[a, b, c], [a, c, d]].forEach(function (t) { t = facing(t, n); if (t) outerTris.push(t); });
            }
        }
        return;
    }
    for (var i = 0; i + 1 < list.length; i++) {
        var c0 = list[i], c1 = list[i + 1];
        var a = at(c0, row, zAt("front", c0)), b = at(c1, row, zAt("front", c1));
        var c = at(c1, row, zAt("back", c1)), d = at(c0, row, zAt("back", c0));
        var mid = (70 + (c0 + c1) / 2 * colDeg) * Math.PI / 180, n = [Math.cos(mid) * edge[1], Math.sin(mid) * edge[1], 0];
        [[a, b, c], [a, c, d]].forEach(function (t) { t = facing(t, n); if (t) rimTris.push(t); });
    }
});
group("outer", outerTris, function (q) { return q.uv; });
groupPlanar("rim", rimTris);
groupPlanar("wall", wallTris);

// ---- The inner ring: one screen and its divider ------------------------------------------------

function inSlot(t) {
    var c = [(t[0][0] + t[1][0] + t[2][0]) / 3, (t[0][1] + t[1][1] + t[2][1]) / 3];
    var a = angleOf(c);
    return a >= 85 && a < 95;
}
groupPlanar("screen", parts.screens.filter(inSlot));
groupPlanar("separator", parts.separators.filter(inSlot));

// ---- The top chevron ---------------------------------------------------------------------------

function topChevron(t) {
    var c = [(t[0][0] + t[1][0] + t[2][0]) / 3, (t[0][1] + t[1][1] + t[2][1]) / 3];
    return Math.abs(angleOf(c) - 90) < 15;
}
var chev = decimate(parts.chevrons.filter(topChevron), CHEVRON_TRIS);
groupPlanar("chevron", chev.filter(function (t) { return !/Grooves/.test(t.name); }));
groupPlanar("chevron_dark", chev.filter(function (t) { return /Grooves/.test(t.name); }));
var lights = decimate(parts.lights.filter(topChevron), LIGHT_TRIS);
groupPlanar("light_block", lights.filter(function (t) { return t.name == "Chevron_Block_Light"; }));
groupPlanar("light_bars", lights.filter(function (t) { return t.name == "Chevron_Light"; }));

Files.write(Paths.get(outFile), (out.join("\n") + "\n").getBytes("UTF-8"));
print("wrote " + out.length + " lines");

// ---- The fixed ring's textures --------------------------------------------------------------
//
// As on the show: polished silver, with the engraved parts darker, lit by hand where the surface
// is raised or sunk by more than the model shows (its height, against the average round it), and
// a blue patina in the engraving

function paleAt(m, h, c, r) {
    var x = (SEC0 + (c >= SECW ? 0 : c)) % HW;
    for (var d = 0; d < 8; d++) {
        var y = Math.min(ROW1, r + d);
        if (!isNaN(h[y * HW + x])) return m[y * HW + x];
        y = Math.max(ROW0, r - d);
        if (!isNaN(h[y * HW + x])) return m[y * HW + x];
    }
    return 0.5;
}
// The reference marks its worn patches and its engraving as darker materials; drawn as dark as
// that, they are blotchy, so its paleness only shades the metal, the engraving showing more by how
// it is cut in (the lift)
function soften(p) {
    return 0.6 + (p - 0.6) * 0.45;
}
function bilinear(a, cols, fx, fy) {
    var x = Math.floor(fx), y = Math.floor(fy), u = fx - x, v = fy - y, i = y * cols + x;
    return a[i] * (1 - u) * (1 - v) + a[i + 1] * u * (1 - v) + a[i + cols] * (1 - u) * v + a[i + cols + 1] * u * v;
}
// How bright the gate's metal is drawn, against the reference's (to sit beside the Milky Way gate),
// and its tint, a cool blue-gray
var LINE_CELLS = 2;
var TONE = 0.6, METAL_TINT = [0.9, 0.97, 1.1], PATINA_TINT = [0.8, 0.94, 1.16];
// The largest of the values within r cells of each, along rows (dx 1) or columns (dx cols), or the
// least (sign -1)
function spread(a, cols, rows, r, along, sign) {
    var out = new Float64Array(cols * rows);
    for (var y = 0; y < rows; y++) for (var x = 0; x < cols; x++) {
        var best = a[y * cols + x];
        for (var k = -r; k <= r; k++) {
            var xx = along ? Math.max(0, Math.min(cols - 1, x + k)) : x, yy = along ? y : Math.max(0, Math.min(rows - 1, y + k));
            var w = a[yy * cols + xx];
            if (sign * (w - best) > 0) best = w;
        }
        out[y * cols + x] = best;
    }
    return out;
}
// Fills in dark lines narrower than about 2r + 1 cells (the reference's fine scratched lines, which
// break up into grain at any distance), leaving the broader engraving
function closeLines(a, cols, rows, r) {
    a = spread(spread(a, cols, rows, r, true, 1), cols, rows, r, false, 1);
    return spread(spread(a, cols, rows, r, true, -1), cols, rows, r, false, -1);
}
// Evens out a map over r cells either way
function blur(a, cols, rows, r) {
    var tmp = new Float64Array(cols * rows), out = new Float64Array(cols * rows);
    for (var y = 0; y < rows; y++) for (var x = 0; x < cols; x++) {
        var s = 0;
        for (var k = -r; k <= r; k++) s += a[y * cols + Math.max(0, Math.min(cols - 1, x + k))];
        tmp[y * cols + x] = s / (2 * r + 1);
    }
    for (var y = 0; y < rows; y++) for (var x = 0; x < cols; x++) {
        var s = 0;
        for (var k = -r; k <= r; k++) s += tmp[Math.max(0, Math.min(rows - 1, y + k)) * cols + x];
        out[y * cols + x] = s / (2 * r + 1);
    }
    return out;
}
// The metal's color, from how bright it is (v) and how pale the reference has it (p): its
// engraving tinted with a blue patina
function metalRGB(v, p) {
    var f = Math.max(0, Math.min(1, (0.55 - p) / 0.15)), c = [0, 1, 2].map(function (i) {
        return Math.min(255, v * TONE * (METAL_TINT[i] + (PATINA_TINT[i] - METAL_TINT[i]) * f) * 255) | 0;
    });
    return c[0] << 16 | c[1] << 8 | c[2];
}
["front", "back"].forEach(function (side) {
    var h = side == "front" ? hFront : hBack, pm = side == "front" ? paleFront : paleBack, sign = side == "front" ? 1 : -1;
    var cols = SECW, rows = ROW1 - ROW0 + 1;
    var hh = new Float64Array(cols * rows), pp = new Float64Array(cols * rows);
    for (var y = 0; y < rows; y++) for (var x = 0; x < cols; x++) {
        hh[y * cols + x] = sign * heightAt(h, x, ROW0 + y);
        pp[y * cols + x] = paleAt(pm, h, x, ROW0 + y);
    }
    // The average height round each point
    var R = Math.round(0.4 / HDA), avg = new Float64Array(cols * rows), tmp = new Float64Array(cols * rows);
    for (var y = 0; y < rows; y++) for (var x = 0; x < cols; x++) {
        var s = 0, n = 0;
        for (var k = -R; k <= R; k++) { var xx = (x + k + cols) % cols; s += hh[y * cols + xx]; n++; }
        tmp[y * cols + x] = s / n;
    }
    for (var y = 0; y < rows; y++) for (var x = 0; x < cols; x++) {
        var s = 0, n = 0;
        for (var k = -R; k <= R; k++) { var yy = Math.max(0, Math.min(rows - 1, y + k)); s += tmp[yy * cols + x]; n++; }
        avg[y * cols + x] = s / n;
    }
    // How bright each point is
    var vv = new Float64Array(cols * rows);
    for (var i = 0; i < cols * rows; i++) {
        vv[i] = soften(pp[i]) * (1 + Math.max(-0.4, Math.min(0.25, (hh[i] - avg[i]) * 45))) * 0.9;
    }
    vv = blur(vv, cols, rows, 1);
    var img = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_RGB);
    for (var py = 0; py < TEX; py++) for (var px = 0; px < TEX; px++) {
        // Texture coordinates as texUV gives them: u across from the ninth's start, v down from
        // the outer edge
        // Sampled smoothly between the height map's points
        var fx = Math.max(0, Math.min(cols - 1.001, (px + 0.5) / TEX * cols - 0.5)), fy = Math.max(0, Math.min(rows - 1.001, (1 - (py + 0.5) / TEX) * rows - 0.5));
        img.setRGB(px, py, metalRGB(bilinear(vv, cols, fx, fy), bilinear(pp, cols, fx, fy)));
    }
    ImageIO.write(img, "png", new File(texDir + "/" + side + ".png"));
    print("drew " + side + ".png");
});

// The polished silver of the rest of the gate (its edges, the dividers between its screens and
// its chevrons), repeating seamlessly: a little mottled, with a fine grain
(function () {
    var N = 256, img = new BufferedImage(N, N, BufferedImage.TYPE_INT_RGB);
    function hash(x, y, seed) {
        var s = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453;
        return s - Math.floor(s);
    }
    function noise(u, v, cells, seed) {
        var x = u * cells, y = v * cells, xi = Math.floor(x), yi = Math.floor(y), fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        var x0 = ((xi % cells) + cells) % cells, x1 = (x0 + 1) % cells, y0 = ((yi % cells) + cells) % cells, y1 = (y0 + 1) % cells;
        var a = hash(x0, y0, seed), b = hash(x1, y0, seed), c = hash(x0, y1, seed), d = hash(x1, y1, seed);
        return a + (b - a) * fx + (c - a + (a - b + d - c) * fx) * fy;
    }
    for (var y = 0; y < N; y++) for (var x = 0; x < N; x++) {
        var u = x / N, v = y / N;
        // Broad, faint mottling only: finer grain shimmers at any distance
        var m = noise(u, v, 4, 1) * 0.7 + noise(u, v, 8, 2) * 0.3;
        img.setRGB(x, y, metalRGB((0.6 + 0.06 * (m - 0.5)) * 0.9, 0.6));
    }
    ImageIO.write(img, "png", new File(texDir + "/metal.png"));
    print("drew metal.png");
})();

// The outer edge's texture, over (angle, z) as the outer group's corners have it, from rimmap.js's
// map: how pale the surface is there, lit by hand where it stands further out or in than the
// average round it, as the faces' textures are
(function () {
    var rb = ByteBuffer.wrap(Files.readAllBytes(Paths.get(rimFile)));
    var W = rb.getInt(), H = rb.getInt(), DA = rb.getFloat(), DZ = rb.getFloat();
    var rr = new FloatArray(W * H), pl = new FloatArray(W * H);
    for (var i = 0; i < W * H; i++) rr[i] = rb.getFloat();
    for (var i = 0; i < W * H; i++) pl[i] = rb.getFloat();
    var s0 = Math.round(70 / DA), cols = Math.round(40 / DA);
    var r = new Float64Array(cols * H), p = new Float64Array(cols * H);
    for (var y = 0; y < H; y++) for (var x = 0; x < cols; x++) {
        // Filled in from the nearest height where there is a hole
        var k = -1;
        for (var d = 0; d < H && k < 0; d++) {
            [y + d, y - d].forEach(function (yy) {
                if (k < 0 && yy >= 0 && yy < H && !isNaN(rr[yy * W + (s0 + x) % W])) k = yy * W + (s0 + x) % W;
            });
        }
        r[y * cols + x] = k < 0 ? 3.0 : rr[k];
        p[y * cols + x] = k < 0 ? 0.5 : pl[k];
    }
    var R = Math.round(0.4 / DA), tmp = new Float64Array(cols * H), avg = new Float64Array(cols * H);
    for (var y = 0; y < H; y++) for (var x = 0; x < cols; x++) {
        var s = 0;
        for (var k = -R; k <= R; k++) s += r[y * cols + (x + k + cols) % cols];
        tmp[y * cols + x] = s / (2 * R + 1);
    }
    for (var y = 0; y < H; y++) for (var x = 0; x < cols; x++) {
        var s = 0;
        for (var k = -R; k <= R; k++) s += tmp[Math.max(0, Math.min(H - 1, y + k)) * cols + x];
        avg[y * cols + x] = s / (2 * R + 1);
    }
    // How bright each point is. The carved band round the middle of the edge is made of small
    // facets, each its own material in the reference, which drawn as they are break up into
    // shards: it is shaded by its relief, the facets' paleness evened out over a couple of
    // centimeters
    var vv = new Float64Array(cols * H);
    p = blur(p, cols, H, Math.round(0.01 / DZ));
    for (var i = 0; i < cols * H; i++) vv[i] = soften(p[i]) * (1 + Math.max(-0.4, Math.min(0.25, (r[i] - avg[i]) * 45))) * 0.9;
    vv = blur(closeLines(vv, cols, H, LINE_CELLS), cols, H, 1);
    var img = new BufferedImage(TEX, TEX / 2, BufferedImage.TYPE_INT_RGB);
    for (var py = 0; py < TEX / 2; py++) for (var px = 0; px < TEX; px++) {
        // v down from the front (RZ1); the map's rows go up from RZ0
        var z = RZ1 - (py + 0.5) / (TEX / 2) * (RZ1 - RZ0);
        var fx = Math.max(0, Math.min(cols - 1.001, (px + 0.5) / TEX * cols - 0.5)), fy = Math.max(0, Math.min(H - 1.001, (z - RZ0) / DZ - 0.5));
        img.setRGB(px, py, metalRGB(bilinear(vv, cols, fx, fy), bilinear(p, cols, fx, fy)));
    }
    ImageIO.write(img, "png", new File(texDir + "/outer.png"));
    print("drew outer.png");
})();
