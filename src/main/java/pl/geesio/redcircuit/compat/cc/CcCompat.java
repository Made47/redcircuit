package pl.geesio.redcircuit.compat.cc;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.wire.BundledIO;

/**
 * Integracja z CC: Tweaked (ładowana tylko, gdy mod "computercraft" jest zainstalowany).
 * <ul>
 *     <li>kabel wiązkowy RedCircuit ↔ {@code redstone.getBundledInput/setBundledOutput} komputerów i żółwi,</li>
 *     <li>peryferia: bramki z ustawieniami ({@code redcircuit_gate}) i nadajniki/odbiorniki bezprzewodowe
 *     ({@code redcircuit_wireless}),</li>
 *     <li>maszyny z inwentarzem, energią albo płynami są widoczne automatycznie jako peryferia ogólne CC
 *     ({@code inventory}, {@code energy_storage}, {@code fluid_storage}).</li>
 * </ul>
 */
public final class CcCompat {
    private CcCompat() {}

    public static void init(IEventBus modBus) {
        // Komputer czyta kabel: CC pyta o (pozycja kabla, ściana kabla zwrócona do komputera).
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) ->
                e.enqueueWork(() -> ComputerCraftAPI.registerBundledRedstoneProvider(BundledIO::cableOutput)));

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

        modBus.addListener(CcCompat::registerCapabilities);
        RedCircuit.LOGGER.info("RedCircuit: integracja z CC: Tweaked włączona");
    }

    /** Komputery, żółwie i przekaźnik redstone z CC: Tweaked. */
    static boolean isComputer(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!ComputerCraftAPI.MOD_ID.equals(id.getNamespace())) return false;
        String p = id.getPath();
        return p.startsWith("computer_") || p.startsWith("turtle_") || p.equals("redstone_relay");
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ModBlockEntities.GATE.get(),
                (be, side) -> new GatePeripheral(be));
        event.registerBlockEntity(PeripheralCapability.get(), ModBlockEntities.WIRELESS.get(),
                (be, side) -> new WirelessPeripheral(be));
    }
}
