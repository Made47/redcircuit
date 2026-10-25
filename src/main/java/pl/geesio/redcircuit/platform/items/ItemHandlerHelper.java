package pl.geesio.redcircuit.platform.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemHandlerHelper {
    private ItemHandlerHelper() {}

    /** Wkłada stos do kolejnych slotów; zwraca resztę. */
    public static ItemStack insertItem(@Nullable IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;
        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }
        return stack;
    }

    /** Najpierw dopełnia sloty z tym samym przedmiotem, potem puste. */
    public static ItemStack insertItemStacked(@Nullable IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (inventory == null || stack.isEmpty()) return stack;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                stack = inventory.insertItem(i, stack, simulate);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                stack = inventory.insertItem(i, stack, simulate);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        return !a.isEmpty() && ItemStack.isSameItemSameComponents(a, b) && a.isStackable();
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        return size == 0 ? ItemStack.EMPTY : stack.copyWithCount(size);
    }

    /** Daje przedmiot graczowi; gdy ekwipunek jest pełny, upuszcza go obok gracza. */
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
}
