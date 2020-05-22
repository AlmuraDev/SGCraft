//------------------------------------------------------------------------------------------------
//
//   SG Craft - Conversions between coordinates and  stargate addresses
//
//------------------------------------------------------------------------------------------------
//
//   New Addressing Scheme
//   ---------------------
//
//   There are 36 symbols. An address consists of 7 or 9 symbols:
//
//      CCCCCCC
//
//   or
//
//      CCCCCCCDD
//
//   where DD encodes the dimension and CCCCCCC encode the coordinates within
//   the dimension. When connecting to a gate in the same dimension, DD may
//   be omitted, allowing a 7-chevron gate to be used.
//
//   The coordinate range is +/- sqrt(36**7))/2-1 = 139,967 chunks = 2,239,472 blocks
//
//   The dimension range is -36**2/2 to 36**2/2-1 = -648 to 647
//

package gcewing.sg.util;

import gcewing.sg.BaseTileEntity;
import gcewing.sg.BaseUtils;
import gcewing.sg.tileentity.SGBaseTE;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

public class SGAddressing {

    static boolean debugAddressing = false;

    public static class AddressingError extends Exception {
        AddressingError(String s) {super(s);}
    }

    public final static String symbolChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public final static int numSymbols = symbolChars.length();
    public final static int numCoordSymbols = 7;
    public final static int numDimensionSymbols = 2;
    
    public final static int maxAddressLength = numCoordSymbols + numDimensionSymbols;
    public final static int maxCoord = 139967;
    public final static int minCoord = -maxCoord;
    public final static int coordRange = maxCoord - minCoord + 1;
    public final static int minDirectDimension = -648;
//     public final static int minDimension = -648;
//     public final static int maxDimension = 647;
//     public final static int dimensionRange = maxDimension - minDimension + 1;
    public final static int maxDimensionIndex = 1295;
    public final static int dimensionRange = maxDimensionIndex + 1;
    final static String padding = "---------";
    final static long mc = coordRange + 2; // == 2 * maxCoord + 3;
    final static long pc = 93563;   //  (pc * qc) % mc == 1
    final static long qc = 153742;
//     final static long md = dimensionRange + 2;
    final static long md = dimensionRange + 1;
    final static long pd = 953;  //  (pd * qd) % md == 1
    final static long qd = 788;
     // Historical error
    final static long mdOld = dimensionRange + 2;
    final static long qdOld = 459;
    
    protected static boolean isValidSymbolChar(char c) {
        return isValidSymbolChar(String.valueOf(c));
    }

    public static boolean isValidSymbolChar(String c) {
        return symbolChars.indexOf(c) >= 0;
    }

    public static char symbolToChar(int i) {
        return symbolChars.charAt(i);
    }

    public static int charToSymbol(char c) {
        return charToSymbol(String.valueOf(c));
    }

    public static int charToSymbol(String c) {
        return symbolChars.indexOf(c);
    }

    public static boolean validSymbols(String s) {
        for (int i = 0; i < s.length(); i++)
            if (charToSymbol(s.charAt(i)) < 0)
                return false;
        return true;
    }

    public static void validateAddress(String s) throws AddressingError {
        int l = s.length();
        if ((l ==  numCoordSymbols || l == numCoordSymbols + numDimensionSymbols)
            && validSymbols(s))
                return;
        throw new AddressingError("malformedAddress");
    }
    
    public static String normalizeAddress(String address) {
        return address.replace("-", "").toUpperCase();
    }
    
//     public static String relativeAddress(String targetAddress, String contextAddress)
//         throws AddressingError
//     {
//         validateAddress(targetAddress);
//         if (addressesInSameDimension(targetAddress, contextAddress))
//             return coordSymbolsOf(targetAddress);
//         else
//             return targetAddress;
//     }
    
//     public static String normalizedRelativeAddress(String targetAddress, String contextAddress)
//         throws AddressingError
//     {
//         return relativeAddress(normalizeAddress(targetAddress), contextAddress);
//     }
    

