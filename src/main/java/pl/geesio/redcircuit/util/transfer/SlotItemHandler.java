package pl.geesio.redcircuit.util.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot menu oparty o {@link IItemHandler}. */
public class SlotItemHandler extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final IItemHandler handler;
    private final int index;

    public SlotItemHandler(IItemHandler handler, int index, int x, int y) {
        super(EMPTY, index, x, y);
        this.handler = handler;
        this.index = index;
    }

    public IItemHandler getItemHandler() {
        return handler;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && handler.isItemValid(index, stack);
    }

    @Override
    public ItemStack getItem() {
        return handler.getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable m) m.setStackInSlot(index, stack);
        setChanged();
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {}

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(index);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(handler.getSlotLimit(index), stack.getMaxStackSize());
    }

    @Override
    public boolean mayPickup(Player player) {
        return !handler.extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack current = getItem();
        if (current.isEmpty()) return ItemStack.EMPTY;
        if (handler instanceof IItemHandlerModifiable m) {
            ItemStack out = current.split(Math.min(amount, current.getCount()));
            m.setStackInSlot(index, current.isEmpty() ? ItemStack.EMPTY : current);
            return out;
        }
        return handler.extractItem(index, amount, false);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof SlotItemHandler s && s.handler == handler;
    }
}
