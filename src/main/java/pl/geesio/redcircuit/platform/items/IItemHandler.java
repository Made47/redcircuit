package pl.geesio.redcircuit.platform.items;

import net.minecraft.world.item.ItemStack;

/** Odpowiednik IItemHandler z NeoForge (inwentarz ze slotami, operacje z symulacją). */
public interface IItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    default boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
