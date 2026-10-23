package pl.geesio.redcircuit.platform.items;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot menu oparty na {@link IItemHandler} (jak SlotItemHandler z NeoForge). */
public class SlotItemHandler extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final IItemHandler itemHandler;
    private final int index;

    public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(EMPTY, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return itemHandler.isItemValid(index, stack);
    }

    @Override
    public ItemStack getItem() {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack) {
        if (itemHandler instanceof IItemHandlerModifiable m) m.setStackInSlot(index, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public int getMaxStackSize() {
        return itemHandler.getSlotLimit(index);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        ItemStack maxAdd = stack.copyWithCount(stack.getMaxStackSize());
        IItemHandler handler = itemHandler;
        ItemStack currentStack = handler.getStackInSlot(index);
        if (handler instanceof IItemHandlerModifiable m) {
            m.setStackInSlot(index, ItemStack.EMPTY);
            ItemStack remainder = m.insertItem(index, maxAdd, true);
            m.setStackInSlot(index, currentStack);
            return maxAdd.getCount() - remainder.getCount();
        }
        ItemStack remainder = handler.insertItem(index, maxAdd, true);
        int current = currentStack.getCount();
        int added = maxAdd.getCount() - remainder.getCount();
        return current + added;
    }

    @Override
    public boolean mayPickup(Player player) {
        return !itemHandler.extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return itemHandler.extractItem(index, amount, false);
    }
}
