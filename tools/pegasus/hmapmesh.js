// Turns a ninth of the Pegasus height maps (from heightmap.js), front and back, into simplified
// meshes of their broad shapes (see cleanFace): bands split where the face steps, each a quadtree
// over (angle, radius), each cell split into four until the shape is within TOL of a flat surface
// over it, and the leaves fanned into triangles from their middles, taking in the corners of
// smaller neighbors so that there are no cracks; joined by upright walls. Loaded by the other
// scripts with `hmapFile` set; meshFace(side, tol, minSize) gives {verts: [[col, row, z]],
// tris: [[i, j, k]]}, in height map cells from the ninth's start (70 degrees) and from R0.
var Files = Java.type("java.nio.file.Files"), Paths = Java.type("java.nio.file.Paths");
var ByteBuffer = Java.type("java.nio.ByteBuffer");
var FloatArray = Java.type("float[]");
var hb = ByteBuffer.wrap(Files.readAllBytes(Paths.get(hmapFile)));
var HW = hb.getInt(), HH = hb.getInt(), HDA = hb.getFloat(), HDR = hb.getFloat(), HR0 = 2.20;
var hFront = new FloatArray(HW * HH), hBack = new FloatArray(HW * HH);
for (var i = 0; i < HW * HH; i++) hFront[i] = hb.getFloat();
for (var i = 0; i < HW * HH; i++) hBack[i] = hb.getFloat();
var paleFront = new FloatArray(HW * HH), paleBack = new FloatArray(HW * HH);
if (hb.remaining() >= 8 * HW * HH) {
    for (var i = 0; i < HW * HH; i++) paleFront[i] = hb.getFloat();
    for (var i = 0; i < HW * HH; i++) paleBack[i] = hb.getFloat();
}
// The ninth's columns: from 70 degrees, 40 degrees' worth, and the rows that have the ring
var SEC0 = Math.round(70 / HDA), SECW = Math.round(40 / HDA);
var ROW0 = HH, ROW1 = 0;
for (var y = 0; y < HH; y++) for (var x = 0; x < HW; x++) {
    if (!isNaN(hFront[y * HW + x])) { ROW0 = Math.min(ROW0, y); ROW1 = Math.max(ROW1, y); }
}
// The faces stop short of the outer edge if FACE_TO (meters out) is set; the edge is drawn
// another way (see rimmap.js)
if (typeof FACE_TO == "number") ROW1 = Math.min(ROW1, Math.round((FACE_TO - HR0) / HDR));

// The height at a column and row of the ninth (column 0 to SECW, row ROW0 to ROW1), filled in
// from the nearest row where there is a hole. Its last column is its first, so that each ninth
// meets the next without a crack.
function heightAt(m, c, r) {
    var x = (SEC0 + (c >= SECW ? 0 : c)) % HW;
    for (var d = 0; d < 8; d++) {
        var y1 = Math.min(ROW1, r + d), y2 = Math.max(ROW0, r - d);
        var z = m[y1 * HW + x];
        if (!isNaN(z)) return z;
        z = m[y2 * HW + x];
        if (!isNaN(z)) return z;
    }
    return 0;
}

// ---- The faces' broad shapes -----------------------------------------------------------------
//
// Seen in Minecraft, the faces' fine carving (the arches, the zigzags, the ornate band at the
// outer edge, about a centimeter deep) is drawn in their textures; meshed, it crumples the
// surface into creases and slivers. So the faces are meshed from their height maps cleaned of
// it: each a turned profile (its typical height at each radius, away from the chevrons), with
// only the broad features that stand out of it by more than KEEP_DEPTH and are wider than
// KEEP_WIDTH (the recesses round the chevrons, the slots under them, the panels between them on
// the back) kept, evened out by the median round each point so that their own carving goes too.
var KEEP_DEPTH = 0.008, KEEP_WIDTH = 0.03, BEVEL = 0.008;
// Where the profile steps by more than STEP between rows, the face is split into bands meshed on
// their own, joined by upright walls, so that each step stays a clean edge
var STEP = 0.004;
// The largest cell, in columns: small enough that the faces stay round
var MAX_CELL = 32;

