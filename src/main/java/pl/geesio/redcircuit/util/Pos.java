package pl.geesio.redcircuit.util;

import net.minecraftforge.common.util.ForgeDirection;

/** Niezmienna pozycja bloku (1.7.10 nie ma BlockPos). */
public final class Pos {
    public final int x, y, z;

    public Pos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Pos offset(ForgeDirection d) {
        return new Pos(x + d.offsetX, y + d.offsetY, z + d.offsetZ);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pos p && p.x == x && p.y == y && p.z == z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
