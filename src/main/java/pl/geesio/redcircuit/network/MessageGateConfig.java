package pl.geesio.redcircuit.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import pl.geesio.redcircuit.gate.BlockGate;

/** Ustawienia z okna bramki (klient -> serwer). */
public class MessageGateConfig implements IMessage {
    public int x, y, z, interval, max, disabled;
    public boolean resetCount, muted, mirrored, quiet;

    public MessageGateConfig() {
    }

    public MessageGateConfig(int x, int y, int z, int interval, int max, boolean resetCount, boolean muted,
                             boolean mirrored, int disabled, boolean quiet) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.interval = interval;
        this.max = max;
        this.resetCount = resetCount;
        this.muted = muted;
        this.mirrored = mirrored;
        this.disabled = disabled;
        this.quiet = quiet;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(interval);
        buf.writeInt(max);
        buf.writeByte(disabled);
        buf.writeByte((resetCount ? 1 : 0) | (muted ? 2 : 0) | (mirrored ? 4 : 0) | (quiet ? 8 : 0));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        interval = buf.readInt();
        max = buf.readInt();
        disabled = buf.readByte() & 7;
        int flags = buf.readByte();
        resetCount = (flags & 1) != 0;
        muted = (flags & 2) != 0;
        mirrored = (flags & 4) != 0;
        quiet = (flags & 8) != 0;
    }

    public static class Handler implements IMessageHandler<MessageGateConfig, IMessage> {
        @Override
        public IMessage onMessage(MessageGateConfig m, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            World world = player.worldObj;
            if (player.getDistanceSq(m.x + 0.5, m.y + 0.5, m.z + 0.5) > 64.0) return null;
            if (!world.blockExists(m.x, m.y, m.z)) return null;
            if (world.getBlock(m.x, m.y, m.z) instanceof BlockGate gate) {
                gate.applySettings(world, m.x, m.y, m.z, m.interval, m.max, m.resetCount, m.muted, m.mirrored,
                        m.disabled, m.quiet);
            }
            return null;
        }
    }
}
