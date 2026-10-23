package pl.geesio.redcircuit.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/** Częstotliwość nadajnika / odbiornika. */
public class WirelessBlockEntity extends BlockEntity {
    private int frequency = 1;

    public WirelessBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS.get(), pos, state);
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int f) {
        frequency = WirelessNetwork.clampFreq(f);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
        }
    }

    /** Odbiornik: co 2 ticki sprawdza, czy jego częstotliwość jest aktywna. */
    public static void tick(Level level, BlockPos pos, BlockState state, WirelessBlockEntity be) {
        if (level.isClientSide || level.getServer() == null || (level.getGameTime() & 1) != 0) return;
        if (!(state.getBlock() instanceof WirelessReceiverBlock receiver)) return;
        boolean on = WirelessNetwork.get(level.getServer()).isActive(level.getServer(), be.frequency);
        receiver.setOutput(level, pos, state, on);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Frequency", frequency);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequency = tag.contains("Frequency") ? WirelessNetwork.clampFreq(tag.getInt("Frequency")) : 1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
