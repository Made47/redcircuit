package pl.geesio.redcircuit.wire;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

/**
 * Rodzaj przewodu w danym miejscu (przyklejanego do ściany albo w obudowie):
 * -1 = stop sygnałowy, 0..15 = przewód izolowany danego koloru, 16 = kabel wiązkowy, 17 = przewód energetyczny.
 */
public final class WireTypes {
    private WireTypes() {}

    public static final int RED = -1, BUNDLED = 16;
    /** Przewód ze stopu energetycznego (energia RF, bez sygnału). */
    public static final int POWER = 17;

    /** Rodzaj przewodu w bloku albo null, gdy to nie przewód. */
    public static Integer typeAt(IBlockAccess world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        if (b instanceof IWire w) return w.wireType(world.getBlockMetadata(x, y, z));
        return null;
    }

    /** Przewód sygnałowy (czerwony lub izolowany) - jeden węzeł sieci. */
    public static boolean isSignal(Integer t) {
        return t != null && t >= RED && t < BUNDLED;
    }

    /** Dowolny przewód sieci redstone (sygnałowy albo wiązkowy). */
    public static boolean isRedstone(Integer t) {
        return t != null && t >= RED && t <= BUNDLED;
    }

    public static boolean compatible(int a, int b) {
        if (a == POWER || b == POWER) return a == b;
        if (a == BUNDLED && b == BUNDLED) return true;
        if (a == BUNDLED) return b >= 0;
        if (b == BUNDLED) return a >= 0;
        return a < 0 || b < 0 || a == b;
    }

    /** Blok-przewód (przyklejany albo w obudowie). */
    public interface IWire {
        int wireType(int meta);
    }
}
