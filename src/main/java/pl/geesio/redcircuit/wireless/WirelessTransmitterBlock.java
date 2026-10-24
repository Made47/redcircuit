package pl.geesio.redcircuit.wireless;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.server.level.ServerLevel;
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
        if (level.isClientSide() || level.getServer() == null) return;
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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
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
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (level.getServer() != null) {
            WirelessNetwork.get(level.getServer()).removeTransmitter(level, pos);
        }
    }

    @Override
    public void applyFrequency(Level level, BlockPos pos, int freq) {
        super.applyFrequency(level, pos, freq);
        update(level, pos, level.getBlockState(pos));
    }
}
