package pl.geesio.redcircuit.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import pl.geesio.redcircuit.gate.GateBlock;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(GateConfigPayload.TYPE, GateConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FrequencyPayload.TYPE, FrequencyPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GateConfigPayload.TYPE, ModNetwork::handleGateConfig);
        ServerPlayNetworking.registerGlobalReceiver(FrequencyPayload.TYPE, ModNetwork::handleFrequency);
    }

    private static void handleFrequency(FrequencyPayload msg, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayer player = ctx.player();
            int freq = pl.geesio.redcircuit.wireless.WirelessNetwork.clampFreq(msg.frequency());
            if (msg.target() == 0) {
                Level level = player.level();
                if (!level.isLoaded(msg.pos()) || player.distanceToSqr(msg.pos().getCenter()) > 64.0) return;
                if (level.getBlockState(msg.pos()).getBlock() instanceof pl.geesio.redcircuit.wireless.WirelessBlock block) {
                    block.applyFrequency(level, msg.pos(), freq);
                }
            } else {
                var hand = msg.target() == 1 ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND;
                var stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof pl.geesio.redcircuit.wireless.WirelessRemoteItem) {
                    stack.set(pl.geesio.redcircuit.registry.ModDataComponents.FREQUENCY.get(), freq);
                }
            }
        });
    }

    private static void handleGateConfig(GateConfigPayload msg, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayer player = ctx.player();
            Level level = player.level();
            if (!level.isLoaded(msg.pos()) || player.distanceToSqr(msg.pos().getCenter()) > 64.0) return;
            if (level.getBlockState(msg.pos()).getBlock() instanceof GateBlock gate && gate.configurable()) {
                gate.applySettings(level, msg.pos(), msg.interval(), msg.max(), msg.resetCount(), msg.muted(), msg.mirrored(),
                        msg.disabled(), msg.quiet());
            }
        });
    }
}
