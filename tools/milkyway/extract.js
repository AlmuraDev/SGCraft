// Measures the Milky Way gate reference model into the data the mod builds its own model from
// (see MilkyWayGateModel): the profiles its two bodies are turned from, and as polygons, what
// stands out of or is cut into them:
//
//   p static|ring  r z r z ...        a body's profile, in (radius, z) meters
//   s x y z x y z ...                 the fixed ring's detail, for the ninth of it round the top
//                                     chevron (70 to 110 degrees); the rest is that, turned
//   c column segment                  a cell of the fixed body (column j from 70 + 4j to 74 + 4j
//                                     degrees, along that segment of its profile) left out of the
//                                     turned body, as something is cut into it; the model's own
//                                     faces there are among the s polygons
//   r x y z ...                       the symbol ring's detail, all round, at rest
//   v x y z m ...  /  l x y z m ...   the top chevron, and its lights, each corner with how it
//                                     moves as the chevron locks a symbol (0 not at all, 1 or 2;
//                                     see the m lines)
//   t x y z m ...                     the lower part of the lights' triangle, below where the
//                                     slatted lights begin, lit only as the top chevron locks
//   m n dx dy dz                      how far movement n takes a corner, locked
//
// All in the gate's own frame (see mwcore.js). Faces that lie in one plane and share edges are
// merged into one polygon where it stays a simple outline.
// jjs extract.js -- <scratch> <fbx> <out file>
var file = arguments[1];
load(arguments[0] + "/fbxcore.js");
load(arguments[0] + "/mwcore.js");
var outFile = arguments[2];

var STATIC = [[4.8353, 0.0439], [4.8469, 0.0865], [4.8526, 0.0909], [4.8103, 0.1318], [4.5490, 0.0746], [4.5490, 0.0562],
    [4.5724, 0.0475], [4.5724, -0.0475], [4.5490, -0.0562], [4.5490, -0.0746], [4.8002, -0.1318], [4.8197, -0.1014],
    [4.9261, -0.1653], [5.3510, -0.2531], [5.4484, -0.2536], [5.5024, -0.3162], [5.9597, -0.4148], [5.9592, -0.2107],
    [5.9353, -0.2107], [5.9353, -0.1934], [6.0960, -0.1052], [6.0960, 0.1052], [5.9353, 0.1934], [5.9353, 0.2107],
    [5.9592, 0.2107], [5.9638, 0.4148], [5.4139, 0.2654], [5.4139, 0.1944]];
var RING = [[4.8436, 0.0663], [5.4408, 0.2243]];
var A0 = 70, A1 = 110, COLUMN = 4;

// Distance from a profile in (r, z), and the nearest segment
function nearest(prof, r, z) {
    var best = 1e9, seg = -1;
    for (var i = 0; i + 1 < prof.length; i++) {
        var a = prof[i], b = prof[i + 1], dx = b[0] - a[0], dz = b[1] - a[1], l2 = dx * dx + dz * dz;
        var t = Math.max(0, Math.min(1, ((r - a[0]) * dx + (z - a[1]) * dz) / l2));
        var ex = a[0] + dx * t - r, ez = a[1] + dz * t - z, d = Math.sqrt(ex * ex + ez * ez);
        if (d < best) { best = d; seg = i; }
    }
    return {d: best, seg: seg};
}
function inside(prof, r, z) {
    var c = false;
    for (var i = 0, j = prof.length - 1; i < prof.length; j = i++) {
        var a = prof[i], b = prof[j];
        if ((a[1] > z) != (b[1] > z) && r < (b[0] - a[0]) * (z - a[1]) / (b[1] - a[1]) + a[0]) c = !c;
    }
    return c;
}
// The fixed body is faceted, a flat face every 4 degrees with its corners at 2 + 4k: moves a
// point out as far as its facet falls short of the round body there, so that the model's own faces
// meet the turned body without gaps
function unfacet(q) {
    var a = angleOf(q), c = Math.round(a / COLUMN) * COLUMN;
    var k = Math.cos((a - c) * Math.PI / 180) / Math.cos(COLUMN / 2 * Math.PI / 180);
    return [q[0] * k, q[1] * k, q[2]];
}
// Clips a polygon to the side of the plane through the axis at angle a where (sign) * (distance
// round) >= 0
function clip(poly, a, sign) {
    var nx = -Math.sin(a * Math.PI / 180) * sign, ny = Math.cos(a * Math.PI / 180) * sign;
    var res = [];
    for (var i = 0; i < poly.length; i++) {
        var p = poly[i], q = poly[(i + 1) % poly.length];
        var dp = p[0] * nx + p[1] * ny, dq = q[0] * nx + q[1] * ny;
        if (dp >= 0) res.push(p);
        if ((dp >= 0) != (dq >= 0)) {
            var t = dp / (dp - dq);
            var m = [];
            for (var e = 0; e < p.length; e++) m.push(p[e] + (q[e] - p[e]) * t);
            res.push(m);
        }
    }
    return res;
}

