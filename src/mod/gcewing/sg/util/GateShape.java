//------------------------------------------------------------------------------------------------
//
//   SG Craft - Size and layout of a stargate multiblock
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.util;

import gcewing.sg.Vector3;

// Everything that depends on how big a gate is should read it from here, so that a gate of a
// different size only needs a different GateShape.
//
// All positions are in the gate's local frame (see SGBaseTE.localToGlobalTransformation): the
// base block is at the origin at the bottom centre of the ring, x runs left to right, y runs up,
// and the event horizon lies in the z = 0 plane.

public final class GateShape {

    // Values in the pattern
    public static final int EMPTY = 0;
    public static final int RING = 1;
    public static final int CHEVRON = 2;

    // How far in front of and behind the event horizon entities are tracked each tick
    public static final double PORTAL_TRACKING_DEPTH = 1.5;

    public static final GateShape STANDARD = new GateShape(
        new int[][] {
            {2, 1, 2, 1, 2},
            {1, 0, 0, 0, 1},
            {2, 0, 0, 0, 2},
            {1, 0, 0, 0, 1},
            {2, 1, 0, 1, 2},
        },
        2.0, 2.25, 2.5, 0.5,
        5, 32,
        new Vector3(-1.5, 0.5, 0.5), new Vector3(1.5, 3.5, 5.5),
        new Vector3(-1.0, 0.5, 0.0), new Vector3(1.0, 2.5, 2.5));

    // Rows from top to bottom; the base block is the centre of the bottom row
    private final int[][] pattern;

    public final int width;
    public final int height;
    public final int halfWidth;

    // Height of the centre of the ring above the base block
    public final double centreY;

    public final double ringInnerRadius;
    public final double ringMidRadius;
    public final double ringOuterRadius;
    public final double ringDepth;

    // Half the width and height of the region in which entities are tracked for teleporting
    public final double apertureHalfSize;
    // An entity only teleports if it ends up no further than this behind the event horizon
    public final double maxCrossingDepth;

    // Event horizon ripple simulation grid
    public final int ehGridRadialSize;
    public final int ehGridPolarSize;
    public final double ehBandWidth;
    public final double ehClipRadius;
    public final double[] ehSin;
    public final double[] ehCos;

    // Region damaged by the opening vortex (kawoosh), and the region of blocks it destroys
    public final Vector3 kawooshDamageMin, kawooshDamageMax;
    public final Vector3 kawooshBlockMin, kawooshBlockMax;

    public GateShape(int[][] pattern,
        double ringInnerRadius, double ringMidRadius, double ringOuterRadius, double ringDepth,
        int ehGridRadialSize, int ehGridPolarSize,
        Vector3 kawooshDamageMin, Vector3 kawooshDamageMax,
        Vector3 kawooshBlockMin, Vector3 kawooshBlockMax)
    {
        this.pattern = pattern;
        this.height = pattern.length;
        this.width = pattern[0].length;
        this.halfWidth = width / 2;
        this.centreY = (height - 1) / 2.0;
        this.ringInnerRadius = ringInnerRadius;
        this.ringMidRadius = ringMidRadius;
        this.ringOuterRadius = ringOuterRadius;
        this.ringDepth = ringDepth;
        this.apertureHalfSize = ringInnerRadius - 0.5;
        this.maxCrossingDepth = height;
        this.ehGridRadialSize = ehGridRadialSize;
        this.ehGridPolarSize = ehGridPolarSize;
        this.ehBandWidth = ringInnerRadius / ehGridRadialSize;
        this.ehClipRadius = ringOuterRadius;
        this.ehSin = new double[ehGridPolarSize + 1];
        this.ehCos = new double[ehGridPolarSize + 1];
        for (int i = 0; i <= ehGridPolarSize; i++) {
            double a = 2 * Math.PI * i / ehGridPolarSize;
            ehSin[i] = Math.sin(a);
            ehCos[i] = Math.cos(a);
        }
        this.kawooshDamageMin = kawooshDamageMin;
        this.kawooshDamageMax = kawooshDamageMax;
        this.kawooshBlockMin = kawooshBlockMin;
        this.kawooshBlockMax = kawooshBlockMax;
    }

    // What should be at local position (x, y) of a vertical gate, where x is in
    // -halfWidth..halfWidth and y is in 0..height-1. The base block itself is at (0, 0).
    public int cellAt(int x, int y) {
        return pattern[height - 1 - y][halfWidth + x];
    }

    // How far to search sideways from a block for blocks belonging to the same gate, in either
    // orientation. A horizontal gate extends height - 1 blocks out from its base.
    public int searchRadius() {
        return Math.max(halfWidth, height - 1) + 1;
    }

    // How far to search up from the base (or down from a ring block) for blocks of the same gate
    public int searchHeight() {
        return height - 1;
    }

}
