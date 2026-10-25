package pl.geesio.redcircuit.compat.cc;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.wire.BundledIO;

/**
 * Integracja z CC: Tweaked (ładowana tylko, gdy mod "computercraft" jest zainstalowany).
 * <ul>
 *     <li>kabel wiązkowy RedCircuit ↔ {@code redstone.getBundledInput/setBundledOutput} komputerów i żółwi,</li>
 *     <li>peryferia: bramki z ustawieniami ({@code redcircuit_gate}) i nadajniki/odbiorniki bezprzewodowe
 *     ({@code redcircuit_wireless}),</li>
 *     <li>maszyny z inwentarzem i płynami są widoczne automatycznie jako peryferia ogólne CC
 *     ({@code inventory}, {@code fluid_storage}) - przez Fabric Transfer API.</li>
 * </ul>
 */
public final class CcCompat {
    private CcCompat() {}

    public static void init() {
        // Komputer czyta kabel: CC pyta o (pozycja kabla, ściana kabla zwrócona do komputera).
        ComputerCraftAPI.registerBundledRedstoneProvider(BundledIO::cableOutput);

        // Kabel czyta komputer: pytamy CC o wyjście bloku obok (bez naszego własnego dostawcy - kable nie
        // oddają sygnału, gdy sieć liczy wejścia).
        BundledIO.register(new BundledIO.Source() {
            @Override
            public int output(Level level, BlockPos pos, Direction side) {
                return isComputer(level.getBlockState(pos)) ? ComputerCraftAPI.getBundledRedstoneOutput(level, pos, side) : -1;
            }

            @Override
            public boolean isDevice(BlockGetter level, BlockPos pos, BlockState state) {
                return isComputer(state);
            }
        });

        registerPeripherals();
        RedCircuit.LOGGER.info("RedCircuit: integracja z CC: Tweaked włączona");
    }

    /** Komputery, żółwie i przekaźnik redstone z CC: Tweaked. */
    static boolean isComputer(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!ComputerCraftAPI.MOD_ID.equals(id.getNamespace())) return false;
        String p = id.getPath();
        return p.startsWith("computer_") || p.startsWith("turtle_") || p.equals("redstone_relay");
    }

    private static void registerPeripherals() {
        PeripheralLookup.get().registerForBlockEntity((be, side) -> new GatePeripheral(be), ModBlockEntities.GATE.get());
        PeripheralLookup.get().registerForBlockEntity((be, side) -> new WirelessPeripheral(be), ModBlockEntities.WIRELESS.get());
    }
}
