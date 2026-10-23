package pl.geesio.redcircuit.compat.cbm;

import codechicken.multipart.api.MultipartType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.WireTypes;

/** Kabel wiązkowy jako część CB Multipart (16 kanałów). */
public class BundledWirePart extends RCWirePart {
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
        tile().setChanged();
    }

    @Override
    public MultipartType<?> getType() {
        return CbmCompat.BUNDLED_WIRE.get();
    }

    @Override
    public FaceWireBlock hostBlock() {
        return ModBlocks.BUNDLED_CABLE.get();
    }

    @Override
    public int wireType() {
        return WireTypes.BUNDLED;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        super.save(tag, registries);
        tag.putByteArray("Channels", channels.clone());
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        super.load(tag, registries);
        byte[] data = tag.getByteArray("Channels");
        for (int i = 0; i < 16 && i < data.length; i++) channels[i] = data[i];
    }
}
