package pl.geesio.redcircuit.tube;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.InvUtil;
import pl.geesio.redcircuit.util.Pos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Operacje na całej sieci rur (zamawianie przedmiotów dla Requestera i Managera). */
public final class TubeNetwork {
    private TubeNetwork() {}

    private static final int MAX_TUBES = 2048;

    /**
     * Szuka w inwentarzach podłączonych do sieci rur (od {@code startTube}) przedmiotu spełniającego {@code match},
     * wyciąga do {@code maxCount} sztuk i wysyła je rurami do {@code target}. Pomija pozycje z {@code exclude}.
     * Zwraca liczbę wysłanych sztuk.
     */
    public static int fetch(World world, Pos target, Pos startTube, Set<Pos> exclude, Predicate<ItemStack> match, int maxCount) {
        if (!(world.getTileEntity(startTube.x, startTube.y, startTube.z) instanceof TileTube)) return 0;
        Set<Pos> seen = new HashSet<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        seen.add(startTube);
        queue.add(startTube);
        while (!queue.isEmpty() && seen.size() < MAX_TUBES) {
            Pos tp = queue.poll();
            TileEntity tte = world.getTileEntity(tp.x, tp.y, tp.z);
            if (!(tte instanceof TileTube tube)) continue;
            for (ForgeDirection dir : Dirs.ALL) {
                if (!BlockTube.connected(world, tp.x, tp.y, tp.z, dir)) continue;
                Pos np = tp.offset(dir);
                if (np.equals(target) || exclude.contains(np) || seen.contains(np) || !world.blockExists(np.x, np.y, np.z)) continue;
                Block nb = world.getBlock(np.x, np.y, np.z);
                if (nb instanceof BlockTube) {
                    seen.add(np);
                    TileTube t = BlockTube.tile(world, np.x, np.y, np.z);
                    if (t != null && TileTube.passable(t.paint, -1)) queue.add(np);
                    continue;
                }
                if (nb instanceof ITargetOnly) continue;
                IInventory src = InvUtil.inventory(world, np.x, np.y, np.z);
                if (src == null || src instanceof TileTube || !tube.canAccept(-1)) continue;
                int side = dir.getOpposite().ordinal();
                for (int slot : InvUtil.slots(src, side)) {
                    ItemStack peek = InvUtil.peek(src, slot, side);
                    if (peek == null || !match.test(peek)) continue;
                    ItemStack got = InvUtil.extract(src, slot, maxCount, side);
                    if (got == null) continue;
                    tube.insertTargeted(got, dir, target);
                    return got.stackSize;
                }
            }
        }
        return 0;
    }
}
