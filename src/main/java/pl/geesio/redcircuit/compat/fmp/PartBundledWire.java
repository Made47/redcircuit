package pl.geesio.redcircuit.compat.fmp;

import net.minecraft.nbt.NBTTagCompound;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.WireTypes;

/** Kabel wiązkowy jako część ForgeMultipart (16 kanałów). */
public class PartBundledWire extends PartWire {
    public static final String TYPE = "redcircuit_bundled_wire";

    private final byte[] channels = new byte[16];

    public int getChannel(int ch) {
        return channels[ch];
    }

    public void setChannelSilently(int ch, int value) {
        channels[ch] = (byte) value;
    }

    public void setChannel(int ch, int value) {
        if (channels[ch] == value) return;
        channels[ch] = (byte) value;
        tile().markDirty();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public BlockFaceWire hostBlock() {
        return (BlockFaceWire) ModBlocks.bundledCable;
    }

    @Override
    public int wireType() {
        return WireTypes.BUNDLED;
    }

    @Override
    public void save(NBTTagCompound tag) {
        super.save(tag);
        tag.setByteArray("Channels", channels.clone());
    }

    @Override
    public void load(NBTTagCompound tag) {
        super.load(tag);
        byte[] data = tag.getByteArray("Channels");
        for (int i = 0; i < 16 && i < data.length; i++) channels[i] = data[i];
    }
}
