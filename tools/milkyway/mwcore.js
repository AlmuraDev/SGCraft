// The Milky Way gate reference model in the gate's own frame and units: meters, the ring in the
// x-y plane, +y up, the front (the side with the chevron lights) towards +z, so that seen from in
// front +x is to the right. The file has the front towards -z, so this is it turned half a turn
// about y. Loaded after fbxcore.js. parts: {static, chevrons, lights, ring}, each a list of
// triangles [[x, y, z] x 3] with the material name as .name
var parts = {static: [], chevrons: [], lights: [], ring: []};
prims.forEach(function (p) {
    var P = p.pos, I = p.idx;
    var list = p.model == "Ring" || p.model == "GizaPoO" ? parts.ring
        : p.name == "ChevronBase" ? parts.chevrons : /^ChevronLight/.test(p.name) ? parts.lights : parts.static;
    for (var t = 0; t < I.length; t += 3) {
        var tri = [0, 1, 2].map(function (e) {
            var k = I[t + e] * 3;
            return [-P[k] / 100, P[k + 1] / 100, -P[k + 2] / 100];
        });
        tri.name = p.name;
        tri.vertex = [I[t], I[t + 1], I[t + 2]];
        tri.model = p.model;
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
