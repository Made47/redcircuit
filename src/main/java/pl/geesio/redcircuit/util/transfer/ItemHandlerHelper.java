package pl.geesio.redcircuit.util.transfer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

public final class ItemHandlerHelper {
    private ItemHandlerHelper() {}

    /** Wkłada stos do kolejnych slotów; zwraca resztę, która się nie zmieściła. */
    public static ItemStack insertItem(@Nullable ResourceHandler<ItemResource> dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;
        try (Transaction tx = Tx.open()) {
            int n = dest.insert(ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return n >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
        }
    }

    /** Jak {@link #insertItem}, ale najpierw dokłada do istniejących stosów. */
    public static ItemStack insertItemStacked(@Nullable ResourceHandler<ItemResource> dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;
        ItemResource res = ItemResource.of(stack);
        try (Transaction tx = Tx.open()) {
            int left = stack.getCount();
            for (int i = 0; i < dest.size() && left > 0; i++) {
                if (res.equals(dest.getResource(i))) left -= dest.insert(i, res, left, tx);
            }
            if (left > 0) left -= dest.insert(res, left, tx);
            if (!simulate) tx.commit();
            return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
        }
    }

    /** Daje przedmiot graczowi; to, co się nie zmieści, upuszcza obok niego. */
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        player.getInventory().placeItemBackInInventory(stack);
    }

    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        return !a.isEmpty() && ItemStack.isSameItemSameComponents(a, b) && a.isStackable();
    }
}
