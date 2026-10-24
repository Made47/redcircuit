package pl.geesio.redcircuit.tube;

import pl.geesio.redcircuit.util.transfer.ItemHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import pl.geesio.redcircuit.util.transfer.IItemHandler;
import pl.geesio.redcircuit.util.CableBlock;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Operacje na całej sieci rur (zamawianie przedmiotów dla Requestera i Managera). */
public final class TubeNetwork {
    private TubeNetwork() {}

    private static final int MAX_TUBES = 2048;

    /**
     * Szuka w inwentarzach podłączonych do sieci rur (zaczynając od {@code startTube}) przedmiotu
     * spełniającego {@code match}, wyciąga do {@code maxCount} sztuk i wysyła je rurami do {@code target}.
     * Pomija pozycje z {@code exclude}. Zwraca liczbę wysłanych sztuk.
     */
    public static int fetch(Level level, BlockPos target, BlockPos startTube, Set<BlockPos> exclude,
                            Predicate<ItemStack> match, int maxCount) {
        if (!(level.getBlockEntity(startTube) instanceof TubeBlockEntity)) return 0;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(startTube);
        queue.add(startTube);
        while (!queue.isEmpty() && seen.size() < MAX_TUBES) {
            BlockPos tubePos = queue.poll();
            BlockState ts = level.getBlockState(tubePos);
            if (!(level.getBlockEntity(tubePos) instanceof TubeBlockEntity tube)) continue;
            for (Direction dir : Direction.values()) {
                if (!CableBlock.isConnected(ts, dir)) continue;
                BlockPos np = tubePos.relative(dir);
                if (np.equals(target) || exclude.contains(np) || seen.contains(np) || !level.isLoaded(np)) continue;
                BlockState ns = level.getBlockState(np);
                if (ns.getBlock() instanceof TubeBlock) {
                    seen.add(np);
                    if (TubeBlock.passable(ns, -1)) queue.add(np);
                    continue;
                }
                if (ns.getBlock() instanceof TargetOnly) continue;
                IItemHandler src = ItemHandlers.wrap(level.getCapability(Capabilities.Item.BLOCK, np, dir.getOpposite()));
                if (src == null || !tube.canAccept(-1)) continue;
                for (int slot = 0; slot < src.getSlots(); slot++) {
                    ItemStack sim = src.extractItem(slot, maxCount, true);
                    if (sim.isEmpty() || !match.test(sim)) continue;
                    ItemStack got = src.extractItem(slot, Math.min(maxCount, sim.getCount()), false);
                    if (got.isEmpty()) continue;
                    tube.insertTargeted(got, dir, target);
                    return got.getCount();
                }
            }
        }
        return 0;
    }
}
