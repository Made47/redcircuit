package pl.geesio.redcircuit.wire;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Pos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rozwiązywanie sieci przewodów (port z wersji 1.21).
 * <p>
 * Węzeł sieci to (pozycja, kanał). Przewody ze stopu sygnałowego i izolowane mają jeden węzeł (kanał -1),
 * kabel wiązkowy ma 16 węzłów (kanały 0..15). Spójna składowa grafu dostaje jeden poziom sygnału
 * = maksimum z zewnętrznych wejść redstone. Przy spadku sygnału sieć jest najpierw gaszona
 * (żeby pył redstone zasilany przez przewód nie "zatrzasnął" sygnału).
 */
public final class WireNetwork {
    private WireNetwork() {}

    private static final int MAX_NODES = 16384;
    private static final int MAX_SOLVES_PER_NODE = 3;

    private static boolean emitting = true;
    private static boolean busy = false;
    private static final ArrayDeque<Pos> PENDING = new ArrayDeque<>();
    private static Set<Pos> currentComponent = Collections.emptySet();

    /** Węzeł: pozycja, kanał (-1 = zwykły sygnał) i ściana przewodu-części ForgeMultipart (-1 = przewód-blok). */
    private static final class Node {
        final Pos pos;
        final int ch;
        final int face;

        Node(Pos pos, int ch, int face) {
            this.pos = pos;
            this.ch = ch;
            this.face = face;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Node n && n.ch == ch && n.face == face && n.pos.equals(pos);
        }

        @Override
        public int hashCode() {
            return (pos.hashCode() * 31 + ch) * 7 + face;
        }
    }

    /** Rodzaj przewodu w węźle (blok albo część). */
    private static Integer typeAt(World world, Pos p, int face) {
        return face < 0 ? WireTypes.typeAt(world, p.x, p.y, p.z) : PartWires.wireAt(world, p, face);
    }

    /** Gdy false, przewody nie oddają sygnału (używane podczas liczenia wejść). */
    public static boolean isEmitting() {
        return emitting;
    }

    public static void update(World world, int x, int y, int z) {
        if (world.isRemote) return;
        Pos pos = new Pos(x, y, z);
        if (busy) {
            if (!currentComponent.contains(pos)) PENDING.add(pos);
            return;
        }
        busy = true;
        try {
            Map<Node, Integer> solveCount = new HashMap<>();
            solveAt(world, pos, solveCount);
            int guard = 0;
            while (!PENDING.isEmpty() && guard++ < 20000) {
                solveAt(world, PENDING.poll(), solveCount);
            }
        } finally {
            PENDING.clear();
            currentComponent = Collections.emptySet();
            emitting = true;
            busy = false;
        }
    }

    /** Po usunięciu przewodu - przelicza sąsiednie sieci i odświeża bloki obok. */
    public static void onWireRemoved(World world, int x, int y, int z, Block block) {
        Pos pos = new Pos(x, y, z);
        updateNearbyWires(world, pos);
        world.notifyBlocksOfNeighborChange(x, y, z, block);
        for (ForgeDirection d : Dirs.ALL) {
            world.notifyBlocksOfNeighborChange(x + d.offsetX, y + d.offsetY, z + d.offsetZ, block);
        }
    }

    /** Po postawieniu przewodu lub zmianie ścian, na których leży. */
    public static void onWireChanged(World world, int x, int y, int z) {
        update(world, x, y, z);
        updateNearbyWires(world, new Pos(x, y, z));
    }

    /** Przelicza sieci przewodów obok, także po przekątnej (narożniki zewnętrzne). */
    private static void updateNearbyWires(World world, Pos pos) {
        List<Pos> around = new ArrayList<>(BlockFaceWire.diagonals(pos));
        for (ForgeDirection d : Dirs.ALL) around.add(pos.offset(d));
        for (Pos np : around) {
            if (!world.blockExists(np.x, np.y, np.z)) continue;
            if (WireTypes.isRedstone(WireTypes.typeAt(world, np.x, np.y, np.z)) || PartWires.faceMask(world, np) != 0) {
                update(world, np.x, np.y, np.z);
            }
        }
    }

    // ------------------------------------------------------------------

    private static void solveAt(World world, Pos pos, Map<Node, Integer> solveCount) {
        if (!world.blockExists(pos.x, pos.y, pos.z)) return;
        Integer type = WireTypes.typeAt(world, pos.x, pos.y, pos.z);
        List<Node> starts = new ArrayList<>();
        if (WireTypes.isSignal(type)) {
            starts.add(new Node(pos, -1, -1));
        } else if (type != null && type == WireTypes.BUNDLED) {
            for (int ch = 0; ch < 16; ch++) starts.add(new Node(pos, ch, -1));
        } else if (PartWires.isHost(world, pos)) {
            // Przewody-części ForgeMultipart: każda ściana to osobny węzeł
            for (int f = 0; f < 6; f++) {
                Integer t = PartWires.wireAt(world, pos, f);
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
            Integer done = solveCount.get(start);
            if (done != null && done >= MAX_SOLVES_PER_NODE) continue;
            List<Node> component = collect(world, start);
            visitedThisCall.addAll(component);
            for (Node n : component) {
                Integer c = solveCount.get(n);
                solveCount.put(n, c == null ? 1 : c + 1);
            }
            solveComponent(world, component);
        }
    }

    private static List<Node> collect(World world, Node start) {
        List<Node> result = new ArrayList<>();
        Set<Node> seen = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && result.size() < MAX_NODES) {
            Node n = queue.poll();
            result.add(n);
            for (Node next : neighbors(world, n)) {
                if (seen.add(next)) queue.add(next);
            }
        }
        return result;
    }

