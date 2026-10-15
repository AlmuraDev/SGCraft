// Measures the raised detail of the Destiny gate reference model into outlines: every upward
// facing flat face that isn't part of the ring's revolved body, on the front, between the
// middle of one post between arches (70 degrees) and the post as far the other side (110), so a whole chevron
// (at 90) in the middle, merged with the faces
// it continues flat into. Writes one line per face: its part and its outline as
// (degrees from the chevron's center, radius, z).
// jjs extract.js -- <scratch> <glb> <out file>
var file = arguments[1], out = arguments[0];
load(arguments[0] + "/glbcore.js");
var outFile = arguments[2];
var A0 = 70, A1 = 110;

// The front of the ring's revolved body: its profile from the inner edge out
var body = [[2.5188, 0.0771], [2.5209, 0.0828], [2.5261, 0.0859], [2.6479, 0.1080], [2.6699, 0.1403], [2.7414, 0.1482],
    [3.1400, 0.1864], [3.2618, 0.1969], [3.2618, 0.2139], [3.2671, 0.2201], [3.3290, 0.2314], [3.3436, 0.2300],
    [3.3542, 0.2211], [3.3581, 0.2070], [3.3581, 0.1534], [3.3870, 0.1176], [3.3870, 0.0566], [3.4111, 0.0570]];
function onBody(r, z) {
    for (var i = 0; i + 1 < body.length; i++) {
        var a = body[i], b = body[i + 1];
        var dx = b[0] - a[0], dz = b[1] - a[1], l2 = dx * dx + dz * dz;
        var t = Math.max(0, Math.min(1, ((r - a[0]) * dx + (z - a[1]) * dz) / l2));
        var ex = a[0] + dx * t - r, ez = a[1] + dz * t - z;
        if (ex * ex + ez * ez < 0.0006 * 0.0006) return true;
    }
    return false;
}

// Clips a polygon of [x, y, z] to the side of the plane through the axis at angle a where
// (sign) * (distance round) >= 0
function clip(poly, a, sign) {
    var nx = -Math.sin(a * Math.PI / 180) * sign, ny = Math.cos(a * Math.PI / 180) * sign;
    var res = [];
    for (var i = 0; i < poly.length; i++) {
        var p = poly[i], q = poly[(i + 1) % poly.length];
        var dp = p[0] * nx + p[1] * ny, dq = q[0] * nx + q[1] * ny;
        if (dp >= 0) res.push(p);
        if ((dp >= 0) != (dq >= 0)) {
            var t = dp / (dp - dq);
            res.push([p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] + (q[2] - p[2]) * t]);
        }
    }
    return res;
}

var faces = [];
prims.forEach(function (p, pi) {
    var P = p.pos, I = p.idx;
    for (var t = 0; t < I.length; t += 3) {
        var v = [0, 1, 2].map(function (e) { var k = I[t + e] * 3; return [P[k], P[k + 1], P[k + 2]]; });
        var cz = (v[0][2] + v[1][2] + v[2][2]) / 3;
        if (cz <= 0) continue;
        var ang = v.map(function (q) { return Math.atan2(q[1], q[0]) * 180 / Math.PI; });
        if (Math.max.apply(null, ang) < A0 - 0.01 || Math.min.apply(null, ang) > A1 + 0.01) continue;
        if (Math.max.apply(null, ang) - Math.min.apply(null, ang) > 90) continue;
        var ux = v[1][0] - v[0][0], uy = v[1][1] - v[0][1], uz = v[1][2] - v[0][2];
        var wx = v[2][0] - v[0][0], wy = v[2][1] - v[0][1], wz = v[2][2] - v[0][2];
        var nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx, l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-12 || nz / l < 0.05) continue;
        if (v.every(function (q) { return onBody(Math.sqrt(q[0] * q[0] + q[1] * q[1]), q[2]); })) continue;
        var poly = clip(clip(v, A0, 1), A1, -1);
        if (poly.length < 3) continue;
        faces.push({part: pi, poly: poly, n: [nx / l, ny / l, nz / l], d: (nx * v[0][0] + ny * v[0][1] + nz * v[0][2]) / l});
    }
});

