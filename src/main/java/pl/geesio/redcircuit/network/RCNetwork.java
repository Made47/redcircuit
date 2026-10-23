package pl.geesio.redcircuit.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import pl.geesio.redcircuit.RedCircuit;

/** Kanał pakietów moda. */
public final class RCNetwork {
    private RCNetwork() {}

    public static SimpleNetworkWrapper CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(RedCircuit.MODID);
        CHANNEL.registerMessage(MessageGateConfig.Handler.class, MessageGateConfig.class, 0, Side.SERVER);
        CHANNEL.registerMessage(MessageFrequency.Handler.class, MessageFrequency.class, 1, Side.SERVER);
    }
}
