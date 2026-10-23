package pl.geesio.redcircuit.tube;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.InvUtil;
import pl.geesio.redcircuit.util.Pos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Wyszukiwanie trasy do najbliższego inwentarza, który przyjmie przedmiot (Dijkstra po kosztach rur).
 * Zwraca kierunek pierwszego kroku z bieżącej rury.
 */
public final class TubeRouter {
    private TubeRouter() {}

    private static final int MAX_VISITED = 2048;

    private static final class Entry {
        final Pos pos;
        final ForgeDirection first;
        final int cost;

        Entry(Pos pos, ForgeDirection first, int cost) {
            this.pos = pos;
            this.first = first;
            this.cost = cost;
        }
    }

    public static ForgeDirection route(World world, Pos start, TubeItem item) {
        if (item.target != null) {
            // Przedmiot zamówiony - jedzie tylko do zamawiającego
            ForgeDirection d = search(world, start, item, false);
            if (d != null) return d;
            item.target = null;   // cel nieosiągalny - zachowuj się jak zwykły przedmiot
        }
        // Najpierw z pominięciem strony wejścia (żeby nie zawracać), potem bez ograniczeń
        ForgeDirection d = search(world, start, item, true);
        return d != null ? d : search(world, start, item, false);
    }

    private static ForgeDirection search(World world, Pos start, TubeItem item, boolean skipFrom) {
        Map<Pos, Integer> best = new HashMap<>();
        PriorityQueue<Entry> queue = new PriorityQueue<>(Comparator.comparingInt((Entry e) -> e.cost));
        best.put(start, 0);

        for (ForgeDirection dir : Dirs.ALL) {
            if (skipFrom && dir == item.from) continue;
            if (!BlockTube.connected(world, start.x, start.y, start.z, dir)) continue;
            if (isDestination(world, start, dir, item)) return dir;
            push(world, start.offset(dir), dir, 0, item, queue, best);
        }
        while (!queue.isEmpty() && best.size() < MAX_VISITED) {
            Entry e = queue.poll();
            Integer b = best.get(e.pos);
            if (b != null && e.cost > b) continue;
            for (ForgeDirection dir : Dirs.ALL) {
                if (!BlockTube.connected(world, e.pos.x, e.pos.y, e.pos.z, dir)) continue;
                Pos np = e.pos.offset(dir);
                if (np.equals(start)) continue;
                if (isDestination(world, e.pos, dir, item)) return e.first;
                push(world, np, e.first, e.cost, item, queue, best);
            }
        }
        return null;
    }

    private static void push(World world, Pos np, ForgeDirection first, int cost, TubeItem item,
                             PriorityQueue<Entry> queue, Map<Pos, Integer> best) {
        if (!world.blockExists(np.x, np.y, np.z)) return;
        Block b = world.getBlock(np.x, np.y, np.z);
        if (!(b instanceof BlockTube tube)) return;
        TileEntity te = world.getTileEntity(np.x, np.y, np.z);
        if (!(te instanceof TileTube t) || !TileTube.passable(t.paint, item.color)) return;
        int c = cost + tube.routeCost();
        Integer old = best.get(np);
        if (old == null || c < old) {
            best.put(np, c);
            queue.add(new Entry(np, first, c));
        }
    }

    /** Czy w kierunku {@code dir} od {@code from} jest (nie-rura) inwentarz przyjmujący przedmiot. */
    private static boolean isDestination(World world, Pos from, ForgeDirection dir, TubeItem item) {
        Pos np = from.offset(dir);
        if (!world.blockExists(np.x, np.y, np.z)) return false;
        Block b = world.getBlock(np.x, np.y, np.z);
        if (b instanceof BlockTube) return false;
        if (item.target != null) {
            if (!np.equals(item.target)) return false;
        } else if (b instanceof ITargetOnly) {
            return false;   // Requester/Manager przyjmują tylko przedmioty, które same zamówiły
        }
        if (!InvUtil.isItemBlock(world, np.x, np.y, np.z)) return false;
        ItemStack s = item.stack;
        return InvUtil.fits(world, np.x, np.y, np.z, s, dir.getOpposite()) > 0;
    }
}