    public static String coordSymbolsOf(String address) {
        return address.substring(0, numCoordSymbols);
    }
    
    public static int dimensionSymbolsOf(String address) {
        int dimensionID = -999;
        try {
            validateAddress(address);
        } catch (AddressingError addressingError) {
            addressingError.printStackTrace();
        }
        String csyms = address.substring(0, numCoordSymbols);
        long c = longFromSymbols(csyms);
        if (address.length() == maxAddressLength) {
            String dsyms = address.substring(numCoordSymbols);
            int d = intFromSymbols(dsyms);
            int dp = hash(d, qd, md);
            dimensionID = unpermuteDimension(c, dp);
        }
        return dimensionID;
    }

    public static boolean inSameDimension(String origin, String destination) {
        int originDimension = dimensionSymbolsOf(origin);
        int destinationDimension = dimensionSymbolsOf(destination);
        if (originDimension == destinationDimension) {
            return true;
        } else {
            return false;
        }
    }

    public static String addressForLocation(SGLocation loc) throws AddressingError {
        if (debugAddressing)
            System.out.printf("SGAddressing.addressForLocation: coord range = %d to %d\n",
                minCoord, maxCoord);
        int chunkx = loc.pos.getX() >> 4;
        int chunkz = loc.pos.getZ() >> 4;
        if (!inCoordRange(chunkx) || !inCoordRange(chunkz))
            throw new AddressingError("targetOutOfRange");
//         if (!inDimensionRange(loc.dimension))
//             throw dimensionRangeError;
        Integer di = SGDimensionMap.indexForDimension(loc.dimension);
        if (di > maxDimensionIndex)
            throw new AddressingError("dimensionTooFar");
        long c = interleaveCoords(hash(chunkx - minCoord, pc, mc), hash(chunkz - minCoord, pc, mc));
//         int d = hash(loc.dimension - minDimension, pd, md);
        int dp = permuteDimension(c, di);
        int d = hash(dp, pd, md);
        if (debugAddressing)
            System.out.printf(
                "SGAddressing.addressForLocation: chunk (%d,%d) in dimension %d gives c = %s d = %d\n",
                chunkx, chunkz, loc.dimension, c, d);
        return longToSymbols(c, numCoordSymbols) + intToSymbols(d, numDimensionSymbols);
    }
    
    public static SGBaseTE findAddressedStargate(String address, World fromWorld, boolean pending) throws AddressingError {
        if (debugAddressing)
            System.out.printf("SGAddressing.findAddressedStargate: %s\n", address);
        validateAddress(address);
        String csyms = address.substring(0, numCoordSymbols);
        long c = longFromSymbols(csyms);
        int[] xz = uninterleaveCoords(c);
        int chunkX = minCoord + hash(xz[0], qc, mc);
        int chunkZ = minCoord + hash(xz[1], qc, mc);
        if (debugAddressing)
            System.out.printf("SGAddressing.findAddressedStargate: c = %s chunk = (%d,%d)\n",
                c, chunkX, chunkZ);
        SGBaseTE te = null;
        if (address.length() == maxAddressLength) {
            // Absolute address
            String dsyms = address.substring(numCoordSymbols);
            int d = intFromSymbols(dsyms);
            int dp = hash(d, qd, md);
            int di = unpermuteDimension(c, dp);
            Integer dm = SGDimensionMap.dimensionForIndex(di);
            if (debugAddressing)
                System.out.printf("SGAddressing.findAddressedStargate: d = %s dimension = %s\n",
                    d, dm);
            if (dm != null)
                te = getBaseTE(chunkX, chunkZ, dm, pending);
            if (te == null) {
                // Try old interpretation of dimension symbols
                int dimOld = minDirectDimension + hash(d, qdOld, mdOld);
                if (debugAddressing)
                    System.out.printf("SGAddressing.findAddressedStargate: Trying dimension = %s\n",
                        dimOld);
                te = getBaseTE(chunkX, chunkZ, dimOld, pending);
            }
        }
        else {
            // Relative address
            int dimension = fromWorld.provider.getDimension();
            te = getBaseTE(chunkX, chunkZ, dimension, pending);
        }
        return te;
    }
    