// Merge clipped pieces that share an edge and lie in the same plane
function key(q) { return Math.round(q[0] * 2e4) + "," + Math.round(q[1] * 2e4) + "," + Math.round(q[2] * 2e4); }
var edges = {};
faces.forEach(function (f, i) {
    for (var e = 0; e < f.poly.length; e++) {
        var a = key(f.poly[e]), b = key(f.poly[(e + 1) % f.poly.length]);
        if (a == b) continue;
        var ek = a < b ? a + "|" + b : b + "|" + a;
        (edges[ek] = edges[ek] || []).push(i);
    }
});
var group = new Array(faces.length), groups = [];
for (var i = 0; i < faces.length; i++) {
    if (group[i] !== undefined) continue;
    var g = groups.length, stack = [i], members = [];
    group[i] = g;
    while (stack.length) {
        var c = stack.pop(); members.push(c);
        var f = faces[c];
        for (var e = 0; e < f.poly.length; e++) {
            var a = key(f.poly[e]), b = key(f.poly[(e + 1) % f.poly.length]);
            if (a == b) continue;
            (edges[a < b ? a + "|" + b : b + "|" + a] || []).forEach(function (o) {
                if (group[o] !== undefined || faces[o].part != faces[i].part) return;
                var n1 = faces[o].n, n0 = faces[i].n;
                if (n1[0] * n0[0] + n1[1] * n0[1] + n1[2] * n0[2] > 0.9998 && Math.abs(faces[o].d - faces[i].d) < 0.0005) {
                    group[o] = g; stack.push(o);
                }
            });
        }
    }
    groups.push(members);
}

var lines = [], holes = 0;
groups.forEach(function (m) {
    var count = {}, from = {};
    m.forEach(function (c) {
        var f = faces[c];
        for (var e = 0; e < f.poly.length; e++) {
            var a = f.poly[e], b = f.poly[(e + 1) % f.poly.length], ka = key(a), kb = key(b);
            if (ka == kb) continue;
            var ek = ka < kb ? ka + "|" + kb : kb + "|" + ka;
            count[ek] = (count[ek] || 0) + 1;
            from[ek] = [a, b];
        }
    });
    var next = {};
    Object.keys(count).forEach(function (ek) { if (count[ek] == 1) next[key(from[ek][0])] = from[ek]; });
    var seen = {}, loops = [];
    Object.keys(next).forEach(function (s) {
        if (seen[s]) return;
        var loop = [], cur = s, guard = 0;
        while (!seen[cur] && next[cur] && guard++ < 5000) { seen[cur] = 1; loop.push(next[cur][0]); cur = key(next[cur][1]); }
        if (loop.length >= 3) loops.push(loop);
    });
    if (loops.length > 1) holes++;
    loops.forEach(function (loop) {
        // drop points in the middle of straight runs
        var keep = [];
        for (var i = 0; i < loop.length; i++) {
            var a = loop[(i + loop.length - 1) % loop.length], b = loop[i], c = loop[(i + 1) % loop.length];
            var ab = [b[0] - a[0], b[1] - a[1], b[2] - a[2]], bc = [c[0] - b[0], c[1] - b[1], c[2] - b[2]];
            var cr = [ab[1] * bc[2] - ab[2] * bc[1], ab[2] * bc[0] - ab[0] * bc[2], ab[0] * bc[1] - ab[1] * bc[0]];
            var lab = Math.sqrt(ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2]), lbc = Math.sqrt(bc[0] * bc[0] + bc[1] * bc[1] + bc[2] * bc[2]);
            if (lab < 1e-7 || lbc < 1e-7) continue;
            if (Math.sqrt(cr[0] * cr[0] + cr[1] * cr[1] + cr[2] * cr[2]) / (lab * lbc) > 1e-4) keep.push(b);
        }
        if (keep.length < 3) return;
        lines.push("abcdefgh".charAt(faces[m[0]].part) + " " + keep.map(function (q) {
            return (90 - Math.atan2(q[1], q[0]) * 180 / Math.PI).toFixed(4) + " " + Math.sqrt(q[0] * q[0] + q[1] * q[1]).toFixed(4) + " " + q[2].toFixed(4);
        }).join(" "));
    });
});
Files.write(Paths.get(outFile), (lines.join("\n") + "\n").getBytes("UTF-8"));
print("faces " + faces.length + " groups " + groups.length + " outlines " + lines.length + " with holes " + holes);
