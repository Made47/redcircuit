package pl.geesio.redcircuit.compat.cbm.client;

import codechicken.multipart.api.MultipartClientRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import pl.geesio.redcircuit.compat.cbm.CbmCompat;

/** Rejestracja rendererów przewodów-części (tylko klient, tylko z CB Multipart). */
public final class CbmCompatClient {
    private CbmCompatClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(CbmCompatClient::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            WirePartRenderer renderer = new WirePartRenderer();
            MultipartClientRegistry.register(CbmCompat.SIGNAL_WIRE.get(), renderer);
            MultipartClientRegistry.register(CbmCompat.BUNDLED_WIRE.get(), renderer);
            MultipartClientRegistry.register(CbmCompat.POWER_WIRE.get(), renderer);
            pl.geesio.redcircuit.RedCircuit.LOGGER.info("RedCircuit: zarejestrowano renderery przewodów-części CB Multipart");
        });
    }
}
