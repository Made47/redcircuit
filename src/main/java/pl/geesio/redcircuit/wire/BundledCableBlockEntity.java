package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/** Przechowuje poziom sygnału (0-15) dla każdego z 16 kanałów kabla wiązkowego. */
public class BundledCableBlockEntity extends BlockEntity {
    private final byte[] channels = new byte[16];

    public BundledCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUNDLED_CABLE.get(), pos, state);
    }

    public int getChannel(int ch) {
        return channels[ch];
    }

    public void setChannel(int ch, int value) {
        if (channels[ch] != value) {
            channels[ch] = (byte) value;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByteArray("Channels", channels.clone());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        byte[] data = tag.getByteArray("Channels");
        for (int i = 0; i < 16 && i < data.length; i++) channels[i] = data[i];
    }
}
