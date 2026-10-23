package pl.geesio.redcircuit.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wspólna baza dla wszystkich "rurowych" bloków: kabli, rur, przewodów Azurity.
 * Blok ma środek i do 6 ramion łączących go z sąsiadami.
 */
public abstract class CableBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final Map<Direction, BooleanProperty> PROPS = new EnumMap<>(Direction.class);
    static {
        PROPS.put(Direction.NORTH, NORTH);
        PROPS.put(Direction.SOUTH, SOUTH);
        PROPS.put(Direction.EAST, EAST);
        PROPS.put(Direction.WEST, WEST);
        PROPS.put(Direction.UP, UP);
        PROPS.put(Direction.DOWN, DOWN);
    }

    private final double half;
    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    protected CableBlock(Properties properties, double thicknessPx) {
        super(properties);
        this.half = thicknessPx / 2.0;
    }

    public static BooleanProperty prop(Direction dir) {
        return PROPS.get(dir);
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        BooleanProperty p = PROPS.get(dir);
        return state.hasProperty(p) && state.getValue(p);
    }

    /** Stan z wszystkimi połączeniami wyłączonymi - do użycia w konstruktorach podklas. */
    protected BlockState disconnected(BlockState state) {
        for (BooleanProperty p : PROPS.values()) state = state.setValue(p, false);
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    /** Czy ten blok łączy się z sąsiadem w kierunku {@code dir}. */
    protected abstract boolean canConnectTo(LevelAccessor level, BlockPos pos, BlockState self, Direction dir, BlockPos neighborPos, BlockState neighbor);

    public BlockState computeConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            BlockPos np = pos.relative(dir);
            state = state.setValue(PROPS.get(dir), canConnectTo(level, pos, state, dir, np, level.getBlockState(np)));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeConnections(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPS.get(dir), canConnectTo(level, pos, state, dir, neighborPos, neighbor));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.computeIfAbsent(state, this::makeShape);
    }

    private VoxelShape makeShape(BlockState state) {
        double a = 8 - half, b = 8 + half;
        VoxelShape shape = Block.box(a, a, a, b, b, b);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, Block.box(a, a, 0, b, b, a));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, Block.box(a, a, b, b, b, 16));
        if (state.getValue(WEST)) shape = Shapes.or(shape, Block.box(0, a, a, a, b, b));
        if (state.getValue(EAST)) shape = Shapes.or(shape, Block.box(b, a, a, 16, b, b));
        if (state.getValue(DOWN)) shape = Shapes.or(shape, Block.box(a, 0, a, b, a, b));
        if (state.getValue(UP)) shape = Shapes.or(shape, Block.box(a, b, a, b, 16, b));
        return shape;
    }
}