// A face's typical height at each row
function profileOf(m) {
    var prof = [];
    for (var r = ROW0; r <= ROW1; r++) {
        var vals = [];
        for (var x = 0; x < HW; x++) {
            var d = ((x * HDA - 10) % 40 + 40) % 40;
            if (d < 10 || d > 30) continue;
            var z = m[r * HW + x];
            if (!isNaN(z)) vals.push(z);
        }
        vals.sort(function (a, b) { return a - b; });
        prof[r] = vals.length ? vals[vals.length >> 1] : (r > ROW0 ? prof[r - 1] : 0);
    }
    return prof;
}

// A face's broad shape over the ninth: {z: heights, columns 0 to SECW by rows ROW0 to ROW1,
// prof: its profile by row}
function cleanFace(m) {
    var cols = SECW + 1, rows = ROW1 - ROW0 + 1, prof = profileOf(m);
    var dev = new Float64Array(cols * rows), keep = new Uint8Array(cols * rows);
    for (var y = 0; y < rows; y++) for (var c = 0; c < cols; c++) {
        var dv = heightAt(m, c, ROW0 + y) - prof[ROW0 + y];
        dev[y * cols + c] = dv;
        keep[y * cols + c] = Math.abs(dv) > KEEP_DEPTH ? 1 : 0;
    }
    // Opened (shrunk and grown again by half KEEP_WIDTH), which drops whatever is narrower
    var rc = Math.max(1, Math.round(KEEP_WIDTH / 2 / (HDA * Math.PI / 180 * 2.6))), rr = Math.max(1, Math.round(KEEP_WIDTH / 2 / HDR));
    function morph(a, grow) {
        var t = new Uint8Array(cols * rows), o = new Uint8Array(cols * rows);
        for (var y = 0; y < rows; y++) for (var c = 0; c < cols; c++) {
            var v = grow ? 0 : 1;
            for (var k = -rc; k <= rc; k++) {
                var w = a[y * cols + Math.max(0, Math.min(cols - 1, c + k))];
                if (grow ? w : !w) { v = grow ? 1 : 0; break; }
            }
            t[y * cols + c] = v;
        }
        for (var y = 0; y < rows; y++) for (var c = 0; c < cols; c++) {
            var v = grow ? 0 : 1;
            for (var k = -rr; k <= rr; k++) {
                var w = t[Math.max(0, Math.min(rows - 1, y + k)) * cols + c];
                if (grow ? w : !w) { v = grow ? 1 : 0; break; }
            }
            o[y * cols + c] = v;
        }
        return o;
    }
    keep = morph(morph(keep, false), true);
    // Within what is kept, the median round each point, of what is kept
    var out = new Float64Array(cols * rows), mc = Math.max(1, rc >> 1), mr = Math.max(1, rr >> 1), sr = Math.max(1, mr >> 1);
    for (var y = 0; y < rows; y++) for (var c = 0; c < cols; c++) {
        var i = y * cols + c, z = prof[ROW0 + y];
        if (keep[i]) {
            var vals = [];
            for (var dy = -mr; dy <= mr; dy += sr) for (var dc = -mc; dc <= mc; dc++) {
                var yy = y + dy, cc = c + dc;
                if (yy < 0 || yy >= rows || cc < 0 || cc >= cols || !keep[yy * cols + cc]) continue;
                vals.push(dev[yy * cols + cc]);
            }
            vals.sort(function (a, b) { return a - b; });
            z += vals[vals.length >> 1];
        }
        out[i] = z;
    }
    // Their edges bevelled, evened out over BEVEL either way: an edge slanting across the height
    // map's grid, kept sharp, can only be meshed as a staircase along it; bevelled, it is a
    // slope, which flat triangles follow
    var bc = Math.max(1, Math.round(BEVEL / (HDA * Math.PI / 180 * 2.6))), br = Math.max(1, Math.round(BEVEL / HDR));
    var lift = new Float64Array(cols * rows), tmp = new Float64Array(cols * rows);
    for (var y = 0; y < rows; y++) for (var c = 0; c < cols; c++) lift[y * cols + c] = out[y * cols + c] - prof[ROW0 + y];
    for (var y = 0; y < rows; y++) for (var c = 0; c < cols; c++) {
        var s = 0;
        for (var k = -bc; k <= bc; k++) s += lift[y * cols + Math.max(0, Math.min(cols - 1, c + k))];
        tmp[y * cols + c] = s / (2 * bc + 1);
    }
    for (var y = 0; y < rows; y++) for (var c = 0; c < cols; c++) {
        var s = 0;
        for (var k = -br; k <= br; k++) s += tmp[Math.max(0, Math.min(rows - 1, y + k)) * cols + c];
        out[y * cols + c] = prof[ROW0 + y] + s / (2 * br + 1);
    }
    // The ninth's last column is its first, so that it meets the next without a crack
    for (var y = 0; y < rows; y++) out[y * cols + SECW] = out[y * cols];
    return {z: out, prof: prof};
}

