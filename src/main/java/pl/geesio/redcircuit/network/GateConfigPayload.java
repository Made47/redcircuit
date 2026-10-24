package pl.geesio.redcircuit.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import pl.geesio.redcircuit.RedCircuit;

/** Klient -> serwer: nowe ustawienia bramki z okna ustawień. */
public record GateConfigPayload(BlockPos pos, int interval, int max, boolean resetCount, boolean muted, boolean mirrored,
                                int disabled, boolean quiet) implements CustomPacketPayload {
    public static final Type<GateConfigPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(RedCircuit.MODID, "gate_config"));

    private static final int RESET = 1, MUTED = 2, MIRRORED = 4, QUIET = 8;

    public static final StreamCodec<ByteBuf, GateConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                BlockPos.STREAM_CODEC.encode(buf, msg.pos());
                ByteBufCodecs.VAR_INT.encode(buf, msg.interval());
                ByteBufCodecs.VAR_INT.encode(buf, msg.max());
                ByteBufCodecs.VAR_INT.encode(buf, msg.disabled());
                int flags = (msg.resetCount() ? RESET : 0) | (msg.muted() ? MUTED : 0) | (msg.mirrored() ? MIRRORED : 0)
                        | (msg.quiet() ? QUIET : 0);
                buf.writeByte(flags);
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                int interval = ByteBufCodecs.VAR_INT.decode(buf);
                int max = ByteBufCodecs.VAR_INT.decode(buf);
                int disabled = ByteBufCodecs.VAR_INT.decode(buf);
                int flags = buf.readByte();
                return new GateConfigPayload(pos, interval, max, (flags & RESET) != 0, (flags & MUTED) != 0,
                        (flags & MIRRORED) != 0, disabled, (flags & QUIET) != 0);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
