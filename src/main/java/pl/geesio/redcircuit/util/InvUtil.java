package pl.geesio.redcircuit.util;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.function.Predicate;

/** Operacje na inwentarzach 1.7.10 (IInventory / ISidedInventory) - odpowiednik IItemHandler z 1.21. */
public final class InvUtil {
    private InvUtil() {}

    /** Inwentarz bloku (podwójna skrzynia jako całość) albo null. */
    public static IInventory inventory(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) return null;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityChest chest) {
            chest.checkForAdjacentChests();
            if (chest.adjacentChestXNeg != null) return new InventoryLargeChest("container.chestDouble", chest.adjacentChestXNeg, chest);
            if (chest.adjacentChestXPos != null) return new InventoryLargeChest("container.chestDouble", chest, chest.adjacentChestXPos);
            if (chest.adjacentChestZNeg != null) return new InventoryLargeChest("container.chestDouble", chest.adjacentChestZNeg, chest);
            if (chest.adjacentChestZPos != null) return new InventoryLargeChest("container.chestDouble", chest, chest.adjacentChestZPos);
            return chest;
        }
        return te instanceof IInventory inv ? inv : null;
    }

    /** Czy blok przyjmuje / oddaje przedmioty (inwentarz albo IItemAcceptor). */
    public static boolean isItemBlock(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) return false;
        TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof IInventory || te instanceof IItemAcceptor;
    }

    /** Sloty dostępne od ściany {@code side}. */
    public static int[] slots(IInventory inv, int side) {
        if (inv instanceof ISidedInventory s) {
            int[] r = s.getAccessibleSlotsFromSide(side);
            return r == null ? new int[0] : r;
        }
        int[] r = new int[inv.getSizeInventory()];
        for (int i = 0; i < r.length; i++) r[i] = i;
        return r;
    }

    /** Ten sam przedmiot (metadane ignorowane tylko dla przedmiotów z wytrzymałością) - jak isSameItem w 1.21. */
    public static boolean sameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.getItem() != b.getItem()) return false;
        return a.isItemStackDamageable() || a.getItemDamage() == b.getItemDamage();
    }

    /** Czy stosy można połączyć (przedmiot, metadane, NBT). */
    public static boolean stackable(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    public static ItemStack copy(ItemStack s, int count) {
        if (s == null || count <= 0) return null;
        ItemStack c = s.copy();
        c.stackSize = count;
        return c;
    }

    /**
     * Wkłada stos do bloku od ściany {@code side}. Zwraca resztę (null = wszystko się zmieściło).
     * Najpierw IItemAcceptor (rury), potem zwykły inwentarz.
     */
    public static ItemStack insert(World world, int x, int y, int z, ItemStack stack, ForgeDirection side, boolean simulate) {
        if (stack == null || stack.stackSize <= 0) return null;
        TileEntity te = world.blockExists(x, y, z) ? world.getTileEntity(x, y, z) : null;
        if (te instanceof IItemAcceptor acc) return acc.accept(stack.copy(), side, simulate);
        IInventory inv = inventory(world, x, y, z);
        return inv == null ? stack : insert(inv, stack, side.ordinal(), simulate);
    }

    public static ItemStack insert(IInventory inv, ItemStack stack, int side, boolean simulate) {
        stack = stack.copy();
        int[] slots = slots(inv, side);
        ISidedInventory sided = inv instanceof ISidedInventory s ? s : null;
        int limit = Math.min(inv.getInventoryStackLimit(), stack.getMaxStackSize());
        boolean changed = false;
        // najpierw dopełnij istniejące stosy, potem puste sloty
        for (int pass = 0; pass < 2 && stack.stackSize > 0; pass++) {
            for (int slot : slots) {
                if (stack.stackSize <= 0) break;
                ItemStack cur = inv.getStackInSlot(slot);
                if (pass == 0 ? cur == null : cur != null) continue;
                if (!inv.isItemValidForSlot(slot, stack)) continue;
                if (sided != null && !sided.canInsertItem(slot, stack, side)) continue;
                if (cur == null) {
                    int n = Math.min(limit, stack.stackSize);
                    if (!simulate) inv.setInventorySlotContents(slot, copy(stack, n));
                    stack.stackSize -= n;
                    changed = true;
                } else if (stackable(cur, stack) && cur.stackSize < limit) {
                    int n = Math.min(limit - cur.stackSize, stack.stackSize);
                    if (!simulate) cur.stackSize += n;
                    stack.stackSize -= n;
                    changed = true;
                }
            }
        }
        if (changed && !simulate) inv.markDirty();
        return stack.stackSize > 0 ? stack : null;
    }

    /** Ile sztuk z {@code stack} zmieści się w bloku. */
    public static int fits(World world, int x, int y, int z, ItemStack stack, ForgeDirection side) {
        ItemStack rest = insert(world, x, y, z, stack, side, true);
        return stack.stackSize - (rest == null ? 0 : rest.stackSize);
    }

    /** Podgląd slotu do wyciągnięcia (null, gdy nic nie można wyjąć). */
    public static ItemStack peek(IInventory inv, int slot, int side) {
        ItemStack s = inv.getStackInSlot(slot);
        if (s == null || s.stackSize <= 0) return null;
        if (inv instanceof ISidedInventory sided && !sided.canExtractItem(slot, s, side)) return null;
        return s;
    }

    /** Wyjmuje do {@code max} sztuk ze slotu. */
    public static ItemStack extract(IInventory inv, int slot, int max, int side) {
        ItemStack s = peek(inv, slot, side);
        if (s == null || max <= 0) return null;
        ItemStack got = inv.decrStackSize(slot, Math.min(max, s.stackSize));
        inv.markDirty();
        return got;
    }

    /** Pierwszy pasujący stos (do {@code max} sztuk) wyjęty z inwentarza od ściany {@code side}. */
    public static ItemStack extractFirst(IInventory inv, int side, Predicate<ItemStack> match, int max) {
        for (int slot : slots(inv, side)) {
            ItemStack s = peek(inv, slot, side);
            if (s != null && match.test(s)) return extract(inv, slot, max, side);
        }
        return null;
    }

    /** Liczba sztuk pasujących do wzorca w slotach dostępnych od ściany {@code side}. */
    public static int count(IInventory inv, int side, ItemStack pattern) {
        int n = 0;
        for (int slot : slots(inv, side)) {
            ItemStack s = inv.getStackInSlot(slot);
            if (stackable(s, pattern)) n += s.stackSize;
        }
        return n;
    }

    /** Wyrzuca przedmiot na środek bloku (z ruchem w kierunku {@code motion}, jeśli podany). */
    public static void spawn(World world, int x, int y, int z, ItemStack stack, ForgeDirection motion) {
        if (stack == null || stack.stackSize <= 0 || world.isRemote) return;
        EntityItem e = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack);
        if (motion != null && motion != ForgeDirection.UNKNOWN) {
            e.motionX = motion.offsetX * 0.15;
            e.motionY = motion.offsetY * 0.15;
            e.motionZ = motion.offsetZ * 0.15;
        } else {
            e.motionX = e.motionY = e.motionZ = 0;
        }
        world.spawnEntityInWorld(e);
    }

    /** Wyrzuca całą zawartość inwentarza (przy niszczeniu bloku). */
    public static void dropAll(World world, int x, int y, int z, IInventory inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlotOnClosing(i);
            if (s != null) spawnRandom(world, x, y, z, s);
        }
    }

    public static void spawnRandom(World world, int x, int y, int z, ItemStack s) {
        if (world.isRemote || s == null) return;
        float dx = world.rand.nextFloat() * 0.8F + 0.1F, dy = world.rand.nextFloat() * 0.8F + 0.1F, dz = world.rand.nextFloat() * 0.8F + 0.1F;
        EntityItem e = new EntityItem(world, x + dx, y + dy, z + dz, s);
        e.motionX = world.rand.nextGaussian() * 0.05;
        e.motionY = world.rand.nextGaussian() * 0.05 + 0.2;
        e.motionZ = world.rand.nextGaussian() * 0.05;
        world.spawnEntityInWorld(e);
    }

    /** Czy przez blok można wyrzucić przedmiot (brak kolizji). */
    public static boolean open(World world, int x, int y, int z) {
        return world.getBlock(x, y, z).getCollisionBoundingBoxFromPool(world, x, y, z) == null;
    }
}
