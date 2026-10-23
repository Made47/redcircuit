package pl.geesio.redcircuit.wire;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.energy.PowerWireBlock;

/**
 * Wspólny opis rodzaju przewodu - zarówno przyklejanego do ściany, jak i w obudowie (encased):
 * -1 = stop sygnałowy, 0..15 = przewód izolowany danego koloru, 16 = kabel wiązkowy, 17 = stop energetyczny.
 */
public final class WireTypes {
    private WireTypes() {}

    public static final int RED = -1, BUNDLED = 16, POWER = 17;

    @Nullable
    public static Integer typeOf(Block block) {
        if (block instanceof RedWireBlock w) return w.getColor();
        if (block instanceof BundledCableBlock) return BUNDLED;
        if (block instanceof PowerWireBlock) return POWER;
        if (block instanceof EncasedWireBlock j) return j.wireType();
        return null;
    }

    /** Przewód sygnałowy (czerwony lub izolowany) - jeden węzeł sieci. */
    public static boolean isSignal(@Nullable Integer t) {
        return t != null && t >= RED && t < BUNDLED;
    }

    /** Dowolny przewód sieci redstone (sygnałowy albo wiązkowy). */
    public static boolean isRedstone(@Nullable Integer t) {
        return t != null && t >= RED && t <= BUNDLED;
    }

    public static boolean compatible(int a, int b) {
        if (a == POWER || b == POWER) return a == b;
        if (a == BUNDLED && b == BUNDLED) return true;
        if (a == BUNDLED) return b >= 0;
        if (b == BUNDLED) return a >= 0;
        return a < 0 || b < 0 || a == b;
    }
}