var out = [];
out.push("p static " + STATIC.map(function (q) { return q[0].toFixed(4) + " " + q[1].toFixed(4); }).join(" "));
out.push("p ring " + RING.map(function (q) { return q[0].toFixed(4) + " " + q[1].toFixed(4); }).join(" "));

// The fixed ring
var cut = {}, staticFaces = [], bodyFaces = [];
parts.static.forEach(function (v) {
    var ang = v.map(angleOf);
    if (Math.max.apply(null, ang) < A0 - 0.01 || Math.min.apply(null, ang) > A1 + 0.01) return;
    if (Math.max.apply(null, ang) - Math.min.apply(null, ang) > 90) return;
    var poly = clip(clip(v.map(unfacet), A0, 1), A1, -1);
    if (poly.length < 3) return;
    var near = poly.map(function (q) { return nearest(STATIC, radius(q), q[2]); });
    var c = [0, 0, 0];
    poly.forEach(function (q) { c[0] += q[0] / poly.length; c[1] += q[1] / poly.length; c[2] += q[2] / poly.length; });
    var cn = nearest(STATIC, radius(c), c[2]);
    if (near.every(function (n) { return n.d < 0.001; }) && cn.d < 0.001) {
        // The columns it reaches across
        var angs = poly.map(angleOf);
        var c0 = Math.max(0, Math.floor((Math.min.apply(null, angs) - A0) / COLUMN + 1e-4));
        var c1 = Math.min(9, Math.floor((Math.max.apply(null, angs) - A0) / COLUMN - 1e-4));
        bodyFaces.push({poly: poly, cols: [c0, Math.max(c0, c1)], seg: cn.seg});
        return;
    }
    staticFaces.push(poly);
    // Cut into the body: the cells round it are left to the model's own faces
    var deep = poly.filter(function (q, i) { return near[i].d > 0.002 && inside(STATIC, radius(q), q[2]); });
    if (deep.length) {
        var cols = poly.map(function (q) { return Math.max(0, Math.min(9, Math.floor((angleOf(q) - A0) / COLUMN))); });
        var c0 = Math.min.apply(null, cols), c1 = Math.max.apply(null, cols);
        poly.forEach(function (q) {
            for (var s = 0; s + 1 < STATIC.length; s++) {
                if (nearest([STATIC[s], STATIC[s + 1]], radius(q), q[2]).d < 0.1) {
                    for (var j = c0; j <= c1; j++) cut[j + ":" + s] = 1;
                }
            }
        });
    }
});
var cutBody = 0;
bodyFaces.forEach(function (f) {
    // Kept where any of it is cut, so that the turned body and these leave no gap between them
    for (var j = f.cols[0]; j <= f.cols[1]; j++) {
        if (cut[j + ":" + f.seg]) { staticFaces.push(f.poly); cutBody++; break; }
    }
});
print("fixed ring: detail " + (staticFaces.length - cutBody) + ", body faces kept where cut " + cutBody + ", cells cut " + Object.keys(cut).length);
Object.keys(cut).forEach(function (k) { out.push("c " + k.replace(":", " ")); });

// The symbol ring: everything off its cone
var ringSlope = (RING[1][1] - RING[0][1]) / (RING[1][0] - RING[0][0]);
function coneZ(r) { return RING[0][1] + (r - RING[0][0]) * ringSlope; }
var cn = [-ringSlope, 1], cl = Math.sqrt(1 + ringSlope * ringSlope);
var ringFaces = [], below = 0;
parts.ring.forEach(function (v) {
    var h = v.map(function (q) { return q[2] - coneZ(radius(q)); });
    var n = triNormal(v);
    if (!n) return;
    var c = [(v[0][0] + v[1][0] + v[2][0]) / 3, (v[0][1] + v[1][1] + v[2][1]) / 3];
    var rc = Math.sqrt(c[0] * c[0] + c[1] * c[1]);
    var along = (n[0] * c[0] + n[1] * c[1]) / rc * cn[0] / cl + n[2] * cn[1] / cl;
    if (h.every(function (d) { return d > -0.008 && d < 0.0015; }) && Math.abs(along) > 0.995) return;
    if (h.some(function (d) { return d < -0.008; })) below++;
    ringFaces.push(v.map(function (q) { return [q[0], q[1], q[2]]; }));
});
print("symbol ring: detail " + ringFaces.length + ", reaching below its cone " + below);

