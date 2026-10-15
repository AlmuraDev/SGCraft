// Draws cross sections of the Milky Way gate reference model, through its axis at the given angles
// (degrees anticlockwise from the right, seen from in front), one color each, to look at its
// profile. r runs left to right and z (front up) bottom to top.
// jjs section.js -- <scratch> <fbx> <out.png> <part> <r0> <r1> <angle>...
var file = arguments[1];
load(arguments[0] + "/fbxcore.js");
load(arguments[0] + "/mwcore.js");
var outFile = arguments[2], part = arguments[3], r0 = +arguments[4], r1 = +arguments[5];
var W = 1600, scale = W / (r1 - r0), H = Math.round(1.1 * scale);
var img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB), g = img.createGraphics();
var colors = [0xffffff, 0xff6060, 0x60ff60, 0x6080ff, 0xffff60, 0xff60ff, 0x60ffff];
g.setColor(new java.awt.Color(0x404040));
g.drawLine(0, H / 2, W, H / 2);
var tris = part == "all" ? parts.static.concat(parts.chevrons, parts.lights, parts.ring) : parts[part];
for (var i = 6; i < arguments.length; i++) {
    g.setColor(new java.awt.Color(colors[(i - 6) % colors.length]));
    slice(tris, +arguments[i]).forEach(function (s) {
        g.drawLine(Math.round((s[0][0] - r0) * scale), Math.round(H / 2 - s[0][1] * scale),
            Math.round((s[1][0] - r0) * scale), Math.round(H / 2 - s[1][1] * scale));
    });
}
ImageIO.write(img, "png", new File(outFile));
