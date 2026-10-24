package pl.geesio.redcircuit.tube;

import pl.geesio.redcircuit.util.transfer.ItemHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import pl.geesio.redcircuit.util.transfer.IItemHandler;
import pl.geesio.redcircuit.util.transfer.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.util.CableBlock;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Wyszukiwanie trasy (BFS) do najbliższego inwentarza, który przyjmie przedmiot.
 * Zwraca kierunek pierwszego kroku z bieżącej rury.
 */
public final class TubeRouter {
    private TubeRouter() {}

    private static final int MAX_VISITED = 2048;

    private record Entry(BlockPos pos, Direction first, int cost) {}

    @Nullable
    public static Direction route(Level level, BlockPos start, TubeItem item) {
        if (item.target != null) {
            // Przedmiot zamówiony przez Requester - jedzie tylko do niego.
            Direction d = search(level, start, item, false);
            if (d != null) return d;
            item.target = null;   // cel nieosiągalny - zachowuj się jak zwykły przedmiot
        }
        // Najpierw szukaj z pominięciem strony wejścia (żeby nie zawracać), potem bez ograniczeń.
        Direction d = search(level, start, item, true);
        return d != null ? d : search(level, start, item, false);
    }

    /**
     * Najtańsza trasa (Dijkstra): każda rura kosztuje {@link TubeBlock#routeCost()},
     * więc rury restrykcyjne są wybierane tylko wtedy, gdy nie ma innej drogi.
     */
    @Nullable
    private static Direction search(Level level, BlockPos start, TubeItem item, boolean skipFrom) {
        BlockState startState = level.getBlockState(start);
        Map<BlockPos, Integer> best = new HashMap<>();
        PriorityQueue<Entry> queue = new PriorityQueue<>(Comparator.comparingInt(Entry::cost));
        best.put(start, 0);

        for (Direction dir : Direction.values()) {
            if (skipFrom && dir == item.from) continue;
            if (!CableBlock.isConnected(startState, dir)) continue;
            if (isDestination(level, start, dir, item)) return dir;
            push(level, start.relative(dir), dir, 0, item, queue, best);
        }
        while (!queue.isEmpty() && best.size() < MAX_VISITED) {
            Entry e = queue.poll();
            if (e.cost > best.getOrDefault(e.pos, Integer.MAX_VALUE)) continue;
            BlockState s = level.getBlockState(e.pos);
            for (Direction dir : Direction.values()) {
                if (!CableBlock.isConnected(s, dir)) continue;
                BlockPos np = e.pos.relative(dir);
                if (np.equals(start)) continue;
                if (isDestination(level, e.pos, dir, item)) return e.first;
                push(level, np, e.first, e.cost, item, queue, best);
            }
        }
        return null;
    }

    private static void push(Level level, BlockPos np, Direction first, int cost, TubeItem item,
                             PriorityQueue<Entry> queue, Map<BlockPos, Integer> best) {
        if (!level.isLoaded(np)) return;
        BlockState ns = level.getBlockState(np);
        if (!(ns.getBlock() instanceof TubeBlock tube) || !TubeBlock.passable(ns, item.color)) return;
        int c = cost + tube.routeCost();
        if (c < best.getOrDefault(np, Integer.MAX_VALUE)) {
            best.put(np, c);
            queue.add(new Entry(np, first, c));
        }
    }

    /** Czy w kierunku {@code dir} od {@code from} jest (nie-rura) inwentarz przyjmujący przedmiot. */
    private static boolean isDestination(Level level, BlockPos from, Direction dir, TubeItem item) {
        BlockPos np = from.relative(dir);
        if (!level.isLoaded(np)) return false;
        BlockState ns = level.getBlockState(np);
        if (ns.getBlock() instanceof TubeBlock) return false;
        if (item.target != null) {
            if (!np.equals(item.target)) return false;
        } else if (ns.getBlock() instanceof TargetOnly) {
            return false;   // Requester/Manager przyjmują tylko przedmioty, które same zamówiły
        }
        IItemHandler handler = ItemHandlers.wrap(level.getCapability(Capabilities.Item.BLOCK, np, dir.getOpposite()));
        if (handler == null) return false;
        ItemStack rest = ItemHandlerHelper.insertItem(handler, item.stack.copy(), true);
        return rest.getCount() < item.stack.getCount();
    }
}
