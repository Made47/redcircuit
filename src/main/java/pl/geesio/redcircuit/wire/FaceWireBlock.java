package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Przewód przyklejony do powierzchni bloku.
 * <p>
 * Właściwości north/south/east/west/up/down oznaczają, do których ścian sąsiednich bloków
 * przyklejony jest przewód w tej przestrzeni (np. down = leży na podłodze).
 * W jednym bloku może być kilka przewodów tego samego typu na różnych ścianach - łączą się wtedy
 * w narożnikach wewnętrznych. Przewody łączą się też prosto oraz przez narożniki zewnętrzne
 * (zawijając się na krawędzi bloku).
 */
public abstract class FaceWireBlock extends Block {
    private static final Map<Direction, BooleanProperty> FACES = new EnumMap<>(Direction.class);
    static {
        FACES.put(Direction.DOWN, BlockStateProperties.DOWN);
        FACES.put(Direction.UP, BlockStateProperties.UP);
        FACES.put(Direction.NORTH, BlockStateProperties.NORTH);
        FACES.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        FACES.put(Direction.WEST, BlockStateProperties.WEST);
        FACES.put(Direction.EAST, BlockStateProperties.EAST);
    }

    /** Rodzaj połączenia przewodu na danej ścianie w danym kierunku. */
    public enum Link { NONE, STRAIGHT, INTERNAL, CORNER, DEVICE, ENCASED }

    /** Szerokość i wysokość przewodu w pikselach (1/16 bloku). */
    private final float width;
    private final float height;
    private final Map<ShapeKey, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    private record ShapeKey(BlockState state, WireConnections conn) {}

    protected FaceWireBlock(Properties properties, float widthPx, float heightPx) {
        super(properties);
        this.width = widthPx;
        this.height = heightPx;
    }

    public float wireWidth() {
        return width;
    }

    public float wireHeight() {
        return height;
    }

    public static BooleanProperty face(Direction side) {
        return FACES.get(side);
    }

    public static boolean hasFace(BlockState state, Direction side) {
        return state.getBlock() instanceof FaceWireBlock && state.getValue(FACES.get(side));
    }

    public static int faceCount(BlockState state) {
        int n = 0;
        for (Direction d : Direction.values()) if (hasFace(state, d)) n++;
        return n;
    }

