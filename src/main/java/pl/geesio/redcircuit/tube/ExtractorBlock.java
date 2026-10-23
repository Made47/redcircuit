package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemHandlerHelper;

/**
 * Extractor: na impuls redstone wyciąga stos przedmiotów z inwentarza za sobą
 * i wypycha go do przodu (do rury, inwentarza albo na ziemię).
 * Stawia się go, klikając na inwentarz, z którego ma pobierać.
 */
public class ExtractorBlock extends Block implements TubeConnectable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ExtractorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public boolean connectsToTube(BlockState state, Direction fromTube) {
        // Rura podłącza się tylko do wyjścia (przodu).
        return fromTube == state.getValue(FACING).getOpposite();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            if (powered) level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        transfer(level, pos, state);
    }

    // ------------------------------------------------------------------

    /** Czy ten blok może pobrać dany przedmiot (Filter / Sorting Machine nadpisują). */
    protected boolean accepts(Level level, BlockPos pos, ItemStack stack) {
        return true;
    }

    /** Kolor nadawany przedmiotowi w rurze (-1 = bez koloru). */
    protected int colorFor(Level level, BlockPos pos, ItemStack stack) {
        return -1;
    }

    protected void transfer(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos source = pos.relative(facing.getOpposite());
        IItemHandler src = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.ItemHandler.BLOCK, source, facing);
        if (src == null) return;

        for (int slot = 0; slot < src.getSlots(); slot++) {
            ItemStack simulated = src.extractItem(slot, 64, true);
            if (simulated.isEmpty() || !accepts(level, pos, simulated)) continue;
            int color = colorFor(level, pos, simulated);
            int count = deliverableCount(level, pos, facing, simulated, color);
            if (count <= 0) continue;
            ItemStack extracted = src.extractItem(slot, count, false);
            if (extracted.isEmpty()) continue;
            deliver(level, pos, facing, extracted, color);
            return;
        }
    }

    public static int deliverableCount(Level level, BlockPos pos, Direction facing, ItemStack stack, int color) {
        BlockPos front = pos.relative(facing);
        if (level.getBlockEntity(front) instanceof TubeBlockEntity tube) {
            return tube.canAccept(color) ? stack.getCount() : 0;
        }
        IItemHandler dst = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.ItemHandler.BLOCK, front, facing.getOpposite());
        if (dst != null) {
            return stack.getCount() - ItemHandlerHelper.insertItem(dst, stack.copy(), true).getCount();
        }
        if (level.getBlockState(front).getCollisionShape(level, front).isEmpty()) {
            return stack.getCount();
        }
        return 0;
    }

    public static void deliver(Level level, BlockPos pos, Direction facing, ItemStack stack, int color) {
        BlockPos front = pos.relative(facing);
        if (level.getBlockEntity(front) instanceof TubeBlockEntity tube) {
            tube.insert(stack, facing.getOpposite(), color);
            return;
        }
        IItemHandler dst = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.ItemHandler.BLOCK, front, facing.getOpposite());
        if (dst != null) {
            stack = ItemHandlerHelper.insertItem(dst, stack, false);
            if (stack.isEmpty()) return;
        }
        TubeBlockEntity.spawn(level, front, stack, facing);
    }
}
