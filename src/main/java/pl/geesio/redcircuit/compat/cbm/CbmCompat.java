package pl.geesio.redcircuit.compat.cbm;

import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.PartConverter;
import codechicken.multipart.api.SimpleMultipartType;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.block.TileMultipart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.wire.PartWires;

/**
 * Integracja z CB Multipart (opcjonalna - ładowana tylko, gdy mod jest zainstalowany).
 * Przewody przyklejane do ścian mogą być częściami wieloczęściowego bloku, więc dzielą blok
 * z mikroblokami (osłony, panele) i innymi częściami.
 */
public final class CbmCompat {
    private CbmCompat() {}

    public static final DeferredRegister<MultipartType<?>> PART_TYPES =
            DeferredRegister.create(MultipartType.MULTIPART_TYPES, RedCircuit.MODID);
    public static final DeferredRegister<PartConverter> CONVERTERS =
            DeferredRegister.create(PartConverter.PART_CONVERTERS, RedCircuit.MODID);

    public static final DeferredHolder<MultipartType<?>, SimpleMultipartType<SignalWirePart>> SIGNAL_WIRE =
            PART_TYPES.register("signal_wire", () -> new SimpleMultipartType<>(client -> new SignalWirePart()));
    public static final DeferredHolder<MultipartType<?>, SimpleMultipartType<BundledWirePart>> BUNDLED_WIRE =
            PART_TYPES.register("bundled_wire", () -> new SimpleMultipartType<>(client -> new BundledWirePart()));
    public static final DeferredHolder<MultipartType<?>, SimpleMultipartType<PowerWirePart>> POWER_WIRE =
            PART_TYPES.register("power_wire", () -> new SimpleMultipartType<>(client -> new PowerWirePart()));

    public static final DeferredHolder<PartConverter, WireConverter> WIRE_CONVERTER =
            CONVERTERS.register("wires", WireConverter::new);

    /**
     * Energia przewodów-części: blok wieloczęściowy wystawia zdolność FE swojego przewodu ze stopu
     * energetycznego, więc generatory, akumulatory, zwykłe przewody i maszyny innych modów mogą
     * oddawać do niego energię (i ją z niego pobierać).
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        Block[] hosts = BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(e -> e.getKey().location().getNamespace().equals("cb_multipart"))
                .map(java.util.Map.Entry::getValue)
                .toArray(Block[]::new);
        if (hosts.length == 0) return;
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            if (!(be instanceof TileMultipart tile)) return null;
            PowerWirePart fallback = null;
            for (MultiPart part : tile.getPartList()) {
                if (part instanceof PowerWirePart wire) {
                    // Najpierw przewód leżący na ścianie, od której pytają; inaczej dowolny
                    if (side != null && wire.side() == side) return wire.getEnergy();
                    if (fallback == null) fallback = wire;
                }
            }
            return fallback == null ? null : fallback.getEnergy();
        }, hosts);
    }

    public static void init(IEventBus modBus) {
        PART_TYPES.register(modBus);
        CONVERTERS.register(modBus);
        PartWires.host = new CbmWireHost();
        modBus.addListener(CbmCompat::registerCapabilities);
        RedCircuit.LOGGER.info("RedCircuit: integracja z CB Multipart włączona");
    }
}
