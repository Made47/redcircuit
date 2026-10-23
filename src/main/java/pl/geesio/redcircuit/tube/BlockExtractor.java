package pl.geesio.redcircuit.tube;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.machine.BlockFacing;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Ekstraktor: na impuls redstone wyciąga stos przedmiotów z inwentarza za sobą i wypycha go do przodu
 * (do rury, inwentarza albo na ziemię). Stawia się go, klikając na inwentarz, z którego ma pobierać.
 */
public class BlockExtractor extends BlockFacing {
    public BlockExtractor(String name) {
        super(name);
    }

    public BlockExtractor() {
        this("extractor");
    }

    /** Czy ten blok może pobrać dany przedmiot (filtr / maszyna sortująca nadpisują). */
    protected boolean accepts(World world, int x, int y, int z, ItemStack stack) {
        return true;
    }

    /** Kolor nadawany przedmiotowi w rurze (-1 = bez koloru). */
    protected int colorFor(World world, int x, int y, int z, ItemStack stack) {
        return -1;
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
        ForgeDirection facing = facing(world, x, y, z);
        ForgeDirection back = facing.getOpposite();
        IInventory src = InvUtil.inventory(world, x + back.offsetX, y + back.offsetY, z + back.offsetZ);
        if (src == null) return;
        int side = facing.ordinal();   // ściana źródła zwrócona do ekstraktora
        for (int slot : InvUtil.slots(src, side)) {
            ItemStack peek = InvUtil.peek(src, slot, side);
            if (peek == null || !accepts(world, x, y, z, peek)) continue;
            int color = colorFor(world, x, y, z, peek);
            int count = deliverableCount(world, x, y, z, facing, InvUtil.copy(peek, Math.min(64, peek.stackSize)), color);
            if (count <= 0) continue;
            ItemStack got = InvUtil.extract(src, slot, count, side);
            if (got == null) continue;
            deliver(world, x, y, z, facing, got, color);
            return;
        }
    }
}