    private static List<Node> neighbors(World world, Node node) {
        List<Node> out = new ArrayList<>(6);
        Pos p = node.pos;
        Integer selfType = typeAt(world, p, node.face);
        if (!WireTypes.isRedstone(selfType)) return out;
        Block block = world.getBlock(p.x, p.y, p.z);

        if (node.face >= 0) {
            // Przewód-część na ścianie node.face
            ForgeDirection side = Dirs.of(node.face);
            int self = selfType;
            for (ForgeDirection dir : Dirs.ALL) {
                if (Dirs.sameAxis(dir, side)) continue;
                Pos np = p.offset(dir);
                Pos sp = np.offset(side);
                if (!world.blockExists(np.x, np.y, np.z) || !world.blockExists(sp.x, sp.y, sp.z)) continue;
                WireLinks.Target t = WireLinks.link(world, p, self, side, dir, f -> {
                    Integer o = PartWires.wireAt(world, p, f);
                    return o != null && WireTypes.compatible(self, o);
                }, true, null);
                if (t.pos != null) addLink(world, out, self, node.ch, t.pos, t.face);
            }
            return out;
        }

        if (block instanceof BlockEncasedWire encased) {
            for (ForgeDirection dir : Dirs.ALL) {
                if (!encased.connects(world, p.x, p.y, p.z, dir)) continue;
                Pos np = p.offset(dir);
                if (!world.blockExists(np.x, np.y, np.z)) continue;
                if (PartWires.isHost(world, np)) {
                    for (ForgeDirection f : Dirs.ALL) {
                        if (!Dirs.sameAxis(f, dir)) addLink(world, out, selfType, node.ch, np, f.ordinal());
                    }
                } else {
                    addLink(world, out, selfType, node.ch, np, -1);
                }
            }
            return out;
        }

        if (!(block instanceof BlockFaceWire wire)) return out;
        int faces = BlockFaceWire.faces(world, p.x, p.y, p.z);
        for (ForgeDirection side : Dirs.ALL) {
            if ((faces & (1 << side.ordinal())) == 0) continue;
            for (ForgeDirection dir : Dirs.ALL) {
                if (Dirs.sameAxis(dir, side)) continue;
                Pos np = p.offset(dir);
                Pos sp = np.offset(side);
                if (!world.blockExists(np.x, np.y, np.z) || !world.blockExists(sp.x, sp.y, sp.z)) continue;
                WireLinks.Target t = wire.target(world, p, side, dir);
                if (t.link == WireLinks.Link.INTERNAL) continue;   // ten sam blok = ten sam węzeł
                if (t.pos != null) addLink(world, out, selfType, node.ch, t.pos, t.face);
            }
        }
        return out;
    }

    private static void addLink(World world, List<Node> out, int selfType, int ch, Pos lp, int face) {
        Node next = link(selfType, ch, typeAt(world, lp, face), lp, face);
        if (next != null && !out.contains(next)) out.add(next);
    }

