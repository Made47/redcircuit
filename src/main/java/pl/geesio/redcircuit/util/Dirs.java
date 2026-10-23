package pl.geesio.redcircuit.util;

import net.minecraftforge.common.util.ForgeDirection;

/** Pomocnicze dla ForgeDirection (kolejność 0-5 taka sama jak Direction w 1.21: D, U, N, S, W, E). */
public final class Dirs {
    private Dirs() {}

    public static final ForgeDirection[] ALL = ForgeDirection.VALID_DIRECTIONS;

    public static ForgeDirection of(int i) {
        return ForgeDirection.getOrientation(i);
    }

    /** Oś: 0 = Y (dół/góra), 1 = Z (północ/południe), 2 = X (zachód/wschód). */
    public static int axis(ForgeDirection d) {
        return d.ordinal() >> 1;
    }

    public static boolean sameAxis(ForgeDirection a, ForgeDirection b) {
        return axis(a) == axis(b);
    }

    /** Indeks współrzędnej w tablicy {x, y, z}. */
    public static int coord(ForgeDirection d) {
        return switch (axis(d)) {
            case 0 -> 1;
            case 1 -> 2;
            default -> 0;
        };
    }

    /** Czy kierunek jest dodatni (góra, południe, wschód). */
    public static boolean positive(ForgeDirection d) {
        return (d.ordinal() & 1) == 1;
    }

    /** Obrót w prawo patrząc z góry: N -> E -> S -> W (jak getClockWise w 1.21). */
    public static ForgeDirection cw(ForgeDirection d) {
        return switch (d) {
            case NORTH -> ForgeDirection.EAST;
            case EAST -> ForgeDirection.SOUTH;
            case SOUTH -> ForgeDirection.WEST;
            case WEST -> ForgeDirection.NORTH;
            default -> d;
        };
    }

    /** Obrót w lewo patrząc z góry: N -> W -> S -> E (jak getCounterClockWise w 1.21). */
    public static ForgeDirection ccw(ForgeDirection d) {
        return switch (d) {
            case NORTH -> ForgeDirection.WEST;
            case WEST -> ForgeDirection.SOUTH;
            case SOUTH -> ForgeDirection.EAST;
            case EAST -> ForgeDirection.NORTH;
            default -> d;
        };
    }

    /** Kierunki poziome. */
    public static final ForgeDirection[] HORIZONTAL = {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST};
}
