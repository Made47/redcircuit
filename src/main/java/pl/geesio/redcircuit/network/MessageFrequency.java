package pl.geesio.redcircuit.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import pl.geesio.redcircuit.wireless.BlockWireless;
import pl.geesio.redcircuit.wireless.ItemWirelessRemote;
import pl.geesio.redcircuit.wireless.WirelessNetwork;

/** Częstotliwość z okna (klient -> serwer). Cel: 0 = blok (x, y, z), 1 = trzymany pilot. */
public class MessageFrequency implements IMessage {
    public int target, x, y, z, frequency;

    public MessageFrequency() {
    }

    public MessageFrequency(int target, int x, int y, int z, int frequency) {
        this.target = target;
        this.x = x;
        this.y = y;
        this.z = z;
        this.frequency = frequency;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(target);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(frequency);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        target = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        frequency = buf.readInt();
    }

    public static class Handler implements IMessageHandler<MessageFrequency, IMessage> {
        @Override
        public IMessage onMessage(MessageFrequency m, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            int freq = WirelessNetwork.clampFreq(m.frequency);
            if (m.target == 0) {
                World world = player.worldObj;
                if (player.getDistanceSq(m.x + 0.5, m.y + 0.5, m.z + 0.5) > 64.0 || !world.blockExists(m.x, m.y, m.z)) return null;
                if (world.getBlock(m.x, m.y, m.z) instanceof BlockWireless block) block.applyFrequency(world, m.x, m.y, m.z, freq);
            } else {
                ItemStack stack = player.getHeldItem();
                if (stack != null && stack.getItem() instanceof ItemWirelessRemote) {
                    ItemWirelessRemote.setFrequency(stack, freq);
                    player.inventoryContainer.detectAndSendChanges();
                }
            }
            return null;
        }
    }
}
