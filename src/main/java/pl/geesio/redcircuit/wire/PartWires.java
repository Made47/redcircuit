package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Most do przewodów zapisanych jako części innego moda (CB Multipart). Gdy mod nie jest zainstalowany,
 * {@link #host} jest null i wszystkie metody zwracają "brak przewodu" - reszta kodu działa jak wcześniej.
 */
public final class PartWires {
    private PartWires() {}

    /** Ustawiane przez integrację z CB Multipart. */
    @Nullable
    public static Host host;

    public interface Host {
        /** Rodzaj przewodu-części leżącego na ścianie {@code face} (0..5) w bloku {@code pos}, albo null. */
        @Nullable
        Integer wireAt(BlockGetter level, BlockPos pos, int face);

        /** Czy w bloku są jakiekolwiek części (np. mikrobloki) - wtedy blok nie jest "pusty" dla narożników. */
        boolean isHost(BlockGetter level, BlockPos pos);

        /** Zapisany poziom sygnału (kanał ch >= 0 dla kabla wiązkowego, -1 dla zwykłego przewodu). */
        int getSignal(BlockGetter level, BlockPos pos, int face, int ch);

        void setSignal(Level level, BlockPos pos, int face, int ch, int value);

        /** Po zmianie sygnału: odświeżenie klienta i powiadomienie sąsiadów. */
        void notifyChanged(Level level, BlockPos pos, int face);
    }

    @Nullable
    public static Integer wireAt(BlockGetter level, BlockPos pos, int face) {
        Host h = host;
        return h == null ? null : h.wireAt(level, pos, face);
    }

    @Nullable
    public static Integer wireAt(BlockGetter level, BlockPos pos, Direction face) {
        return wireAt(level, pos, face.get3DDataValue());
    }

    public static boolean isHost(BlockGetter level, BlockPos pos) {
        Host h = host;
        return h != null && h.isHost(level, pos);
    }

    /** Maska ścian, na których leżą przewody-części. */
    public static int faceMask(BlockGetter level, BlockPos pos) {
        Host h = host;
        if (h == null || !h.isHost(level, pos)) return 0;
        int mask = 0;
        for (int f = 0; f < 6; f++) if (h.wireAt(level, pos, f) != null) mask |= 1 << f;
        return mask;
    }
}