    protected static SGBaseTE getBaseTE(int chunkX, int chunkZ, int dimension, boolean pending) {
        World toWorld = getWorld(dimension);
        if (toWorld != null) {
            Chunk chunk = toWorld.getChunk(chunkX, chunkZ);
            if (chunk != null)
                for (Object te : chunk.getTileEntityMap().values()) {
                    if (te instanceof SGBaseTE) {
                        ((SGBaseTE) te).isPending = pending;
                        return (SGBaseTE) te;
                    }
                }
        }
        return null;
    }
    
    protected static int permuteDimension(long c, int d) {
        return (int)((d + c) % dimensionRange);
    }
    
    protected static int unpermuteDimension(long c, int d) {
        int i = (int)((d - c) % dimensionRange);
        if (i < 0)
            i += dimensionRange;
        return i;
    }
    
    protected static long interleaveCoords(int x, int z) {
        if (debugAddressing)
            System.out.printf("SGAddressing.interleaveCoords: %d, %d\n", x, z);
        long p6 = 1;
        long c = 0;
        while (x > 0 || z > 0) {
            if (debugAddressing)
                System.out.printf("SGAddressing.interleaveCoords: half-digits %d %d\n", x % 6, z % 6);
            c += p6 * (x % 6); x /= 6; p6 *= 6;
            c += p6 * (z % 6); z /= 6; p6 *= 6;
        }
        return c;
    }
    
    protected static int[] uninterleaveCoords(long c) {
        int p6 = 1;
        int[] xy = {0, 0};
        while (c > 0) {
            xy[0] += p6 * (c % 6); c /= 6;
            xy[1] += p6 * (c % 6); c /= 6;
            p6 *= 6;
        }
        return xy;
    }
    
    protected static int hash(int i, long f, long m) {
        int h = (int)(((i + 1) * f) % m) - 1;
        if (debugAddressing)
            System.out.printf("SGAddressing.hash(%s, %s, %s) = %s\n", i, f, m, h);
        return h;
    }

    @Nullable
    public static WorldServer getWorld(int dimension) {
        return BaseUtils.getWorldForDimension(dimension);
    }
    
    protected static boolean inCoordRange(int i) {
        return i >= minCoord && i <= maxCoord;
    }
    
//     protected static boolean inDimensionRange(int i) {
//         return i >= minDimension && i <= maxDimension;
//     }

    protected static String intToSymbols(int i, int n) {
        return longToSymbols(i, n);
    }

    protected static String longToSymbols(long i, int n) {
        String s = "";
        while (n-- > 0) {
            s = symbolToChar((int)(i % numSymbols)) + s;
            i /= numSymbols;
        }
        return s;
    }
    
    protected static int intFromSymbols(String s) {
        return (int)longFromSymbols(s);
    }
    
    protected static long longFromSymbols(String s) {
        long i = 0;
        int n = s.length();
        for (int j = 0; j < n; j++) {
            char c = s.charAt(j);
            i = i * numSymbols + charToSymbol(c);
        }
        return i;
    }
    
    public static String padAddress(String address, String caret, int maxLength) {
        if (maxLength < numCoordSymbols)
            maxLength = numCoordSymbols;
        return formatAddress(address + padding.substring(address.length(), maxLength), " ", " ");
    }
    
    public static String formatAddress(String address, String sep1, String sep2) {
        String coord = address.substring(0, numCoordSymbols);
        String dimen = address.substring(numCoordSymbols);
        int i = (numCoordSymbols + 1) / 2;
        String result = coord.substring(0, i) + sep1 + coord.substring(i);
        if (dimen.length() > 0)
            result += sep2 + dimen;
        return result;
    }
    
    public static String localAddress(String address) {
        return address.substring(0, numCoordSymbols);
    }
        
}