function meshFace(side, tol, minSize) {
    var m = side == "front" ? hFront : hBack, cf = cleanFace(m), cols = SECW + 1;
    function zAt(c, r) { return cf.z[(Math.round(r) - ROW0) * cols + Math.round(c)]; }
    // The bands, split where the profile steps
    var bands = [], start = ROW0;
    for (var r = ROW0; r < ROW1; r++) {
        if (Math.abs(cf.prof[r + 1] - cf.prof[r]) > STEP) { bands.push([start, r]); start = r + 1; }
    }
    bands.push([start, ROW1]);
    var verts = [], index = {}, tris = [], leafCount = 0;
    function key(c, r) { return c + "," + r; }
    function vert(c, r) {
        var k = key(c, r);
        if (index[k] === undefined) { index[k] = verts.length; verts.push([c, r, zAt(c, r)]); }
        return index[k];
    }
    // Each band's corners along its edge rows, for the walls between them
    var edgeCols = {};
    bands.forEach(function (band) {
        var ra = band[0], rb = band[1];
        if (rb <= ra) {
            // A band a single row deep: nothing but its edge, joined to its neighbors by walls
            var list = [];
            for (var c = 0; c < SECW; c += MAX_CELL) list.push(c);
            list.push(SECW);
            edgeCols[ra] = list;
            return;
        }
        var top = 1;
        while (top < Math.max(SECW, rb - ra)) top *= 2;
        var leaves = [];
        function flat(c, r, s) {
            var c1 = Math.min(c + s, SECW), r1 = Math.min(r + s, rb);
            if (c1 <= c || r1 <= r) return true;
            var z00 = zAt(c, r), z10 = zAt(c1, r), z01 = zAt(c, r1), z11 = zAt(c1, r1);
            for (var y = r; y <= r1; y++) for (var x = c; x <= c1; x++) {
                var u = (x - c) / (c1 - c), v = (y - r) / (r1 - r);
                var z = z00 * (1 - u) * (1 - v) + z10 * u * (1 - v) + z01 * (1 - u) * v + z11 * u * v;
                if (Math.abs(zAt(x, y) - z) > tol) return false;
            }
            return true;
        }
        function split(c, r, s) {
            if (c >= SECW || r >= rb) return;
            if (s <= minSize || (s <= MAX_CELL && flat(c, r, s))) { leaves.push([c, r, s]); return; }
            var h = s / 2;
            split(c, r, h); split(c + h, r, h); split(c, r + h, h); split(c + h, r + h, h);
        }
        split(0, ra, top);
        leafCount += leaves.length;
        // Corners in use, by position, so that each leaf takes in the corners on its edges
        var cornerAt = {};
        leaves.forEach(function (l) {
            var c1 = Math.min(l[0] + l[2], SECW), r1 = Math.min(l[1] + l[2], rb);
            [[l[0], l[1]], [c1, l[1]], [l[0], r1], [c1, r1]].forEach(function (p) { cornerAt[key(p[0], p[1])] = p; });
        });
        // The ninth's two ends have the same corners, so that it meets the next ninth along the
        // same edges
        Object.keys(cornerAt).forEach(function (k) {
            var p = cornerAt[k];
            if (p[0] == 0) cornerAt[key(SECW, p[1])] = [SECW, p[1]];
            if (p[0] == SECW) cornerAt[key(0, p[1])] = [0, p[1]];
        });
        var byRow = {}, byCol = {};
        Object.keys(cornerAt).forEach(function (k) {
            var p = cornerAt[k];
            (byRow[p[1]] = byRow[p[1]] || []).push(p[0]);
            (byCol[p[0]] = byCol[p[0]] || []).push(p[1]);
        });
        Object.keys(byRow).forEach(function (k) { byRow[k].sort(function (a, b) { return a - b; }); });
        Object.keys(byCol).forEach(function (k) { byCol[k].sort(function (a, b) { return a - b; }); });
        edgeCols[ra] = byRow[ra];
        edgeCols[rb] = byRow[rb];
        leaves.forEach(function (l) {
            var c0 = l[0], r0 = l[1], c1 = Math.min(l[0] + l[2], SECW), r1 = Math.min(l[1] + l[2], rb);
            // Round the edge, anticlockwise in (column, row): along the bottom, up the right, back
            // along the top, down the left, with every corner on each edge
            var ring = [];
            (byRow[r0] || []).forEach(function (c) { if (c >= c0 && c < c1) ring.push([c, r0]); });
            (byCol[c1] || []).forEach(function (r) { if (r >= r0 && r < r1) ring.push([c1, r]); });
            (byRow[r1] || []).slice().reverse().forEach(function (c) { if (c <= c1 && c > c0) ring.push([c, r1]); });
            (byCol[c0] || []).slice().reverse().forEach(function (r) { if (r <= r1 && r > r0) ring.push([c0, r]); });
            if (ring.length == 4) {
                var a = vert(c0, r0), b = vert(c1, r0), cc = vert(c1, r1), d = vert(c0, r1);
                tris.push([a, b, cc], [a, cc, d]);
            } else {
                var mid = vert(Math.round((c0 + c1) / 2), Math.round((r0 + r1) / 2));
                for (var i = 0; i < ring.length; i++) {
                    var p = ring[i], q = ring[(i + 1) % ring.length];
                    tris.push([mid, vert(p[0], p[1]), vert(q[0], q[1])]);
                }
            }
        });
    });
    // The walls between bands: from one band's top row to the next one's bottom row, at every
    // column either has a corner on its edge, the other's height there taken along its edge
    function along(list, row, c) {
        for (var i = 0; i + 1 < list.length; i++) {
            if (c >= list[i] && c <= list[i + 1]) {
                var f = (c - list[i]) / Math.max(1e-9, list[i + 1] - list[i]);
                return zAt(list[i], row) + (zAt(list[i + 1], row) - zAt(list[i], row)) * f;
            }
        }
        return zAt(c, row);
    }
    for (var b = 0; b + 1 < bands.length; b++) {
        var r0 = bands[b][1], r1 = bands[b + 1][0], l0 = edgeCols[r0] || [0, SECW], l1 = edgeCols[r1] || [0, SECW], u = {};
        l0.concat(l1).forEach(function (c) { u[c] = 1; });
        var list = Object.keys(u).map(Number).sort(function (p, q) { return p - q; });
        function wallVert(c, r, z) {
            var k = key(c, r) + ":" + z.toFixed(5);
            if (index[k] === undefined) { index[k] = verts.length; verts.push([c, r, z]); }
            return index[k];
        }
        for (var i = 0; i + 1 < list.length; i++) {
            var c0 = list[i], c1 = list[i + 1];
            var a = wallVert(c0, r0, along(l0, r0, c0)), bb = wallVert(c1, r0, along(l0, r0, c1));
            var cc = wallVert(c1, r1, along(l1, r1, c1)), d = wallVert(c0, r1, along(l1, r1, c0));
            tris.push([a, bb, cc], [a, cc, d]);
        }
    }
    print(side + ": " + bands.length + " bands, " + leafCount + " cells");
    return {verts: verts, tris: tris, leaves: leafCount};
}
