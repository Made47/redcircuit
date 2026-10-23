package pl.geesio.redcircuit.wire;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import pl.geesio.redcircuit.util.Pos;

/**
 * Most do przewodów zapisanych jako części ForgeMultipart (w jednym bloku z mikroblokami i innymi częściami).
 * Gdy ForgeMultipart nie jest zainstalowany, {@link #host} jest null i wszystkie metody zwracają "brak przewodu" -
 * reszta kodu działa jak wcześniej.
 */
public final class PartWires {
    private PartWires() {}

    /** Ustawiane przez integrację z ForgeMultipart. */
    public static Host host;

    public interface Host {
        /** Rodzaj przewodu-części leżącego na ścianie {@code face} (0..5) w bloku, albo null. */
        Integer wireAt(IBlockAccess world, int x, int y, int z, int face);

        /** Czy w bloku są części ForgeMultipart. */
        boolean isHost(IBlockAccess world, int x, int y, int z);

        /** Czy slot ściany {@code face} jest zajęty przez dowolną część (np. osłonę). */
        boolean slotTaken(IBlockAccess world, int x, int y, int z, int face);

        /** Zapisany poziom sygnału (kanał ch >= 0 dla kabla wiązkowego, -1 dla zwykłego przewodu). */
        int getSignal(IBlockAccess world, int x, int y, int z, int face, int ch);

        void setSignal(World world, int x, int y, int z, int face, int ch, int value);

        /** Po zmianie sygnału: odświeżenie klienta i powiadomienie sąsiadów. */
        void notifyChanged(World world, int x, int y, int z, int face);
    }

    /** Ustawiane przez integrację z ForgeMultipart: stawianie przewodów jako części. */
    public static Placer placer;

    public interface Placer {
        /** Czy w bloku można postawić przewód-część (blok wieloczęściowy albo przewód do przekształcenia). */
        boolean accepts(IBlockAccess world, int x, int y, int z);

        /** Stawia przewód-część na ścianie {@code face}; na serwerze dodaje go do świata. */
        boolean place(World world, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection face, BlockFaceWire block, int meta);
    }

    public static Integer wireAt(IBlockAccess world, Pos p, int face) {
        Host h = host;
        return h == null ? null : h.wireAt(world, p.x, p.y, p.z, face);
    }

    public static boolean isHost(IBlockAccess world, Pos p) {
        Host h = host;
        return h != null && h.isHost(world, p.x, p.y, p.z);
    }

    public static boolean slotTaken(IBlockAccess world, Pos p, int face) {
        Host h = host;
        return h != null && h.slotTaken(world, p.x, p.y, p.z, face);
    }

    /** Maska ścian, na których leżą przewody-części. */
    public static int faceMask(IBlockAccess world, Pos p) {
        Host h = host;
        if (h == null || !h.isHost(world, p.x, p.y, p.z)) return 0;
        int mask = 0;
        for (int f = 0; f < 6; f++) if (h.wireAt(world, p.x, p.y, p.z, f) != null) mask |= 1 << f;
        return mask;
    }
}
