package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.InvUtil;
import pl.geesio.redcircuit.util.Pos;
import pl.geesio.redcircuit.wire.BundledIO;

import java.util.ArrayList;
import java.util.List;

public final class MachineUtil {
    private MachineUtil() {}

    /** Wypycha jeden stos (pierwszy niepusty od slotu {@code start}) przodem maszyny. Zwraca indeks slotu albo -1. */
    public static int pushOne(World world, int x, int y, int z, ForgeDirection facing, IInventory items, int start) {
        int n = items.getSizeInventory();
        for (int k = 0; k < n; k++) {
            int slot = (start + k) % n;
            ItemStack stack = items.getStackInSlot(slot);
            if (stack == null) continue;
            int count = BlockFacing.deliverableCount(world, x, y, z, facing, stack, -1);
            if (count <= 0) continue;
            ItemStack out = items.decrStackSize(slot, count);
            if (out == null) continue;
            BlockFacing.deliver(world, x, y, z, facing, out, -1);
            return slot;
        }
        return -1;
    }

    /** Wkłada stos do inwentarza (wszystkie sloty); zwraca resztę albo null. */
    public static ItemStack insert(IInventory items, ItemStack stack) {
        return InvUtil.insert(items, stack, -1, false);
    }

    /** Czy blok da się rozbić (nie powietrze, nie ciecz, nie skała macierzysta). */
    public static boolean breakable(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        return !b.isAir(world, x, y, z) && !b.getMaterial().isLiquid() && b.getBlockHardness(world, x, y, z) >= 0;
    }

    /** Rozbija blok (z efektem i dźwiękiem) i zwraca jego dropy. */
    public static List<ItemStack> breakBlock(World world, int x, int y, int z) {
        if (!breakable(world, x, y, z)) return new ArrayList<>();
        Block b = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        List<ItemStack> drops = new ArrayList<>(b.getDrops(world, x, y, z, meta, 0));
        world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(b) + (meta << 12));
        world.setBlockToAir(x, y, z);
        return drops;
    }

    /** Odczyt 16 kanałów z kabli wiązkowych dookoła - maska bitowa. */
    public static int bundledInput(World world, int x, int y, int z) {
        int mask = 0;
        Pos pos = new Pos(x, y, z);
        for (ForgeDirection d : Dirs.ALL) {
            int m = BundledIO.cableOutput(world, pos.offset(d), d.getOpposite());
            if (m > 0) mask |= m;
        }
        return mask;
    }
}