    /** Węzeł, do którego prowadzi połączenie z przewodem rodzaju {@code t} (lub null, gdy nie przewodzi). */
    private static Node link(int selfType, int ch, Integer t, Pos lp, int face) {
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

    private static void solveComponent(World world, List<Node> nodes) {
        Set<Pos> positions = new HashSet<>();
        for (Node n : nodes) positions.add(n.pos);

        int[] input = computeInput(world, nodes);
        int target = input[0];
        int reliable = input[1];   // wejścia bez pyłu redstone (pył może być zasilany przez nas)
        int maxCurrent = 0;
        boolean changed = false;
        for (Node n : nodes) {
            int cur = getValue(world, n);
            if (cur != target) changed = true;
            maxCurrent = Math.max(maxCurrent, cur);
        }
        boolean suspicious = target > reliable && maxCurrent > 0 && target <= maxCurrent;
        if (!changed && !suspicious) return;

        Set<Pos> previous = currentComponent;
        currentComponent = positions;
        try {
            if (reliable < maxCurrent && (target < maxCurrent || suspicious)) {
                // Faza 1: obniż sieć do pewnego poziomu, żeby sprzężenia zwrotne zniknęły.
                setAll(world, nodes, reliable);
                notifyAll(world, nodes, positions);
                target = computeInput(world, nodes)[0];
                if (target == reliable) return;
            }
            setAll(world, nodes, target);
            notifyAll(world, nodes, positions);
        } finally {
            currentComponent = previous;
        }
    }

    /** {najsilniejsze wejście, najsilniejsze wejście z pominięciem pyłu redstone}. */
    private static int[] computeInput(World world, List<Node> nodes) {
        int best = 0, bestReliable = 0;
        emitting = false;
        try {
            Map<Pos, Integer> bundledCache = BundledIO.active() ? new HashMap<>() : null;
            for (Node n : nodes) {
                if (n.ch >= 0) {
                    if (bundledCache == null) continue;
                    Integer mask = bundledCache.get(n.pos);
                    if (mask == null) {
                        mask = bundledInput(world, n.pos);
                        bundledCache.put(n.pos, mask);
                    }
                    if ((mask & (1 << n.ch)) != 0) return new int[]{15, 15};
                    continue;
                }
                for (ForgeDirection d : Dirs.ALL) {
                    Pos np = n.pos.offset(d);
                    if (!world.blockExists(np.x, np.y, np.z)) continue;
                    if (WireTypes.isRedstone(WireTypes.typeAt(world, np.x, np.y, np.z))) continue;
                    int s = world.getIndirectPowerLevelTo(np.x, np.y, np.z, d.ordinal());
                    if (s > best) best = s;
                    if (world.getBlock(np.x, np.y, np.z) != Blocks.redstone_wire && s > bestReliable) bestReliable = s;
                    if (bestReliable >= 15) return new int[]{15, 15};
                }
            }
        } finally {
            emitting = true;
        }
        return new int[]{best, bestReliable};
    }

    private static int bundledInput(World world, Pos pos) {
        int mask = 0;
        for (ForgeDirection d : Dirs.ALL) {
            Pos np = pos.offset(d);
            if (!world.blockExists(np.x, np.y, np.z) || WireTypes.typeAt(world, np.x, np.y, np.z) != null) continue;
            mask |= BundledIO.input(world, np, d.getOpposite());
        }
        return mask;
    }

    private static int getValue(World world, Node n) {
        if (n.face >= 0) {
            PartWires.Host h = PartWires.host;
            return h == null ? 0 : h.getSignal(world, n.pos.x, n.pos.y, n.pos.z, n.face, n.ch);
        }
        TileWire t = BlockFaceWire.tile(world, n.pos.x, n.pos.y, n.pos.z);
        if (t == null) return 0;
        return n.ch < 0 ? t.power : t.channels[n.ch];
    }

    private static void setAll(World world, List<Node> nodes, int value) {
        for (Node n : nodes) {
            if (n.face >= 0) {
                PartWires.Host h = PartWires.host;
                if (h != null) h.setSignal(world, n.pos.x, n.pos.y, n.pos.z, n.face, n.ch, value);
                continue;
            }
            TileWire t = BlockFaceWire.tile(world, n.pos.x, n.pos.y, n.pos.z);
            if (t == null) continue;
            if (n.ch < 0) {
                if (t.power != value) {
                    t.power = value;
                    t.markDirty();
                    // Tylko stop sygnałowy zmienia wygląd przy zasileniu
                    if (world.getBlock(n.pos.x, n.pos.y, n.pos.z) instanceof BlockRedWire red && !red.isInsulated()) {
                        world.markBlockForUpdate(n.pos.x, n.pos.y, n.pos.z);
                    }
                }
            } else if (t.channels[n.ch] != value) {
                t.channels[n.ch] = (byte) value;
                t.markDirty();
            }
        }
    }

    private static void notifyAll(World world, List<Node> nodes, Set<Pos> positions) {
        PartWires.Host h = PartWires.host;
        if (h != null) {
            Set<Node> done = new HashSet<>();
            for (Node n : nodes) {
                if (n.face >= 0 && done.add(new Node(n.pos, 0, n.face))) h.notifyChanged(world, n.pos.x, n.pos.y, n.pos.z, n.face);
            }
        }
        for (Pos pos : positions) {
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            Integer type = WireTypes.typeAt(world, pos.x, pos.y, pos.z);
            if (type != null && type == WireTypes.BUNDLED) {
                // urządzenia innych modów (komputery) odczytują kabel przy aktualizacji sąsiada
                if (BundledIO.active()) world.notifyBlocksOfNeighborChange(pos.x, pos.y, pos.z, block);
                continue;
            }
            if (!WireTypes.isSignal(type)) continue;
            world.notifyBlocksOfNeighborChange(pos.x, pos.y, pos.z, block);
            // Przewody silnie zasilają sąsiednie bloki - powiadom też ich sąsiadów.
            for (ForgeDirection d : Dirs.ALL) {
                Pos np = pos.offset(d);
                if (positions.contains(np) || !world.blockExists(np.x, np.y, np.z)) continue;
                if (WireTypes.typeAt(world, np.x, np.y, np.z) != null) continue;
                Block nb = world.getBlock(np.x, np.y, np.z);
                if (nb.isNormalCube(world, np.x, np.y, np.z)) {
                    world.notifyBlocksOfNeighborChange(np.x, np.y, np.z, block, d.getOpposite().ordinal());
                }
            }
        }
    }
}
