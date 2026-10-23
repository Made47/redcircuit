package pl.geesio.redcircuit.wire;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Pos;

import java.util.function.IntPredicate;

/**
 * Logika połączeń przewodu leżącego na ścianie (port z wersji 1.21) - działa zarówno dla bloków
 * {@link BlockFaceWire}, jak i dla przewodów-części ForgeMultipart ({@link PartWires}).
 */
public final class WireLinks {
    private WireLinks() {}

    /** Rodzaj połączenia przewodu na danej ścianie w danym kierunku. */
    public enum Link { NONE, STRAIGHT, INTERNAL, CORNER, DEVICE, ENCASED }

    /**
     * Wynik połączenia: rodzaj, pozycja przewodu po drugiej stronie (null dla NONE/DEVICE)
     * i ściana przewodu-części po drugiej stronie (-1, gdy to blok-przewód).
     */
    public static final class Target {
        public final Link link;
        public final Pos pos;
        public final int face;

        Target(Link link, Pos pos, int face) {
            this.link = link;
            this.pos = pos;
            this.face = face;
        }

        static final Target NONE = new Target(Link.NONE, null, -1);
        static final Target DEVICE = new Target(Link.DEVICE, null, -1);
    }

    /** Test połączenia z urządzeniem (bramka, lampa, maszyna...). */
    public interface DeviceTest {
        boolean test(IBlockAccess world, Pos np, Block nb);
    }

    /** Wersja dla bloków-przewodów: {@code faces} = ściany zajęte w tym bloku przez zgodny przewód. */
    public static Target link(IBlockAccess world, Pos pos, int selfType, ForgeDirection side, ForgeDirection dir,
                              int faces, DeviceTest device) {
        return link(world, pos, selfType, side, dir, f -> (faces & (1 << f)) != 0, false, device);
    }

    /**
     * @param internal   czy w tym samym bloku leży zgodny przewód na danej ścianie (indeks ściany)
     * @param selfIsPart czy pytający przewód jest częścią (wtedy INTERNAL wskazuje ścianę części)
     */
    public static Target link(IBlockAccess world, Pos pos, int selfType, ForgeDirection side, ForgeDirection dir,
                              IntPredicate internal, boolean selfIsPart, DeviceTest device) {
        if (internal.test(dir.ordinal())) return new Target(Link.INTERNAL, pos, selfIsPart ? dir.ordinal() : -1);

        Pos np = pos.offset(dir);
        Block nb = world.getBlock(np.x, np.y, np.z);
        if (nb instanceof BlockEncasedWire encased) {
            return WireTypes.compatible(selfType, encased.wireType(world.getBlockMetadata(np.x, np.y, np.z)))
                    ? new Target(Link.ENCASED, np, -1) : Target.NONE;
        }
        Integer nt = WireTypes.typeAt(world, np.x, np.y, np.z);
        boolean neighborWire = nb instanceof BlockFaceWire;
        if (neighborWire && nt != null && WireTypes.compatible(selfType, nt) && BlockFaceWire.hasFace(world, np, side)) {
            return new Target(Link.STRAIGHT, np, -1);
        }
        boolean host = PartWires.isHost(world, np);
        Integer pt = host ? PartWires.wireAt(world, np, side.ordinal()) : null;
        if (pt != null && WireTypes.compatible(selfType, pt)) return new Target(Link.STRAIGHT, np, side.ordinal());

        // Narożnik zewnętrzny: przestrzeń obok jest wolna, a przewód leży na bocznej ścianie naszego podłoża.
        boolean open;
        if (neighborWire) open = !BlockFaceWire.hasFace(world, np, side);
        else if (host) open = !PartWires.slotTaken(world, np, side.ordinal());
        else open = nb.isAir(world, np.x, np.y, np.z) || !nb.getMaterial().blocksMovement();
        if (open) {
            Pos cp = np.offset(side);
            Block cb = world.getBlock(cp.x, cp.y, cp.z);
            Integer ct = WireTypes.typeAt(world, cp.x, cp.y, cp.z);
            if (cb instanceof BlockFaceWire && ct != null && WireTypes.compatible(selfType, ct)
                    && BlockFaceWire.hasFace(world, cp, dir.getOpposite())) {
                return new Target(Link.CORNER, cp, -1);
            }
            Integer cpt = PartWires.wireAt(world, cp, dir.getOpposite().ordinal());
            if (cpt != null && WireTypes.compatible(selfType, cpt)) return new Target(Link.CORNER, cp, dir.getOpposite().ordinal());
        }

        if (!neighborWire && pt == null && device != null && device.test(world, np, nb)) return Target.DEVICE;
        return Target.NONE;
    }

    /** Zasięg ramienia w modelu dla danego połączenia. */
    public static int reach(Link l, ForgeDirection side, ForgeDirection dir) {
        return switch (l) {
            case NONE -> WireConnections.NONE;
            case STRAIGHT, DEVICE -> WireConnections.EDGE;
            case ENCASED -> WireConnections.RISER;
            // Narożnik wewnętrzny: jedna ze ścian dociąga do krawędzi, druga się skraca.
            case INTERNAL -> side.ordinal() < dir.ordinal() ? WireConnections.EDGE : WireConnections.SHORT;
            // Narożnik zewnętrzny: jeden z przewodów zawija się za krawędź.
            case CORNER -> side.ordinal() < dir.getOpposite().ordinal() ? WireConnections.EXTENDED : WireConnections.EDGE;
        };
    }
}
