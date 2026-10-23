package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

/**
 * Wspólna logika połączeń przewodu leżącego na ścianie - działa zarówno dla bloków {@link FaceWireBlock},
 * jak i dla przewodów-części CB Multipart.
 */
public final class WireLinks {
    private WireLinks() {}

    /**
     * Wynik połączenia.
     *
     * @param pos  pozycja przewodu po drugiej stronie (null dla NONE/DEVICE)
     * @param face ściana przewodu-części po drugiej stronie, albo -1, gdy to blok-przewód
     */
    public record Target(FaceWireBlock.Link link, @Nullable BlockPos pos, int face) {
        static final Target NONE = new Target(FaceWireBlock.Link.NONE, null, -1);
        static final Target DEVICE = new Target(FaceWireBlock.Link.DEVICE, null, -1);
    }

    /** Test połączenia z urządzeniem (bramka, lampa, maszyna...). */
    public interface DeviceTest {
        boolean test(BlockGetter level, BlockPos np, BlockState ns);
    }

    /**
     * @param selfType     rodzaj tego przewodu ({@link WireTypes})
     * @param internal     czy w tym samym bloku leży zgodny przewód na danej ścianie (indeks ściany)
     * @param internalFace -1 dla bloków (ten sam węzeł), indeks ściany dla części
     */
    public static Target link(BlockGetter level, BlockPos pos, int selfType, Direction side, Direction dir,
                              IntPredicate internal, boolean selfIsPart, @Nullable DeviceTest device) {
        if (internal.test(dir.get3DDataValue())) {
            return new Target(FaceWireBlock.Link.INTERNAL, pos, selfIsPart ? dir.get3DDataValue() : -1);
        }

        BlockPos np = pos.relative(dir);
        BlockState ns = level.getBlockState(np);
        if (ns.getBlock() instanceof EncasedWireBlock encased) {
            return WireTypes.compatible(selfType, encased.wireType())
                    ? new Target(FaceWireBlock.Link.ENCASED, np, -1) : Target.NONE;
        }
        Integer nt = WireTypes.typeOf(ns.getBlock());
        if (ns.getBlock() instanceof FaceWireBlock && nt != null && WireTypes.compatible(selfType, nt)
                && FaceWireBlock.hasFace(ns, side)) {
            return new Target(FaceWireBlock.Link.STRAIGHT, np, -1);
        }
        Integer pt = PartWires.wireAt(level, np, side);
        if (pt != null && WireTypes.compatible(selfType, pt)) {
            return new Target(FaceWireBlock.Link.STRAIGHT, np, side.get3DDataValue());
        }

        // Narożnik zewnętrzny: przestrzeń obok jest wolna, a przewód leży na bocznej ścianie naszego podłoża.
        boolean open;
        if (ns.getBlock() instanceof FaceWireBlock) open = !FaceWireBlock.hasFace(ns, side);
        else if (PartWires.isHost(level, np)) open = pt == null && PartWires.wireAt(level, np, side) == null
                && ns.getCollisionShape(level, np).isEmpty();
        else open = ns.getCollisionShape(level, np).isEmpty();
        if (open) {
            BlockPos cp = np.relative(side);
            BlockState cs = level.getBlockState(cp);
            Integer ct = WireTypes.typeOf(cs.getBlock());
            if (cs.getBlock() instanceof FaceWireBlock && ct != null && WireTypes.compatible(selfType, ct)
                    && FaceWireBlock.hasFace(cs, dir.getOpposite())) {
                return new Target(FaceWireBlock.Link.CORNER, cp, -1);
            }
            Integer cpt = PartWires.wireAt(level, cp, dir.getOpposite());
            if (cpt != null && WireTypes.compatible(selfType, cpt)) {
                return new Target(FaceWireBlock.Link.CORNER, cp, dir.getOpposite().get3DDataValue());
            }
        }

        if (!(ns.getBlock() instanceof FaceWireBlock) && pt == null && device != null && device.test(level, np, ns)) {
            return Target.DEVICE;
        }
        return Target.NONE;
    }

    /** Zasięg ramienia w modelu dla danego połączenia. */
    public static int reach(FaceWireBlock.Link l, Direction side, Direction dir) {
        return switch (l) {
            case NONE -> WireConnections.NONE;
            case STRAIGHT, DEVICE -> WireConnections.EDGE;
            case ENCASED -> WireConnections.RISER;
            // Narożnik wewnętrzny: jedna ze ścian dociąga do krawędzi, druga się skraca.
            case INTERNAL -> side.get3DDataValue() < dir.get3DDataValue() ? WireConnections.EDGE : WireConnections.SHORT;
            // Narożnik zewnętrzny: jeden z przewodów zawija się za krawędź.
            case CORNER -> side.get3DDataValue() < dir.getOpposite().get3DDataValue() ? WireConnections.EXTENDED : WireConnections.EDGE;
        };
    }
}
