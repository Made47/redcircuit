package pl.geesio.redcircuit.wire;

import net.minecraft.core.Direction;

/**
 * Niezmienny zapis połączeń przewodu: dla każdej ściany (6) i kierunku (6) zasięg ramienia.
 * 3 bity na wpis, 36 wpisów -> dwa longi.
 */
public record WireConnections(long lo, long hi) {
    public static final int NONE = 0;
    /** Ramię do krawędzi bloku. */
    public static final int EDGE = 1;
    /** Ramię skrócone o grubość przewodu (narożnik wewnętrzny). */
    public static final int SHORT = 2;
    /** Ramię wydłużone za krawędź (narożnik zewnętrzny). */
    public static final int EXTENDED = 3;
    /** Ramię do krawędzi + pionowy łącznik do przewodu w obudowie (biegnącego środkiem bloku). */
    public static final int RISER = 4;

    public static final WireConnections EMPTY = new WireConnections(0, 0);

    private static int index(Direction side, Direction dir) {
        return side.get3DDataValue() * 6 + dir.get3DDataValue();
    }

    public int get(Direction side, Direction dir) {
        int i = index(side, dir) * 3;
        long v = i < 64 ? (lo >>> i) : (hi >>> (i - 64));
        // Wpis może przechodzić przez granicę longów (bit 63/64).
        if (i < 64 && i + 3 > 64) {
            v |= hi << (64 - i);
        }
        return (int) (v & 7);
    }

    public static final class Builder {
        private long lo, hi;

        public void set(Direction side, Direction dir, int value) {
            int i = index(side, dir) * 3;
            for (int b = 0; b < 3; b++) {
                int bit = i + b;
                if (((value >> b) & 1) == 0) continue;
                if (bit < 64) lo |= 1L << bit;
                else hi |= 1L << (bit - 64);
            }
        }

        public WireConnections build() {
            return new WireConnections(lo, hi);
        }
    }
}
