package pl.geesio.redcircuit.machine;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.tube.ExtractorBlock;

/** Igniter: gdy zasilony, zapala ogień przed sobą (i podtrzymuje go); po wyłączeniu gasi. */
public class IgniterBlock extends ExtractorBlock {
    public IgniterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean connectsToTube(BlockState state, Direction fromTube) {
        return false;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered) {
            level.scheduleTick(pos, this, 1);
        } else {
            BlockPos front = pos.relative(state.getValue(FACING));
            if (level.getBlockState(front).getBlock() instanceof BaseFireBlock) level.removeBlock(front, false);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) return;
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing);
        if (level.isEmptyBlock(front) && BaseFireBlock.canBePlacedAt(level, front, facing)) {
            level.setBlock(front, BaseFireBlock.getState(level, front), Block.UPDATE_ALL_IMMEDIATE);
            level.playSound(null, front, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, 1f);
        }
        level.scheduleTick(pos, this, 20);   // podtrzymuj ogień, dopóki jest sygnał
    }
}
