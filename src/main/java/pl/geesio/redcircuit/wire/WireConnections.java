package pl.geesio.redcircuit.wire;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Zapis połączeń przewodu: dla każdej ściany (6) i kierunku (6) zasięg ramienia.
 * 3 bity na wpis, 36 wpisów -> dwa longi.
 */
public final class WireConnections {
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

    private final long lo, hi;

    private WireConnections(long lo, long hi) {
        this.lo = lo;
        this.hi = hi;
    }

    private static int index(ForgeDirection side, ForgeDirection dir) {
        return side.ordinal() * 6 + dir.ordinal();
    }

    public int get(ForgeDirection side, ForgeDirection dir) {
        int i = index(side, dir) * 3;
        long v = i < 64 ? (lo >>> i) : (hi >>> (i - 64));
        if (i < 64 && i + 3 > 64) v |= hi << (64 - i);
        return (int) (v & 7);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WireConnections c && c.lo == lo && c.hi == hi;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(lo) * 31 + Long.hashCode(hi);
    }

    public static final class Builder {
        private long lo, hi;

        public Builder set(ForgeDirection side, ForgeDirection dir, int value) {
            int i = index(side, dir) * 3;
            for (int b = 0; b < 3; b++) {
                int bit = i + b;
                if (((value >> b) & 1) == 0) continue;
                if (bit < 64) lo |= 1L << bit;
                else hi |= 1L << (bit - 64);
            }
            return this;
        }

        public WireConnections build() {
            return new WireConnections(lo, hi);
        }
    }
}