// The top chevron and its lights, with how each corner moves
var moves = {};
Object.keys(objects).forEach(function (id) {
    var o = objects[id];
    if (o.name != "Geometry" || !child(o, "Indexes")) return;
    var ix = child(o, "Indexes").props[0], d = child(o, "Vertices").props[0];
    for (var i = 0; i < ix.length; i++) moves[ix[i]] = [d[3 * i], d[3 * i + 1], d[3 * i + 2]];
});
// Into the gate's frame: as the model's own transform, without moving, then turned as in mwcore
var base = Object.keys(objects).filter(function (id) { return objects[id].name == "Model" && cleanName(objects[id]) == "Base"; })[0];
var bm = modelMatrix(base);
var kinds = {}, kindList = [];
function moveOf(vertex) {
    var d = moves[vertex];
    if (!d) return 0;
    var x = (bm[0] * d[0] + bm[1] * d[1] + bm[2] * d[2]) / 100, y = (bm[4] * d[0] + bm[5] * d[1] + bm[6] * d[2]) / 100,
        z = (bm[8] * d[0] + bm[9] * d[1] + bm[10] * d[2]) / 100;
    var key = [-x, y, -z].map(function (q) { return q.toFixed(4); }).join(" ");
    if (!kinds[key]) { kindList.push(key); kinds[key] = kindList.length; }
    return kinds[key];
}
var chevronFaces = [], lightFaces = [];
function topChevron(v) {
    var c = [(v[0][0] + v[1][0] + v[2][0]) / 3, (v[0][1] + v[1][1] + v[2][1]) / 3];
    return Math.abs(angleOf(c) - 90) < 15;
}
parts.chevrons.forEach(function (v) {
    if (topChevron(v)) chevronFaces.push(v.map(function (q, e) { return [q[0], q[1], q[2], moveOf(v.vertex[e])]; }));
});
parts.lights.forEach(function (v) {
    if (topChevron(v)) lightFaces.push(v.map(function (q, e) { return [q[0], q[1], q[2], moveOf(v.vertex[e])]; }));
});
kindList.forEach(function (k, i) { out.push("m " + (i + 1) + " " + k); });
print("chevron faces " + chevronFaces.length + ", lights " + lightFaces.length + ", movements " + kindList.join(" | "));

// The lower part of the chevron's light: the front of its triangle (the part of the light that
// rises as it locks), from its point up to where it is widest; above that it slopes back over
// the ring's outer rim. As on the show, it only lights as the top chevron locks a symbol.
var rising = kindList.map(function (k) { return +k.split(" ")[1] > 0; }).indexOf(true) + 1;
var front = -1e9, widest = 0, widestY = 0;
lightFaces.forEach(function (f) { f.forEach(function (q) { if (q[3] == rising) front = Math.max(front, q[2]); }); });
lightFaces.forEach(function (f) {
    f.forEach(function (q) {
        if (q[3] == rising && q[2] > front - 1e-3 && Math.abs(q[0]) > widest) { widest = Math.abs(q[0]); widestY = q[1]; }
    });
});
var front = lightFaces.filter(function (f) {
    return f.every(function (q) { return q[3] == rising && q[1] <= widestY + 1e-3; });
});
lightFaces = lightFaces.filter(function (f) { return front.indexOf(f) < 0; });
// Of that, a locked chevron lights all down to where the slatted lights begin; only the part
// below waits for the top chevron's lock
var slatsTop = -1e9;
lightFaces.forEach(function (f) { f.forEach(function (q) { if (q[3] != rising) slatsTop = Math.max(slatsTop, q[1]); }); });
// The part of a polygon above (sign 1) or below (-1) the given height
function clipY(poly, y, sign) {
    var res = [];
    for (var i = 0; i < poly.length; i++) {
        var p = poly[i], q = poly[(i + 1) % poly.length], dp = (p[1] - y) * sign, dq = (q[1] - y) * sign;
        if (dp >= 0) res.push(p);
        if ((dp >= 0) != (dq >= 0)) {
            var t = dp / (dp - dq);
            res.push(p.map(function (v, e) { return e == 3 ? v : v + (q[e] - v) * t; }));
        }
    }
    return res;
}
var lowerFaces = [];
front.forEach(function (f) {
    var above = clipY(f, slatsTop, 1), below = clipY(f, slatsTop, -1);
    if (above.length >= 3) lightFaces.push(above);
    if (below.length >= 3) lowerFaces.push(below);
});
print("light's triangle lit as locked down to " + slatsTop.toFixed(4) + "; lower part " + lowerFaces.length + " faces");