    protected BlockState noFaces(BlockState state) {
        for (BooleanProperty p : FACES.values()) state = state.setValue(p, false);
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
                BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST);
    }

    // ------------------------------------------------------------------ połączenia

    /** Czy ten przewód łączy się z innym typem przewodu (symetryczne). */
    public abstract boolean compatible(FaceWireBlock other);

    /** Połączenie z urządzeniem niebędącym przewodem (bramka, dźwignia, lampa...). */
    protected boolean connectsToDevice(BlockGetter level, BlockPos pos, Direction side, Direction dir,
                                       BlockPos neighborPos, BlockState neighbor) {
        return false;
    }

    public Link link(BlockGetter level, BlockPos pos, BlockState state, Direction side, Direction dir) {
        return target(level, pos, state, side, dir).link();
    }

    /** Pełny wynik połączenia (z pozycją i ścianą przewodu po drugiej stronie). */
    public WireLinks.Target target(BlockGetter level, BlockPos pos, BlockState state, Direction side, Direction dir) {
        Integer me = WireTypes.typeOf(this);
        int selfType = me == null ? WireTypes.RED : me;
        return WireLinks.link(level, pos, selfType, side, dir, f -> hasFace(state, Direction.from3DDataValue(f)), false,
                (lvl, np, ns) -> connectsToDevice(lvl, pos, side, dir, np, ns));
    }

    /** Pozycja przewodu, z którym łączy się dane połączenie (lub null). */
    @Nullable
    public static BlockPos linkedPos(BlockPos pos, Direction side, Direction dir, Link link) {
        return switch (link) {
            case STRAIGHT, ENCASED -> pos.relative(dir);
            case CORNER -> pos.relative(dir).relative(side);
            default -> null;
        };
    }

    public WireConnections computeConnections(BlockGetter level, BlockPos pos, BlockState state) {
        WireConnections.Builder b = new WireConnections.Builder();
        for (Direction side : Direction.values()) {
            if (!hasFace(state, side)) continue;
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() == side.getAxis()) continue;
                b.set(side, dir, WireLinks.reach(link(level, pos, state, side, dir), side, dir));
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------ geometria

    /**
     * Prostopadłościany (w pikselach, 0..16) tworzące przewód na ścianie {@code side}.
     * Każdy element: {minX, minY, minZ, maxX, maxY, maxZ}.
     */
    public java.util.List<float[]> boxes(Direction side, WireConnections conn) {
        java.util.List<float[]> out = new java.util.ArrayList<>();
        float hw = width / 2f;
        float[] min = new float[3], max = new float[3];
        int sAxis = side.getAxis().ordinal();
        // Grubość
        float t0 = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 - height : 0;
        float t1 = t0 + height;

        // Podkładka środkowa
        for (int a = 0; a < 3; a++) { min[a] = 8 - hw; max[a] = 8 + hw; }
        min[sAxis] = t0; max[sAxis] = t1;
        out.add(new float[]{min[0], min[1], min[2], max[0], max[1], max[2]});

        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == side.getAxis()) continue;
            int r = conn.get(side, dir);
            if (r == WireConnections.NONE) continue;
            float reach = switch (r) {
                case WireConnections.SHORT -> 8 - height;
                case WireConnections.EXTENDED -> 8 + height;
                default -> 8;
            };
            for (int a = 0; a < 3; a++) { min[a] = 8 - hw; max[a] = 8 + hw; }
            min[sAxis] = t0; max[sAxis] = t1;
            int dAxis = dir.getAxis().ordinal();
            if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                min[dAxis] = 8 + hw; max[dAxis] = 8 + reach;
            } else {
                min[dAxis] = 8 - reach; max[dAxis] = 8 - hw;
            }
            if (max[dAxis] > min[dAxis]) out.add(new float[]{min[0], min[1], min[2], max[0], max[1], max[2]});
            if (r == WireConnections.RISER) {
                // Pionowy łącznik przy krawędzi: od przewodu na ścianie do środka bloku, gdzie biegnie przewód w obudowie
                float rise = Math.max(hw, 3f);
                if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    min[sAxis] = 8 - rise; max[sAxis] = 16;
                } else {
                    min[sAxis] = 0; max[sAxis] = 8 + rise;
                }
                float w = Math.max(width, 2f);
                if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    min[dAxis] = 16 - w; max[dAxis] = 16;
                } else {
                    min[dAxis] = 0; max[dAxis] = w;
                }
                out.add(new float[]{min[0], min[1], min[2], max[0], max[1], max[2]});
            }
        }
        return out;
    }

    public VoxelShape faceShape(Direction side, WireConnections conn) {
        VoxelShape shape = Shapes.empty();
        for (float[] b : boxes(side, conn)) {
            shape = Shapes.or(shape, Block.box(
                    Math.max(0, b[0]), Math.max(0, b[1]), Math.max(0, b[2]),
                    Math.min(16, b[3]), Math.min(16, b[4]), Math.min(16, b[5])));
        }
        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        WireConnections conn = computeConnections(level, pos, state);
        return shapeCache.computeIfAbsent(new ShapeKey(state, conn), k -> {
            VoxelShape s = Shapes.empty();
            for (Direction side : Direction.values()) {
                if (hasFace(k.state(), side)) s = Shapes.or(s, faceShape(side, k.conn()));
            }
            return s.isEmpty() ? Block.box(7, 7, 7, 9, 9, 9) : s;
        });
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    // ------------------------------------------------------------------ stawianie / podparcie

    public static boolean canAttach(BlockGetter level, BlockPos pos, Direction side) {
        BlockPos sp = pos.relative(side);
        return level.getBlockState(sp).isFaceSturdy(level, sp, side.getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace().getOpposite();
        BlockPos pos = ctx.getClickedPos();
        BlockState existing = ctx.getLevel().getBlockState(pos);
        BlockState base = existing.is(this) ? existing : noFaces(defaultBlockState());
        if (hasFace(base, side) || !canAttach(ctx.getLevel(), pos, side)) return null;
        return base.setValue(face(side), true);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        if (!ctx.getItemInHand().is(asItem())) return false;
        Direction side = ctx.getClickedFace().getOpposite();
        return !hasFace(state, side) && canAttach(ctx.getLevel(), ctx.getClickedPos(), side);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (hasFace(state, side) && canAttach(level, pos, side)) return true;
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (hasFace(state, dir) && !canAttach(level, pos, dir)) {
            BlockState ns = state.setValue(face(dir), false);
            if (faceCount(ns) == 0) return Blocks.AIR.defaultBlockState();
            if (level instanceof Level lvl && !lvl.isClientSide) {
                popResource(lvl, pos, new ItemStack(this));
            }
            return ns;
        }
        return state;
    }

    /** Niszczenie przez gracza usuwa tylko przewód, w który celuje (jeśli jest ich kilka). */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (faceCount(state) <= 1) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        Direction side = pickFace(level, pos, state, player);
        level.setBlock(pos, state.setValue(face(side), false), Block.UPDATE_ALL);
        if (!level.isClientSide && !player.isCreative()) {
            popResource(level, pos, new ItemStack(this));
        }
        return false;
    }

    private Direction pickFace(Level level, BlockPos pos, BlockState state, Player player) {
        WireConnections conn = computeConnections(level, pos, state);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0f).scale(8.0));
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction side : Direction.values()) {
            if (!hasFace(state, side)) continue;
            if (best == null) best = side;
            BlockHitResult hit = faceShape(side, conn).clip(start, end, pos);
            if (hit != null) {
                double d = hit.getLocation().distanceToSqr(start);
                if (d < bestDist) {
                    bestDist = d;
                    best = side;
                }
            }
        }
        return best == null ? Direction.DOWN : best;
    }

    // ------------------------------------------------------------------ sieć

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide) return;
        if (!oldState.is(state.getBlock()) || facesChanged(oldState, state)) {
            onWireChanged(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            onWireRemoved(level, pos);
        }
    }

    private static boolean facesChanged(BlockState a, BlockState b) {
        for (Direction d : Direction.values()) if (hasFace(a, d) != hasFace(b, d)) return true;
        return false;
    }

    /** Wywoływane, gdy przewód powstał lub zmienił ściany. */
    protected void onWireChanged(Level level, BlockPos pos) {}

    /** Wywoływane po usunięciu przewodu. */
    protected void onWireRemoved(Level level, BlockPos pos) {}

    /** Pozycje sąsiadów po przekątnej (narożniki zewnętrzne). */
    public static java.util.List<BlockPos> diagonals(BlockPos pos) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>(12);
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            for (int j = i + 1; j < dirs.length; j++) {
                if (dirs[i].getAxis() != dirs[j].getAxis()) out.add(pos.relative(dirs[i]).relative(dirs[j]));
            }
        }
        return out;
    }
}
