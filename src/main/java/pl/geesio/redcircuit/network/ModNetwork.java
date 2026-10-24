package pl.geesio.redcircuit.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import pl.geesio.redcircuit.gate.GateBlock;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(GateConfigPayload.TYPE, GateConfigPayload.STREAM_CODEC, ModNetwork::handleGateConfig);
        registrar.playToServer(FrequencyPayload.TYPE, FrequencyPayload.STREAM_CODEC, ModNetwork::handleFrequency);
    }

    private static void handleFrequency(FrequencyPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            int freq = pl.geesio.redcircuit.wireless.WirelessNetwork.clampFreq(msg.frequency());
            if (msg.target() == 0) {
                Level level = player.level();
                if (!level.isLoaded(msg.pos()) || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(msg.pos())) > 64.0) return;
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

    private static void handleGateConfig(GateConfigPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            Level level = player.level();
            if (!level.isLoaded(msg.pos()) || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(msg.pos())) > 64.0) return;
            if (level.getBlockState(msg.pos()).getBlock() instanceof GateBlock gate && gate.configurable()) {
                gate.applySettings(level, msg.pos(), msg.interval(), msg.max(), msg.resetCount(), msg.muted(), msg.mirrored(),
                        msg.disabled(), msg.quiet());
            }
        });
    }
}
