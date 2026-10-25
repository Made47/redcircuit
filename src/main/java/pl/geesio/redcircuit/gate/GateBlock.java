package pl.geesio.redcircuit.gate;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Płaska bramka logiczna leżąca na bloku. FACING = kierunek wyjścia.
 */
public class GateBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Stan wewnętrzny (zatrzaski). */
    public static final BooleanProperty STATE = BooleanProperty.create("state");
    /** Poprzednia wartość wejścia (wykrywanie zbocza). */
    public static final BooleanProperty INPUT = BooleanProperty.create("input");
    /** Komórka stanu: wyjście "odliczania" po lewej (inna tekstura); zatrzask RS: prawe wejście ustawia. */
    public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");
    /** Wejścia wyłączone w oknie (tylko bramki, w których da się je wyłączyć) - każde ma własną teksturę. */
    public static final BooleanProperty DISABLED_L = BooleanProperty.create("disabled_left");
    public static final BooleanProperty DISABLED_B = BooleanProperty.create("disabled_back");
    public static final BooleanProperty DISABLED_R = BooleanProperty.create("disabled_right");
    /** Okres timera (indeks w TIMER_TICKS). */
    public static final IntegerProperty INTERVAL = IntegerProperty.create("interval", 0, 7);

    public static final int[] TIMER_TICKS = {5, 10, 20, 40, 60, 100, 200, 400};

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    private final GateType type;

    /** Typ przekazywany do createBlockStateDefinition (wywoływanego w konstruktorze Block, zanim ustawimy pole). */
    private static GateType pendingType;

    public GateBlock(Properties properties, GateType type) {
        super(capture(properties, type));
        this.type = type;
        BlockState def = stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(STATE, false)
                .setValue(INPUT, false)
                .setValue(INTERVAL, 2)
                .setValue(MIRRORED, false);
        registerDefaultState(withDisabled(def, type.defaultDisabled()));
    }

    private static Properties capture(Properties properties, GateType type) {
        pendingType = type;
        return properties;
    }

    /**
     * Czy przewód może się podłączyć do bramki od strony {@code side} (kierunek od bramki do przewodu):
     * tylko wyjścia i włączone wejścia - strony bez funkcji i wyłączone wejścia zostają rozłączone.
     */
    public static boolean connectsOn(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof GateBlock gate) || !side.getAxis().isHorizontal()) return false;
        GateType t = gate.getGateType();
        Direction f = state.getValue(FACING);
        int bit = side == f ? GateType.OUT_F
                : side == f.getCounterClockWise() ? GateType.IN_L
                : side == f.getOpposite() ? GateType.IN_B : GateType.IN_R;
        int mask = (t.inputs() | t.extraOutputs(state.getValue(MIRRORED))) & ~disabledMask(state);
        return (mask & bit) != 0;
    }

    /** Wyłączone wejścia zapisane w stanie bloku (bity GateType.IN_L/IN_B/IN_R). */
    public static int disabledMask(BlockState state) {
        int m = 0;
        if (state.hasProperty(DISABLED_L) && state.getValue(DISABLED_L)) m |= GateType.IN_L;
        if (state.hasProperty(DISABLED_B) && state.getValue(DISABLED_B)) m |= GateType.IN_B;
        if (state.hasProperty(DISABLED_R) && state.getValue(DISABLED_R)) m |= GateType.IN_R;
        return m;
    }

    public static BlockState withDisabled(BlockState state, int mask) {
        if (state.hasProperty(DISABLED_L)) state = state.setValue(DISABLED_L, (mask & GateType.IN_L) != 0);
        if (state.hasProperty(DISABLED_B)) state = state.setValue(DISABLED_B, (mask & GateType.IN_B) != 0);
        if (state.hasProperty(DISABLED_R)) state = state.setValue(DISABLED_R, (mask & GateType.IN_R) != 0);
        return state;
    }

    public GateType getGateType() {
        return type;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateBlockEntity(pos, state);   // każda bramka ma ustawienia z okna
    }

    /** Sekwencer: krok 0..3 zapisany w bitach STATE/INPUT; wyjście kolejno: przód, prawo, tył, lewo. */
    private static int seqStep(BlockState state) {
        return (state.getValue(STATE) ? 2 : 0) | (state.getValue(INPUT) ? 1 : 0);
    }

    private static Direction seqOutput(BlockState state) {
        Direction f = state.getValue(FACING);
        return switch (seqStep(state)) {
            case 1 -> f.getClockWise();
            case 2 -> f.getOpposite();
            case 3 -> f.getCounterClockWise();
            default -> f;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, STATE, INPUT, INTERVAL, MIRRORED);
        GateType t = type != null ? type : pendingType;
        if (t != null) {
            int sw = t.switchableSides();
            if ((sw & GateType.IN_L) != 0) builder.add(DISABLED_L);
            if ((sw & GateType.IN_B) != 0) builder.add(DISABLED_B);
            if ((sw & GateType.IN_R) != 0) builder.add(DISABLED_R);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportRigidBlock(level, pos.below());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (dir == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, dir, neighborPos, neighbor, random);
    }

    // ---------- Redstone ----------

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** Strona ustawiająca zatrzask RS: lewa, albo prawa po zamianie w oknie. */
    private static Direction rsSetSide(BlockState state) {
        Direction f = state.getValue(FACING);
        return state.getValue(MIRRORED) ? f.getClockWise() : f.getCounterClockWise();
    }

    private static boolean rsQuiet(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GateBlockEntity be && be.quiet;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (type == GateType.SEQUENCER) {
            return state.getValue(POWERED) && direction == seqOutput(state).getOpposite() ? 15 : 0;
        }
        if (type == GateType.RS_LATCH) {
            // Zatrzask RS: zawsze świeci dokładnie jedno wyjście - przód (stan ustawiony) albo tył (skasowany).
            // Wejście aktywnego stanu też świeci (strona "set" z przodem, "reset" z tyłem), jak w klasycznych
            // bramkach - chyba że w oknie wybrano tryb "tylko przód/tył".
            Direction f = state.getValue(FACING);
            boolean set = state.getValue(STATE);
            Direction out = direction.getOpposite();
            if (out == f) return set ? 15 : 0;
            if (out == f.getOpposite()) return set ? 0 : 15;
            if (out.getAxis().isHorizontal() && !rsQuiet(level, pos)) {
                Direction setSide = rsSetSide(state);
                if (out == setSide) return set ? 15 : 0;
                if (out == setSide.getOpposite()) return set ? 0 : 15;
            }
            return 0;
        }
        if (type == GateType.TOGGLE) {
            // Przerzutnik T: dwa wyjścia na zmianę - przód (stan 1) albo tył (stan 0)
            Direction f = state.getValue(FACING);
            boolean st = state.getValue(STATE);
            if (direction == f.getOpposite()) return st ? 15 : 0;
            if (direction == f) return st ? 0 : 15;
            return 0;
        }
        if (type == GateType.RANDOMIZER) {
            // Generator losowy: przód = POWERED, lewo = STATE, prawo = INPUT - każde losowane osobno
            Direction f = state.getValue(FACING);
            Direction out = direction.getOpposite();
            if (out == f) return state.getValue(POWERED) ? 15 : 0;
            if (out == f.getCounterClockWise()) return state.getValue(STATE) ? 15 : 0;
            if (out == f.getClockWise()) return state.getValue(INPUT) ? 15 : 0;
            return 0;
        }
        if (type == GateType.COUNTER) {
            // Licznik: przód przy maksimum (POWERED), tył na zerze (STATE)
            Direction f = state.getValue(FACING);
            if (direction == f.getOpposite()) return state.getValue(POWERED) ? 15 : 0;
            if (direction == f) return state.getValue(STATE) ? 15 : 0;
            return 0;
        }
        if (type == GateType.BUFFER || type == GateType.NOT) {
            // Bufor / NOT: wynik wychodzi z przodu, z lewej i z prawej (boki można wyłączyć w oknie)
            if (!state.getValue(POWERED)) return 0;
            Direction f = state.getValue(FACING);
            Direction out = direction.getOpposite();
            if (out == f) return 15;
            int dis = disabledMask(state);
            if (out == f.getCounterClockWise()) return (dis & GateType.IN_L) == 0 ? 15 : 0;
            if (out == f.getClockWise()) return (dis & GateType.IN_R) == 0 ? 15 : 0;
            return 0;
        }
        if (type == GateType.TIMER) {
            // Zegar daje impuls z przodu, z lewej i z prawej (tył to wejście "stop")
            return state.getValue(POWERED) && direction.getAxis().isHorizontal()
                    && direction != state.getValue(FACING) ? 15 : 0;
        }
        if (type == GateType.STATE_CELL && state.getValue(STATE)
                && direction == stateCellSide(state, level, pos).getOpposite()) {
            return 15;   // prawa (albo lewa, z okna ustawień) strona: "w trakcie odliczania"
        }
        return state.getValue(POWERED) && direction == state.getValue(FACING).getOpposite() ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return type == GateType.COUNTER;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof GateBlockEntity be) {
            return Math.min(15, be.count * 15 / Math.max(1, be.max()));
        }
        return 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (!state.canSurvive(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        updateFront(level, pos, state);
    }

    private static boolean input(Level level, BlockPos pos, Direction dir) {
        return level.getSignal(pos.relative(dir), dir) > 0;
    }

    private void updateFront(Level level, BlockPos pos, BlockState state) {
        if (type == GateType.SEQUENCER || type == GateType.STATE_CELL || type == GateType.TIMER || type == GateType.RS_LATCH
                || type == GateType.TOGGLE || type == GateType.BUFFER || type == GateType.NOT
                || type == GateType.RANDOMIZER || type == GateType.COUNTER) {
            // Te bramki mają wyjścia na kilku stronach
            for (Direction d : Direction.Plane.HORIZONTAL) updateSide(level, pos, d);
            return;
        }
        updateSide(level, pos, state.getValue(FACING));
    }

    private void updateSide(Level level, BlockPos pos, Direction side) {
        BlockPos front = pos.relative(side);
        level.neighborChanged(front, this, null);
        level.updateNeighborsAtExceptFromFacing(front, this, side.getOpposite(), null);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        boolean l = input(level, pos, facing.getCounterClockWise());
        boolean b = input(level, pos, facing.getOpposite());
        // Sekwencer sam zasila tył w kroku 3 - przewód z tyłu oddałby mu ten sygnał i bramka by się
        // zatrzymała (zawiesiła). Gdy wyjście jest właśnie z tyłu, tylne wejście ignorujemy.
        if (type == GateType.SEQUENCER && state.getValue(POWERED) && seqOutput(state) == facing.getOpposite()) {
            b = false;
        }
        boolean r = input(level, pos, facing.getClockWise());
        // Wejścia wyłączone w oknie mają wartość neutralną (nie wpływają na wynik)
        int dis = type.canDisableInputs() ? disabledMask(state) : 0;
        if (dis != 0) {
            boolean n = type.neutralInput();
            if ((dis & GateType.IN_L) != 0) l = n;
            if ((dis & GateType.IN_B) != 0) b = n;
            if ((dis & GateType.IN_R) != 0) r = n;
        }

        BlockState ns = state;
        int delay = -1;
        switch (type) {
            case TIMER -> {
                if (b) {
                    ns = state.setValue(POWERED, false); // zatrzymany
                } else if (state.getValue(POWERED)) {
                    ns = state.setValue(POWERED, false);
                    delay = Math.max(1, interval(level, pos, state) - 2);
                } else {
                    ns = state.setValue(POWERED, true);
                    delay = 2;
                    tickSound(level, pos, 1.0f);
                }
            }
            case PULSE -> {
                boolean rising = b && !state.getValue(INPUT);
                ns = state.setValue(INPUT, b);
                if (rising) {
                    ns = ns.setValue(POWERED, true);
                    delay = 2;
                } else if (state.getValue(POWERED)) {
                    ns = ns.setValue(POWERED, false);
                }
            }
            case RANDOMIZER -> {
                if (b) {
                    ns = state.setValue(POWERED, random.nextBoolean())
                            .setValue(STATE, random.nextBoolean())
                            .setValue(INPUT, random.nextBoolean());
                    delay = 4;
                } else {
                    ns = state.setValue(POWERED, false).setValue(STATE, false).setValue(INPUT, false);
                }
            }
            case TOGGLE -> {
                // Zbocze narastające na lewym albo prawym wejściu przełącza wyjście przód <-> tył
                if (level.getBlockEntity(pos) instanceof GateBlockEntity be) {
                    boolean rising = (l && !be.lastL) || (r && !be.lastR);
                    be.lastL = l;
                    be.lastR = r;
                    be.setChanged();
                    if (rising) {
                        boolean st = !state.getValue(STATE);
                        ns = state.setValue(STATE, st).setValue(POWERED, st);
                    }
                }
            }
            case RS_LATCH -> {
                // Lewe wejście ustawia (przód), prawe kasuje (tył) - albo odwrotnie po zamianie stron w oknie.
                // Strona, która sama świeci (aktywny stan), nie jest czytana jako wejście - inaczej przewód
                // oddałby bramce jej własny sygnał. W trybie "tylko przód/tył" boki nie świecą, więc czytamy oba.
                boolean st = state.getValue(STATE);
                boolean mir = state.getValue(MIRRORED);
                boolean sIn = mir ? r : l, rIn = mir ? l : r;
                boolean quiet = rsQuiet(level, pos);
                boolean setIn = sIn && (quiet || !st);
                boolean resetIn = rIn && (quiet || st);
                if (setIn && !resetIn) st = true;
                else if (resetIn && !setIn) st = false;
                ns = state.setValue(STATE, st).setValue(POWERED, st);
            }
            case SEQUENCER -> {
                if (b) {
                    ns = state.setValue(POWERED, false);   // wstrzymany sygnałem z tyłu
                } else {
                    if (state.getValue(POWERED)) {
                        int step = (seqStep(state) + 1) & 3;
                        ns = state.setValue(STATE, (step & 2) != 0).setValue(INPUT, (step & 1) != 0);
                        tickSound(level, pos, step == 0 ? 1.3f : 1.1f);
                    } else {
                        ns = state.setValue(POWERED, true);
                    }
                    delay = interval(level, pos, state);
                }
            }
            case COUNTER -> {
                if (level.getBlockEntity(pos) instanceof GateBlockEntity be) {
                    int max = be.max();
                    int before = be.count;
                    if (l && !be.lastL) be.count = Math.min(max, be.count + 1);
                    if (r && !be.lastR) be.count = Math.max(0, be.count - 1);
                    if (be.count != before) tickSound(level, pos, be.count >= max ? 1.5f : 1.2f);
                    be.lastL = l;
                    be.lastB = b;
                    be.lastR = r;
                    be.sync();
                    ns = state.setValue(POWERED, be.count >= max).setValue(STATE, be.count == 0);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
            }
            case STATE_CELL -> {
                if (level.getBlockEntity(pos) instanceof GateBlockEntity be) {
                    long now = level.getGameTime();
                    if (b && !be.lastB && !state.getValue(STATE)) {
                        // start odliczania
                        ns = state.setValue(STATE, true).setValue(POWERED, false);
                        be.due = now + be.interval;
                        delay = be.interval;
                    } else if (state.getValue(STATE) && now >= be.due) {
                        // koniec odliczania: impuls z przodu
                        ns = state.setValue(STATE, false).setValue(POWERED, true);
                        delay = 2;
                        tickSound(level, pos, 0.9f);
                    } else if (state.getValue(STATE)) {
                        delay = (int) Math.max(1, be.due - now);
                    } else if (state.getValue(POWERED)) {
                        ns = state.setValue(POWERED, false);
                    }
                    be.lastB = b;
                    be.setChanged();
                }
            }
            case SYNCHRONIZER -> {
                if (level.getBlockEntity(pos) instanceof GateBlockEntity be) {
                    if (b) {
                        be.latchL = false;
                        be.latchR = false;
                    } else {
                        if (l && !be.lastL) be.latchL = true;
                        if (r && !be.lastR) be.latchR = true;
                    }
                    be.lastL = l;
                    be.lastR = r;
                    if (be.latchL && be.latchR) {
                        be.latchL = false;
                        be.latchR = false;
                        ns = state.setValue(POWERED, true);
                        delay = 2;
                    } else if (state.getValue(POWERED)) {
                        ns = state.setValue(POWERED, false);
                    }
                    be.setChanged();
                }
            }
            default -> ns = state.setValue(POWERED, type.eval(l, b, r));
        }

        // Najpierw zaplanuj kolejny tick, dopiero potem aktualizuj sąsiadów.
        // Inaczej przewód przed bramką odpowiada aktualizacją, neighborChanged
        // planuje tick za 2 ticki (bo żaden nie jest jeszcze zaplanowany),
        // a nasz dłuższy tick zostaje odrzucony jako duplikat - zegar tyka wtedy co 2 ticki.
        if (delay > 0) {
            level.scheduleTick(pos, this, delay);
        }
        if (ns != state) {
            level.setBlock(pos, ns, Block.UPDATE_ALL);
            if (ns.getValue(POWERED) != state.getValue(POWERED) || type == GateType.SEQUENCER || type == GateType.RS_LATCH
                    || type == GateType.RANDOMIZER || type == GateType.COUNTER
                    || (type == GateType.STATE_CELL && ns.getValue(STATE) != state.getValue(STATE))) {
                updateFront(level, pos, ns);
            }
        }
    }

    /** Okres w tickach: z ustawień bramki (okno), a dla starych bramek bez danych - z właściwości INTERVAL. */
    public static int interval(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof GateBlockEntity be) return Math.max(GateBlockEntity.MIN_INTERVAL, be.interval);
        return TIMER_TICKS[state.getValue(INTERVAL)];
    }

    /** Dźwięk tyknięcia (zegar, sekwencer, komórka stanu, licznik) - chyba że wyciszony w oknie bramki. */
    private static void tickSound(Level level, BlockPos pos, float pitch) {
        if (level.getBlockEntity(pos) instanceof GateBlockEntity be && be.muted) return;
        level.playSound(null, pos, pl.geesio.redcircuit.registry.ModSounds.GATE_TICK.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, pitch);
    }

    /** Bramki z oknem ustawień - wszystkie (opis, podgląd wejść/wyjść, opcje). */
    public boolean configurable() {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (type == GateType.TOGGLE && !player.isSecondaryUseActive()) {
            // Przerzutnik T: PPM przełącza ręcznie, Shift + PPM otwiera okno
            if (!level.isClientSide()) {
                boolean st = !state.getValue(STATE);
                BlockState ns = state.setValue(STATE, st).setValue(POWERED, st);
                level.setBlock(pos, ns, Block.UPDATE_ALL);
                updateFront(level, pos, ns);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, st ? 0.6f : 0.5f);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            pl.geesio.redcircuit.client.GateScreenOpener.open(pos, type);
        }
        return InteractionResult.SUCCESS;
    }

    /** Strona wyjścia "w trakcie odliczania" komórki stanu: prawa, albo lewa po przełączeniu w oknie. */
    private static Direction stateCellSide(BlockState state, BlockGetter level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        return state.getValue(MIRRORED) ? f.getCounterClockWise() : f.getClockWise();
    }

    /** Po postawieniu z zapisanymi ustawieniami (śrubokręt) stan bloku dostaje stronę z danych bramki. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof GateBlockEntity be) {
            BlockState ns = state;
            if (type == GateType.STATE_CELL || type == GateType.RS_LATCH) ns = ns.setValue(MIRRORED, be.mirrored);
            if (type.switchableSides() != 0) ns = withDisabled(ns, be.disabled & type.switchableSides());
            if (ns != state) level.setBlock(pos, ns, Block.UPDATE_ALL);
        }
        // Dane z przedmiotu (np. odwrócone wyjście) wczytują się po postawieniu - odśwież sąsiadów
        for (Direction d : Direction.Plane.HORIZONTAL) updateSide(level, pos, d);
    }

    /** Zastosowanie ustawień z okna (wywoływane po stronie serwera z pakietu). */
    public void applySettings(Level level, BlockPos pos, int interval, int max, boolean resetCount, boolean muted) {
        applySettings(level, pos, interval, max, resetCount, muted, level.getBlockState(pos).getValue(MIRRORED));
    }

    public void applySettings(Level level, BlockPos pos, int interval, int max, boolean resetCount, boolean muted, boolean mirrored) {
        GateBlockEntity cur = gateData(level, pos);
        if (cur == null) return;
        applySettings(level, pos, interval, max, resetCount, muted, mirrored, disabledMask(level.getBlockState(pos)), cur.quiet);
    }

    /** Dane bramki; bramki postawione w starszej wersji (bez danych) dostają je przy pierwszej zmianie ustawień. */
    @org.jetbrains.annotations.Nullable
    private GateBlockEntity gateData(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GateBlockEntity be) return be;
        BlockState st = level.getBlockState(pos);
        if (!st.is(this)) return null;
        GateBlockEntity be = new GateBlockEntity(pos, st);
        level.setBlockEntity(be);
        return level.getBlockEntity(pos) instanceof GateBlockEntity g ? g : null;
    }

    public void applySettings(Level level, BlockPos pos, int interval, int max, boolean resetCount, boolean muted, boolean mirrored,
                              int disabled, boolean quiet) {
        GateBlockEntity be = gateData(level, pos);
        if (be == null) return;
        be.muted = muted;
        boolean outputsChanged = false;
        if (type.switchableSides() != 0) {
            int current = disabledMask(level.getBlockState(pos));
            int mask = disabled & type.switchableSides();
            if (type.canDisableInputs() && mask == type.inputs()) mask = current;   // co najmniej jedno wejście zostaje
            be.disabled = mask;
            if (mask != current) {
                level.setBlock(pos, withDisabled(level.getBlockState(pos), mask), Block.UPDATE_ALL);
                outputsChanged = true;
            }
        }
        if (type == GateType.RS_LATCH && be.quiet != quiet) {
            be.quiet = quiet;
            outputsChanged = true;
        }
        if ((type == GateType.STATE_CELL || type == GateType.RS_LATCH) && be.mirrored != mirrored) {
            be.mirrored = mirrored;
            be.setChanged();
            BlockState st = level.getBlockState(pos).setValue(MIRRORED, mirrored);
            level.setBlock(pos, st, Block.UPDATE_ALL);
            outputsChanged = true;
        }
        if (outputsChanged) {
            be.setChanged();
            for (Direction d : Direction.Plane.HORIZONTAL) updateSide(level, pos, d);
        }
        if (type.hasInterval()) {
            be.interval = Math.clamp(interval, GateBlockEntity.MIN_INTERVAL, GateBlockEntity.MAX_INTERVAL);
            if (type == GateType.STATE_CELL && level.getBlockState(pos).getValue(STATE)) {
                be.due = Math.min(be.due, level.getGameTime() + be.interval);
            }
        }
        if (type == GateType.COUNTER) {
            be.max = Math.clamp(max, 1, GateBlockEntity.MAX_COUNT);
            if (resetCount) be.count = 0;
            be.count = Math.min(be.count, be.max);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        be.sync();
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 1);
    }
}
