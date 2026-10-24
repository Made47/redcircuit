package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rozwiązywanie sieci przewodów.
 * <p>
 * Węzeł sieci to (pozycja, kanał). Przewody Signal Alloy i izolowane mają jeden węzeł (kanał -1),
 * kabel wiązkowy ma 16 węzłów (kanały 0..15). Spójna składowa grafu dostaje jeden poziom
 * sygnału = maksimum z zewnętrznych wejść redstone.
 * <p>
 * Aby uniknąć "zatrzaśnięcia" przez sprzężenie zwrotne (np. pył redstone zasilany z przewodu
 * oddaje sygnał z powrotem), przy spadku sygnału najpierw cała sieć jest gaszona do 0,
 * sąsiedzi są powiadamiani, a dopiero potem wejścia są liczone ponownie.
 */
public final class WireNetwork {
    private WireNetwork() {}

    private static final int MAX_NODES = 16384;
    private static final int MAX_SOLVES_PER_NODE = 3;

    private static boolean emitting = true;
    private static boolean busy = false;
    private static final ArrayDeque<BlockPos> PENDING = new ArrayDeque<>();
    private static Set<BlockPos> currentComponent = Set.of();

    /** Węzeł: pozycja, kanał (-1 = zwykły sygnał) i ściana przewodu-części (-1 = przewód-blok). */
    private record Node(BlockPos pos, int ch, int face) {}

    /** Rodzaj przewodu w danym węźle (blok albo część CB Multipart). */
    @org.jetbrains.annotations.Nullable
    private static Integer typeAt(net.minecraft.world.level.BlockGetter level, BlockPos pos, int face) {
        return face < 0 ? WireTypes.typeOf(level.getBlockState(pos).getBlock()) : PartWires.wireAt(level, pos, face);
    }

    /** Gdy false, przewody nie oddają sygnału (używane podczas liczenia wejść). */
    public static boolean isEmitting() {
        return emitting;
    }

    public static void update(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        if (busy) {
            if (!currentComponent.contains(pos)) PENDING.add(pos.immutable());
            return;
        }
        busy = true;
        try {
            Map<Node, Integer> solveCount = new HashMap<>();
            solveAt(level, pos.immutable(), solveCount);
            int guard = 0;
            while (!PENDING.isEmpty() && guard++ < 20000) {
                solveAt(level, PENDING.poll(), solveCount);
            }
        } finally {
            PENDING.clear();
            currentComponent = Set.of();
            emitting = true;
            busy = false;
        }
    }

    /** Wywoływane po usunięciu przewodu - przelicza sąsiednie sieci i odświeża bloki obok. */
    public static void onWireRemoved(Level level, BlockPos pos, Block block) {
        updateNearbyWires(level, pos);
        level.updateNeighborsAt(pos, block);
        for (Direction d : Direction.values()) {
            level.updateNeighborsAt(pos.relative(d), block);
        }
    }

    /** Wywoływane po postawieniu przewodu lub zmianie ścian, na których leży. */
    public static void onWireChanged(Level level, BlockPos pos, Block block) {
        update(level, pos);
        updateNearbyWires(level, pos);
    }

    /** Przelicza sieci przewodów obok, także po przekątnej (narożniki zewnętrzne). */
    private static void updateNearbyWires(Level level, BlockPos pos) {
        List<BlockPos> around = new ArrayList<>(FaceWireBlock.diagonals(pos));
        for (Direction d : Direction.values()) around.add(pos.relative(d));
        for (BlockPos np : around) {
            if (!level.isLoaded(np)) continue;
            Block nb = level.getBlockState(np).getBlock();
            if (WireTypes.isRedstone(WireTypes.typeOf(nb)) || PartWires.faceMask(level, np) != 0) {
                update(level, np);
            }
        }
    }

    // ------------------------------------------------------------------

    private static void solveAt(Level level, BlockPos pos, Map<Node, Integer> solveCount) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        List<Node> starts = new ArrayList<>();
        Integer type = WireTypes.typeOf(state.getBlock());
        if (WireTypes.isSignal(type)) {
            starts.add(new Node(pos, -1, -1));
        } else if (type != null && type == WireTypes.BUNDLED) {
            for (int ch = 0; ch < 16; ch++) starts.add(new Node(pos, ch, -1));
        } else if (PartWires.isHost(level, pos)) {
            // Przewody-części CB Multipart: każda ściana to osobny węzeł
            for (int f = 0; f < 6; f++) {
                Integer t = PartWires.wireAt(level, pos, f);
                if (WireTypes.isSignal(t)) starts.add(new Node(pos, -1, f));
                else if (t != null && t == WireTypes.BUNDLED) for (int ch = 0; ch < 16; ch++) starts.add(new Node(pos, ch, f));
            }
            if (starts.isEmpty()) return;
        } else {
            return;
        }

