package pl.geesio.redcircuit.platform.items.wrapper;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import pl.geesio.redcircuit.platform.items.IItemHandlerModifiable;

/** IItemHandler nad zwykłym {@link Container} (jak InvWrapper z NeoForge). */
public class InvWrapper implements IItemHandlerModifiable {
    private final Container inv;

    public InvWrapper(Container inv) {
        this.inv = inv;
    }

    public Container getInv() {
        return inv;
    }

    @Override
    public int getSlots() {
        return inv.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack inSlot = inv.getItem(slot);
        if (!inSlot.isEmpty()) {
            if (inSlot.getCount() >= Math.min(inSlot.getMaxStackSize(), getSlotLimit(slot))) return stack;
            if (!ItemStack.isSameItemSameComponents(stack, inSlot)) return stack;
            if (!inv.canPlaceItem(slot, stack)) return stack;
            int m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - inSlot.getCount();
            if (stack.getCount() <= m) {
                if (!simulate) {
                    ItemStack copy = stack.copy();
                    copy.grow(inSlot.getCount());
                    inv.setItem(slot, copy);
                    inv.setChanged();
                }
                return ItemStack.EMPTY;
            }
            stack = stack.copy();
            if (!simulate) {
                ItemStack copy = stack.split(m);
                copy.grow(inSlot.getCount());
                inv.setItem(slot, copy);
                inv.setChanged();
            } else {
                stack.shrink(m);
            }
            return stack;
        }
        if (!inv.canPlaceItem(slot, stack)) return stack;
        int m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        if (m < stack.getCount()) {
            stack = stack.copy();
            if (!simulate) {
                inv.setItem(slot, stack.split(m));
                inv.setChanged();
            } else {
                stack.shrink(m);
            }
            return stack;
        }
        if (!simulate) {
            inv.setItem(slot, stack);
            inv.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        ItemStack inSlot = inv.getItem(slot);
        if (inSlot.isEmpty()) return ItemStack.EMPTY;
        if (simulate) {
            return inSlot.getCount() < amount ? inSlot.copy() : inSlot.copyWithCount(amount);
        }
        int m = Math.min(inSlot.getCount(), amount);
        ItemStack decr = inv.removeItem(slot, m);
        inv.setChanged();
        return decr;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        inv.setItem(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inv.canPlaceItem(slot, stack);
    }
}
