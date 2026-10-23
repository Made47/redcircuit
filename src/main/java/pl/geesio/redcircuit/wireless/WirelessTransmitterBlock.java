package pl.geesio.redcircuit.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Nadajnik: gdy dostaje sygnał redstone, nadaje na swojej częstotliwości. */
public class WirelessTransmitterBlock extends WirelessBlock {
    public WirelessTransmitterBlock(Properties properties) {
        super(properties);
    }

    private void update(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || level.getServer() == null) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
        if (level.getBlockEntity(pos) instanceof WirelessBlockEntity be) {
            WirelessNetwork net = WirelessNetwork.get(level.getServer());
            net.removeTransmitter(level, pos);
            if (powered) net.setTransmitter(level, pos, be.getFrequency(), true);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!state.canSurvive(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }
        update(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) update(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && level.getServer() != null && !state.is(newState.getBlock())) {
            WirelessNetwork.get(level.getServer()).removeTransmitter(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void applyFrequency(Level level, BlockPos pos, int freq) {
        super.applyFrequency(level, pos, freq);
        update(level, pos, level.getBlockState(pos));
    }
}