// Merging faces in one plane that share edges into polygons
function key(q) { return Math.round(q[0] * 2e4) + "," + Math.round(q[1] * 2e4) + "," + Math.round(q[2] * 2e4) + (q.length > 3 ? "," + q[3] : ""); }
function polyNormal(p) {
    var nx = 0, ny = 0, nz = 0;
    for (var i = 0; i < p.length; i++) {
        var a = p[i], b = p[(i + 1) % p.length];
        nx += (a[1] - b[1]) * (a[2] + b[2]); ny += (a[2] - b[2]) * (a[0] + b[0]); nz += (a[0] - b[0]) * (a[1] + b[1]);
    }
    var l = Math.sqrt(nx * nx + ny * ny + nz * nz);
    return l < 1e-14 ? null : [nx / l, ny / l, nz / l];
}
// Splits a polygon into triangles by cutting off ears, flat in the plane it most faces; null if
// it can't be done cleanly. The model does the same (MilkyWayGateModel.triangulate).
function triangulate(p, n) {
    var ax = Math.abs(n[0]), ay = Math.abs(n[1]), az = Math.abs(n[2]);
    var u = az >= ax && az >= ay ? 0 : ax >= ay ? 1 : 0, w = az >= ax && az >= ay ? 1 : 2;
    var flip = (az >= ax && az >= ay ? n[2] : ax >= ay ? n[0] : -n[1]) < 0;
    var P = p.map(function (q) { return [q[u], flip ? -q[w] : q[w]]; });
    var v = [], tris = [];
    for (var i = 0; i < P.length; i++) v.push(i);
    var guard = 0;
    while (v.length > 3 && guard++ < 10 * P.length * P.length) {
        var cutEar = false;
        for (var i = 0; i < v.length; i++) {
            var i0 = v[(i + v.length - 1) % v.length], i1 = v[i], i2 = v[(i + 1) % v.length];
            var cr = (P[i1][0] - P[i0][0]) * (P[i2][1] - P[i0][1]) - (P[i1][1] - P[i0][1]) * (P[i2][0] - P[i0][0]);
            if (cr <= 1e-14) continue;
            var empty = true;
            for (var j = 0; j < v.length; j++) {
                var q = v[j];
                if (q == i0 || q == i1 || q == i2) continue;
                var d1 = side(P[q], P[i0], P[i1]), d2 = side(P[q], P[i1], P[i2]), d3 = side(P[q], P[i2], P[i0]);
                if (!((d1 < 0 || d2 < 0 || d3 < 0) && (d1 > 0 || d2 > 0 || d3 > 0))) { empty = false; break; }
            }
            if (empty) { tris.push([i0, i1, i2]); v.splice(i, 1); cutEar = true; break; }
        }
        if (!cutEar) return null;
    }
    tris.push([v[0], v[1], v[2]]);
    return tris;
}
function side(q, a, b) { return (q[0] - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (q[1] - b[1]); }
function area3(a, b, c) {
    var ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
    var x = uy * wz - uz * wy, y = uz * wx - ux * wz, z = ux * wy - uy * wx;
    return Math.sqrt(x * x + y * y + z * z) / 2;
}

function merge(polys) {
    var faces = [];
    polys.forEach(function (p) {
        var n = polyNormal(p);
        if (n) faces.push({poly: p, n: n, d: n[0] * p[0][0] + n[1] * p[0][1] + n[2] * p[0][2]});
    });
    var edges = {};
    faces.forEach(function (f, i) {
        for (var e = 0; e < f.poly.length; e++) {
            var a = key(f.poly[e]), b = key(f.poly[(e + 1) % f.poly.length]);
            if (a == b) continue;
            var ek = a < b ? a + "|" + b : b + "|" + a;
            (edges[ek] = edges[ek] || []).push(i);
        }
    });
    var group = new Array(faces.length), res = [], merged = 0, kept = 0;
    for (var i = 0; i < faces.length; i++) {
        if (group[i] !== undefined) continue;
        var stack = [i], members = [];
        group[i] = i;
        while (stack.length) {
            var c = stack.pop(); members.push(c);
            var f = faces[c];
            for (var e = 0; e < f.poly.length; e++) {
                var a = key(f.poly[e]), b = key(f.poly[(e + 1) % f.poly.length]);
                if (a == b) continue;
                (edges[a < b ? a + "|" + b : b + "|" + a] || []).forEach(function (o) {
                    if (group[o] !== undefined) return;
                    var n1 = faces[o].n, n0 = faces[i].n;
                    if (n1[0] * n0[0] + n1[1] * n0[1] + n1[2] * n0[2] > 0.99995 && Math.abs(faces[o].d - faces[i].d) < 0.0002) {
                        group[o] = i; stack.push(o);
                    }
                });
            }
        }
        var ok = false;
        if (members.length > 1) {
            // Its outline: the edges used once, followed round, going the same way as its faces
            var count = {}, from = {};
            members.forEach(function (m) {
                var p = faces[m].poly;
                for (var e = 0; e < p.length; e++) {
                    var ka = key(p[e]), kb = key(p[(e + 1) % p.length]);
                    if (ka == kb) continue;
                    var ek = ka < kb ? ka + "|" + kb : kb + "|" + ka;
                    count[ek] = (count[ek] || 0) + 1;
                    from[ek] = [p[e], p[(e + 1) % p.length]];
                }
            });
            var next = {}, n = 0, bad = false;
            Object.keys(count).forEach(function (ek) {
                if (count[ek] > 2) bad = true;
                if (count[ek] == 1) { var s = key(from[ek][0]); if (next[s]) bad = true; next[s] = from[ek]; n++; }
            });
            var start = Object.keys(next)[0], loop = [], cur = start, guard = 0;
            while (!bad && next[cur] && guard++ <= n) {
                loop.push(next[cur][0]); cur = key(next[cur][1]);
                if (cur == start) break;
            }
            if (!bad && cur == start && loop.length == n) {
                // Without corners in the middle of straight edges
                var keep = [];
                for (var j = 0; j < loop.length; j++) {
                    var a = loop[(j + loop.length - 1) % loop.length], b = loop[j], c = loop[(j + 1) % loop.length];
                    var ab = [b[0] - a[0], b[1] - a[1], b[2] - a[2]], bc = [c[0] - b[0], c[1] - b[1], c[2] - b[2]];
                    var cr = [ab[1] * bc[2] - ab[2] * bc[1], ab[2] * bc[0] - ab[0] * bc[2], ab[0] * bc[1] - ab[1] * bc[0]];
                    var lab = Math.sqrt(ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2]), lbc = Math.sqrt(bc[0] * bc[0] + bc[1] * bc[1] + bc[2] * bc[2]);
                    if (lab < 1e-7 || lbc < 1e-7) continue;
                    if (Math.sqrt(cr[0] * cr[0] + cr[1] * cr[1] + cr[2] * cr[2]) / (lab * lbc) > 1e-4 || b.length > 3 && (b[3] != a[3] || b[3] != c[3])) keep.push(b);
                }
                var nn = keep.length >= 3 && polyNormal(keep);
                var tris = nn && nn[0] * faces[i].n[0] + nn[1] * faces[i].n[1] + nn[2] * faces[i].n[2] > 0.99 && triangulate(keep, nn);
                if (tris) {
                    var want = 0, got = 0;
                    members.forEach(function (m) { var p = faces[m].poly; for (var t = 1; t + 1 < p.length; t++) want += area3(p[0], p[t], p[t + 1]); });
                    tris.forEach(function (t) { got += area3(keep[t[0]], keep[t[1]], keep[t[2]]); });
                    if (Math.abs(got - want) < 1e-4 * Math.max(want, 1e-6) + 1e-9) {
                        res.push(keep); ok = true; merged += members.length;
                    }
                }
            }
        }
        if (!ok) members.forEach(function (m) { res.push(faces[m].poly); kept++; });
    }
    print("  " + polys.length + " faces into " + res.length + " polygons (" + merged + " merged, " + kept + " as they were)");
    return res;
}

function write(tag, polys) {
    merge(polys).forEach(function (p) {
        out.push(tag + " " + p.map(function (q) {
            return q.slice(0, 3).map(function (x) { return x.toFixed(4); }).join(" ") + (q.length > 3 ? " " + q[3] : "");
        }).join(" "));
    });
}
write("s", staticFaces);
write("r", ringFaces);
write("v", chevronFaces);
write("l", lightFaces);
write("t", lowerFaces);
Files.write(Paths.get(outFile), (out.join("\n") + "\n").getBytes("UTF-8"));
print("wrote " + out.length + " lines");
