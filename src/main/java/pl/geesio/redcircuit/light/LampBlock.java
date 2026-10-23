package pl.geesio.redcircuit.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Lampa w jednym z 16 kolorów. Zwykła świeci przy sygnale redstone, odwrócona - bez sygnału.
 * Gaśnie z małym opóźnieniem (jak lampa z gry), żeby nie migotała przy krótkich impulsach.
 */
public class LampBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final int color;
    private final boolean inverted;

    public LampBlock(Properties properties, int color, boolean inverted) {
        super(properties);
        this.color = color;
        this.inverted = inverted;
        registerDefaultState(stateDefinition.any().setValue(LIT, inverted));
    }

    public int getColor() {
        return color;
    }

    public boolean isInverted() {
        return inverted;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    private boolean shouldBeLit(Level level, BlockPos pos) {
        return level.hasNeighborSignal(pos) != inverted;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(LIT, shouldBeLit(ctx.getLevel(), ctx.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean lit = state.getValue(LIT);
        boolean want = shouldBeLit(level, pos);
        if (lit == want) return;
        if (want) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
        } else {
            level.scheduleTick(pos, this, 4);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && !shouldBeLit(level, pos)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
        }
    }
}
