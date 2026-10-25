package pl.geesio.redcircuit.wire;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        int[] data = new int[16];
        for (int i = 0; i < 16; i++) data[i] = channels[i];
        output.putIntArray("Channels", data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] data = input.getIntArray("Channels").orElse(new int[0]);
        for (int i = 0; i < 16 && i < data.length; i++) channels[i] = (byte) data[i];
    }
}
