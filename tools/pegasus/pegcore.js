// The Pegasus gate reference model in the gate's own frame and units: meters, the ring in the x-y
// plane, +y up, the front (the side with the inner ring's screens) towards +z, so that seen from in
// front +x is to the right. The file has the front towards -z, so this is it turned half a turn
// about y. Loaded after objcore.js. parts: {static, chevrons, lights, screens, separators, rim},
// each a list of triangles [[x, y, z] x 3] with the material name as .name; `keepPart(name)` may
// be set first to read only some.
var partOfMaterial = function (name) {
    if (/^Chevron_(Block_)?Light$/.test(name)) return "lights";
    if (/^Chevron/.test(name)) return "chevrons";
    if (name == "Inner_Ring_Surface" || name == "Inner_Ring_Interior") return "screens";
    if (name == "Inner_Ring_Glyph_Separator") return "separators";
    return "static";
};
var parts = {static: [], chevrons: [], lights: [], screens: [], separators: []};
prims.forEach(function (p) {
    var part = partOfMaterial(p.name);
    if (typeof keepPart == "function" && !keepPart(part)) return;
    var P = p.pos, I = p.idx, list = parts[part];
    for (var t = 0; t < I.length; t += 3) {
        var tri = [0, 1, 2].map(function (e) {
            var k = I[t + e] * 3;
            return [-P[k], P[k + 1], -P[k + 2]];
        });
        tri.name = p.name;
        list.push(tri);
    }
});

function radius(q) { return Math.sqrt(q[0] * q[0] + q[1] * q[1]); }
// Degrees anticlockwise from +x, seen from in front
function angleOf(q) { return Math.atan2(q[1], q[0]) * 180 / Math.PI; }
function triNormal(v) {
    var ux = v[1][0] - v[0][0], uy = v[1][1] - v[0][1], uz = v[1][2] - v[0][2];
    var wx = v[2][0] - v[0][0], wy = v[2][1] - v[0][1], wz = v[2][2] - v[0][2];
    var nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx, l = Math.sqrt(nx * nx + ny * ny + nz * nz);
    return l < 1e-14 ? null : [nx / l, ny / l, nz / l];
}

// The segments, in (r, z), where the triangles cross the half plane at the given angle
function slice(tris, deg) {
    var a = deg * Math.PI / 180, nx = -Math.sin(a), ny = Math.cos(a), dx = Math.cos(a), dy = Math.sin(a);
    var segs = [];
    tris.forEach(function (v) {
        var d = v.map(function (q) { return q[0] * nx + q[1] * ny; });
        var pts = [];
        for (var e = 0; e < 3; e++) {
            var p = v[e], q = v[(e + 1) % 3], dp = d[e], dq = d[(e + 1) % 3];
            if ((dp > 0) != (dq > 0)) {
                var t = dp / (dp - dq);
                var x = p[0] + (q[0] - p[0]) * t, y = p[1] + (q[1] - p[1]) * t, z = p[2] + (q[2] - p[2]) * t;
                if (x * dx + y * dy > 0) pts.push([Math.sqrt(x * x + y * y), z]);
            }
        }
        if (pts.length == 2) segs.push(pts);
    });
    return segs;
}
