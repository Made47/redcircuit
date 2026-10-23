package pl.geesio.redcircuit.compat.fmp;

import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TMultiPart;
import net.minecraft.block.Block;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.wire.PartWires;

/** Integracja z ForgeMultipart: przewody jako części (wspólny blok z mikroblokami i innymi przewodami). */
public final class FmpCompat {
    private FmpCompat() {}

    /** Wywoływane w fazie init, gdy ForgeMultipart jest zainstalowany. */
    @SuppressWarnings("deprecation")
    public static void init() {
        MultiPartRegistry.registerParts(new MultiPartRegistry.IPartFactory() {
            @Override
            public TMultiPart createPart(String name, boolean client) {
                return switch (name) {
                    case PartSignalWire.TYPE -> new PartSignalWire();
                    case PartBundledWire.TYPE -> new PartBundledWire();
                    case PartPowerWire.TYPE -> new PartPowerWire();
                    default -> null;
                };
            }
        }, new String[]{PartSignalWire.TYPE, PartBundledWire.TYPE, PartPowerWire.TYPE});
        MultiPartRegistry.registerConverter(new WireConverter());
        FmpWireHost host = new FmpWireHost();
        PartWires.host = host;
        PartWires.placer = host;
        RedCircuit.LOG.info("ForgeMultipart: przewody jako części włączone");
    }

    /** Nowy przewód-część odpowiadający blokowi-przewodowi (bez ustawionej ściany). */
    public static PartWire newPart(Block block, int meta) {
        if (block == ModBlocks.signalAlloyWire) return new PartSignalWire().setColor(-1);
        if (block == ModBlocks.insulatedWire) return new PartSignalWire().setColor(meta & 15);
        if (block == ModBlocks.bundledCable) return new PartBundledWire();
        if (block == ModBlocks.powerAlloyWire) return new PartPowerWire();
        return null;
    }
}
