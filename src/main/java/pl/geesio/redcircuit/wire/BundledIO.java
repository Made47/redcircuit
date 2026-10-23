package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wymiana sygnału wiązkowego (16 kanałów) z blokami innych modów, np. komputerami CC: Tweaked.
 * Gdy nic nie jest zarejestrowane, kable działają dokładnie jak wcześniej.
 */
public final class BundledIO {
    private BundledIO() {}

    public interface Source {
        /**
         * Maska kanałów (bit n = kanał n włączony) wystawiana przez blok {@code pos} na jego ścianie {@code side},
         * albo -1, gdy blok nie jest źródłem sygnału wiązkowego.
         */
        int output(Level level, BlockPos pos, Direction side);

        /** Czy z blokiem ma się łączyć kabel wiązkowy (wygląd połączenia). */
        boolean isDevice(BlockGetter level, BlockPos pos, BlockState state);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    public static void register(Source source) {
        SOURCES.add(source);
    }

    public static boolean active() {
        return !SOURCES.isEmpty();
    }

    public static boolean isDevice(BlockGetter level, BlockPos pos, BlockState state) {
        if (WireTypes.typeOf(state.getBlock()) != null) return false;
        for (Source s : SOURCES) if (s.isDevice(level, pos, state)) return true;
        return false;
    }

    /** Suma masek wszystkich źródeł bloku {@code pos} na ścianie {@code side} (0, gdy brak). */
    public static int input(Level level, BlockPos pos, Direction side) {
        int mask = 0;
        for (Source s : SOURCES) {
            int m = s.output(level, pos, side);
            if (m > 0) mask |= m & 0xFFFF;
        }
        return mask;
    }

    /**
     * Sygnał, który kabel wiązkowy w bloku {@code pos} oddaje urządzeniu stykającemu się ze ścianą {@code side}
     * (maska kanałów o poziomie > 0), albo -1, gdy w bloku nie ma kabla wiązkowego.
     */
    public static int cableOutput(Level level, BlockPos pos, Direction side) {
        if (!level.isLoaded(pos)) return -1;
        boolean found = false;
        int mask = 0;
        Block block = level.getBlockState(pos).getBlock();
        Integer type = WireTypes.typeOf(block);
        if (type != null && type == WireTypes.BUNDLED
                && level.getBlockEntity(pos) instanceof BundledCableBlockEntity be) {
            found = true;
            for (int ch = 0; ch < 16; ch++) if (be.getChannel(ch) > 0) mask |= 1 << ch;
        } else if (PartWires.isHost(level, pos) && PartWires.host != null) {
            for (int f = 0; f < 6; f++) {
                Integer t = PartWires.wireAt(level, pos, f);
                if (t == null || t != WireTypes.BUNDLED) continue;
                found = true;
                for (int ch = 0; ch < 16; ch++) if (PartWires.host.getSignal(level, pos, f, ch) > 0) mask |= 1 << ch;
            }
        }
        if (!found) return -1;
        // Podczas liczenia wejść sieci kable nie oddają sygnału (inaczej sygnał krążyłby w kółko).
        return WireNetwork.isEmitting() ? mask : 0;
    }
}