        Set<Node> visitedThisCall = new HashSet<>();
        for (Node start : starts) {
            if (visitedThisCall.contains(start)) continue;
            if (solveCount.getOrDefault(start, 0) >= MAX_SOLVES_PER_NODE) continue;
            List<Node> component = collect(level, start);
            visitedThisCall.addAll(component);
            for (Node n : component) solveCount.merge(n, 1, Integer::sum);
            solveComponent(level, component);
        }
    }

    private static List<Node> collect(Level level, Node start) {
        List<Node> result = new ArrayList<>();
        Set<Node> seen = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && result.size() < MAX_NODES) {
            Node n = queue.poll();
            result.add(n);
            for (Node next : neighbors(level, n)) {
                if (seen.add(next)) queue.add(next);
            }
        }
        return result;
    }

    private static List<Node> neighbors(Level level, Node node) {
        List<Node> out = new ArrayList<>(6);
        BlockState state = level.getBlockState(node.pos);
        Integer selfType = typeAt(level, node.pos, node.face);
        if (!WireTypes.isRedstone(selfType)) return out;

        if (node.face >= 0) {
            // Przewód-część na ścianie node.face
            Direction side = Direction.from3DDataValue(node.face);
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() == side.getAxis()) continue;
                BlockPos np = node.pos.relative(dir);
                if (!level.isLoaded(np) || !level.isLoaded(np.relative(side))) continue;
                WireLinks.Target t = WireLinks.link(level, node.pos, selfType, side, dir, f -> {
                    Integer o = PartWires.wireAt(level, node.pos, f);
                    return o != null && WireTypes.compatible(selfType, o);
                }, true, null);
                addLink(level, out, selfType, node.ch, t.pos(), t.face());
            }
            return out;
        }

        if (state.getBlock() instanceof EncasedWireBlock) {
            // Przewód w obudowie: sąsiedzi w kierunkach, w których ma połączenie
            for (Direction dir : Direction.values()) {
                if (!pl.geesio.redcircuit.util.CableBlock.isConnected(state, dir)) continue;
                BlockPos np = node.pos.relative(dir);
                if (!level.isLoaded(np)) continue;
                if (PartWires.isHost(level, np)) {
                    for (Direction f : Direction.values()) {
                        if (f.getAxis() != dir.getAxis()) addLink(level, out, selfType, node.ch, np, f.get3DDataValue());
                    }
                } else {
                    addLink(level, out, selfType, node.ch, np, -1);
                }
            }
            return out;
        }

        if (!(state.getBlock() instanceof FaceWireBlock wireBlock)) return out;
        for (Direction side : Direction.values()) {
            if (!FaceWireBlock.hasFace(state, side)) continue;
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() == side.getAxis()) continue;
                BlockPos np = node.pos.relative(dir);
                if (!level.isLoaded(np) || !level.isLoaded(np.relative(side))) continue;
                WireLinks.Target t = wireBlock.target(level, node.pos, state, side, dir);
                if (t.link() == FaceWireBlock.Link.INTERNAL) continue;   // ten sam blok = ten sam węzeł
                addLink(level, out, selfType, node.ch, t.pos(), t.face());
            }
        }
        return out;
    }

    private static void addLink(Level level, List<Node> out, int selfType, int ch, @org.jetbrains.annotations.Nullable BlockPos lp, int face) {
        if (lp == null) return;
        Node next = link(selfType, ch, typeAt(level, lp, face), lp, face);
        if (next != null && !out.contains(next)) out.add(next);
    }

    /** Węzeł, do którego prowadzi połączenie z przewodem rodzaju {@code t} (lub null, gdy nie przewodzi). */
    private static Node link(int selfType, int ch, @org.jetbrains.annotations.Nullable Integer t, BlockPos lp, int face) {
        if (!WireTypes.isRedstone(t)) return null;
        if (ch < 0) {
            if (WireTypes.isSignal(t) && WireTypes.compatible(selfType, t)) return new Node(lp, -1, face);
            if (t == WireTypes.BUNDLED && selfType >= 0) return new Node(lp, selfType, face);
        } else {
            if (t == WireTypes.BUNDLED) return new Node(lp, ch, face);
            if (t == ch) return new Node(lp, -1, face);
        }
        return null;
    }

    private static void solveComponent(Level level, List<Node> nodes) {
        Set<BlockPos> positions = new HashSet<>();
        for (Node n : nodes) positions.add(n.pos);

        int[] input = computeInput(level, nodes);
        int target = input[0];
        int reliable = input[1]; // wejścia bez pyłu redstone (pył może być zasilany przez nas)
        int maxCurrent = 0;
        boolean changed = false;
        for (Node n : nodes) {
            int cur = getValue(level, n);
            if (cur != target) changed = true;
            maxCurrent = Math.max(maxCurrent, cur);
        }
        // Pył redstone, który daje najsilniejszy sygnał, może jedynie odbijać nasz własny sygnał.
        boolean suspicious = target > reliable && maxCurrent > 0 && target <= maxCurrent;
        if (!changed && !suspicious) return;

        Set<BlockPos> previous = currentComponent;
        currentComponent = positions;
        try {
            if (reliable < maxCurrent && (target < maxCurrent || suspicious)) {
                // Faza 1: obniż sieć do pewnego poziomu, żeby sprzężenia zwrotne zniknęły.
                setAll(level, nodes, reliable);
                notifyAll(level, nodes, positions);
                target = computeInput(level, nodes)[0];
                if (target == reliable) return;
            }
            setAll(level, nodes, target);
            notifyAll(level, nodes, positions);
        } finally {
            currentComponent = previous;
        }
    }

    /** Zwraca {najsilniejsze wejście, najsilniejsze wejście z pominięciem pyłu redstone}. */
    private static int[] computeInput(Level level, List<Node> nodes) {
        int best = 0;
        int bestReliable = 0;
        emitting = false;
        try {
            Map<BlockPos, Integer> bundledCache = BundledIO.active() ? new HashMap<>() : null;
            for (Node n : nodes) {
                if (n.ch >= 0) {
                    // kabel wiązkowy nie pobiera zwykłego redstone, ale może dostać sygnał wiązkowy z innego moda
                    if (bundledCache == null) continue;
                    int mask = bundledCache.computeIfAbsent(n.pos, p -> bundledInput(level, p));
                    if ((mask & (1 << n.ch)) != 0) return new int[]{15, 15};
                    continue;
                }
                for (Direction d : Direction.values()) {
                    BlockPos np = n.pos.relative(d);
                    if (!level.isLoaded(np)) continue;
                    Block nb = level.getBlockState(np).getBlock();
                    if (WireTypes.isRedstone(WireTypes.typeOf(nb))) continue;
                    int s = level.getSignal(np, d);
                    if (s > best) best = s;
                    if (!(nb instanceof RedStoneWireBlock) && s > bestReliable) bestReliable = s;
                    if (bestReliable >= 15) return new int[]{15, 15};
                }
            }
        } finally {
            emitting = true;
        }
        return new int[]{best, bestReliable};
    }

    /** Maska kanałów, które urządzenia innych modów (np. komputery) podają do kabla wiązkowego w bloku pos. */
    private static int bundledInput(Level level, BlockPos pos) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            BlockPos np = pos.relative(d);
            if (!level.isLoaded(np) || WireTypes.typeOf(level.getBlockState(np).getBlock()) != null) continue;
            mask |= BundledIO.input(level, np, d.getOpposite());
        }
        return mask;
    }

    private static int getValue(Level level, Node n) {
        if (n.face >= 0) {
            PartWires.Host h = PartWires.host;
            return h == null ? 0 : h.getSignal(level, n.pos, n.face, n.ch);
        }
        if (n.ch < 0) {
            BlockState s = level.getBlockState(n.pos);
            return s.hasProperty(RedWireBlock.POWER) && WireTypes.isSignal(WireTypes.typeOf(s.getBlock()))
                    ? s.getValue(RedWireBlock.POWER) : 0;
        }
        BlockEntity be = level.getBlockEntity(n.pos);
        return be instanceof BundledCableBlockEntity bundled ? bundled.getChannel(n.ch) : 0;
    }

    private static void setAll(Level level, List<Node> nodes, int value) {
        for (Node n : nodes) {
            if (n.face >= 0) {
                PartWires.Host h = PartWires.host;
                if (h != null) h.setSignal(level, n.pos, n.face, n.ch, value);
            } else if (n.ch < 0) {
                BlockState s = level.getBlockState(n.pos);
                if (s.hasProperty(RedWireBlock.POWER) && WireTypes.isSignal(WireTypes.typeOf(s.getBlock()))
                        && s.getValue(RedWireBlock.POWER) != value) {
                    level.setBlock(n.pos, s.setValue(RedWireBlock.POWER, value), Block.UPDATE_CLIENTS);
                }
            } else if (level.getBlockEntity(n.pos) instanceof BundledCableBlockEntity bundled) {
                bundled.setChannel(n.ch, value);
            }
        }
    }

    private static void notifyAll(Level level, List<Node> nodes, Set<BlockPos> positions) {
        PartWires.Host h = PartWires.host;
        if (h != null) {
            Set<Node> done = new HashSet<>();
            for (Node n : nodes) {
                if (n.face >= 0 && done.add(new Node(n.pos, 0, n.face))) {
                    h.notifyChanged(level, n.pos, n.face);
                    if (n.ch >= 0 && BundledIO.active()) level.updateNeighborsAt(n.pos, level.getBlockState(n.pos).getBlock());
                }
            }
        }
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            Integer type = WireTypes.typeOf(block);
            if (type != null && type == WireTypes.BUNDLED && BundledIO.active()) {
                // urządzenia innych modów (komputery) odczytują kabel przy aktualizacji sąsiada
                level.updateNeighborsAt(pos, block);
                continue;
            }
            if (!WireTypes.isSignal(type)) continue;
            level.updateNeighborsAt(pos, block);
            // Przewody silnie zasilają sąsiednie bloki - powiadom też ich sąsiadów.
            for (Direction d : Direction.values()) {
                BlockPos np = pos.relative(d);
                if (positions.contains(np) || !level.isLoaded(np)) continue;
                BlockState ns = level.getBlockState(np);
                if (WireTypes.typeOf(ns.getBlock()) != null) continue;
                if (ns.isRedstoneConductor(level, np)) {
                    level.updateNeighborsAtExceptFromFacing(np, block, d.getOpposite(), null);
                }
            }
        }
    }
}
