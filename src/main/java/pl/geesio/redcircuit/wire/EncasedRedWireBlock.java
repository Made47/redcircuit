package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Przewód ze stopu sygnałowego (color = -1) lub izolowany (0..15) w obudowie. */
public class EncasedRedWireBlock extends EncasedWireBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    private final int color;

    public EncasedRedWireBlock(Properties properties, int color) {
        super(properties, 6.0);
        this.color = color;
        registerDefaultState(disconnected(stateDefinition.any()).setValue(POWER, 0));
    }

    @Override
    public int wireType() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    protected boolean connectsToDevice(LevelAccessor level, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        return RedWireBlock.isRedstoneDevice(neighbor, dir);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return WireNetwork.isEmitting() ? state.getValue(POWER) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return WireNetwork.isEmitting() ? state.getValue(POWER) : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) WireNetwork.update(level, pos);
    }

    @Override
    protected void onNetworkChanged(Level level, BlockPos pos) {
        WireNetwork.onWireChanged(level, pos, this);
    }

    @Override
    protected void onNetworkRemoved(Level level, BlockPos pos) {
        WireNetwork.onWireRemoved(level, pos, this);
    }
}
