package pl.geesio.redcircuit.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.geesio.redcircuit.RedCircuit;

/**
 * Klient -> serwer: nowa częstotliwość. {@code target} 0 = blok na pozycji {@code pos},
 * 1 = pilot w głównej ręce, 2 = pilot w drugiej ręce.
 */
public record FrequencyPayload(int target, BlockPos pos, int frequency) implements CustomPacketPayload {
    public static final Type<FrequencyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RedCircuit.MODID, "frequency"));

    public static final StreamCodec<ByteBuf, FrequencyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FrequencyPayload::target,
            BlockPos.STREAM_CODEC, FrequencyPayload::pos,
            ByteBufCodecs.VAR_INT, FrequencyPayload::frequency,
            FrequencyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
