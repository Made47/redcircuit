package pl.geesio.redcircuit.platform.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Wysyłanie pakietów z klienta (jak PacketDistributor.sendToServer z NeoForge). Tylko po stronie klienta. */
public final class PacketDistributor {
    private PacketDistributor() {}

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
