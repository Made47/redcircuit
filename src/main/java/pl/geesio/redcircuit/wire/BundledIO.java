package pl.geesio.redcircuit.wire;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Pos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wymiana sygnału wiązkowego (16 kanałów) z blokami innych modów, np. komputerami ComputerCraft
 * (podłączane w etapie integracji). Gdy nic nie jest zarejestrowane, kable działają samodzielnie.
 */
public final class BundledIO {
    private BundledIO() {}

    public interface Source {
        /** Maska kanałów wystawiana przez blok na jego ścianie {@code side}, albo -1, gdy to nie źródło. */
        int output(World world, Pos pos, ForgeDirection side);

        /** Czy z blokiem ma się łączyć kabel wiązkowy (wygląd połączenia). */
        boolean isDevice(IBlockAccess world, Pos pos, Block block);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    public static void register(Source source) {
        SOURCES.add(source);
    }

    public static boolean active() {
        return !SOURCES.isEmpty();
    }

    public static boolean isDevice(IBlockAccess world, Pos pos, Block block) {
        if (block instanceof WireTypes.IWire) return false;
        for (Source s : SOURCES) if (s.isDevice(world, pos, block)) return true;
        return false;
    }

    /** Suma masek wszystkich źródeł bloku {@code pos} na ścianie {@code side}. */
    public static int input(World world, Pos pos, ForgeDirection side) {
        int mask = 0;
        for (Source s : SOURCES) {
            int m = s.output(world, pos, side);
            if (m > 0) mask |= m & 0xFFFF;
        }
        return mask;
    }

    /** Maska kanałów (> 0) kabla wiązkowego w bloku {@code pos}, albo -1, gdy nie ma tam kabla. */
    public static int cableOutput(World world, Pos pos, ForgeDirection side) {
        if (!world.blockExists(pos.x, pos.y, pos.z)) return -1;
        if (PartWires.isHost(world, pos) && PartWires.host != null) {
            // Kable wiązkowe-części ForgeMultipart w tym bloku
            int mask = 0;
            boolean any = false;
            for (int f = 0; f < 6; f++) {
                Integer t = PartWires.wireAt(world, pos, f);
                if (t == null || t != WireTypes.BUNDLED) continue;
                any = true;
                for (int ch = 0; ch < 16; ch++) if (PartWires.host.getSignal(world, pos.x, pos.y, pos.z, f, ch) > 0) mask |= 1 << ch;
            }
            if (any) return WireNetwork.isEmitting() ? mask : 0;
        }
        Integer type = WireTypes.typeAt(world, pos.x, pos.y, pos.z);
        if (type == null || type != WireTypes.BUNDLED) return -1;
        TileWire t = BlockFaceWire.tile(world, pos.x, pos.y, pos.z);
        if (t == null) return -1;
        int mask = 0;
        for (int ch = 0; ch < 16; ch++) if (t.channels[ch] > 0) mask |= 1 << ch;
        return WireNetwork.isEmitting() ? mask : 0;
    }
}
