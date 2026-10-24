package pl.geesio.redcircuit.util.transfer;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Inwentarz urządzenia: transakcyjny {@link ItemStacksResourceHandler} z wygodnymi metodami slotowymi.
 * Nadpisuj {@link #onContentsChanged(int)}, {@link #isItemValid} i {@link #getSlotLimit}.
 */
public class ItemStackHandler extends ItemStacksResourceHandler implements IItemHandlerModifiable {
    public ItemStackHandler(int size) {
        super(size);
    }

    public ItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    /** Wywoływane po każdej zmianie zawartości slotu. */
    protected void onContentsChanged(int slot) {}

    /** Wywoływane po wczytaniu z zapisu. */
    protected void onLoad() {}

    @Override
    protected final void onContentsChanged(int index, ItemStack previousContents) {
        onContentsChanged(index);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Item.ABSOLUTE_MAX_STACK_SIZE;
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    // ---------- most do ItemStacksResourceHandler ----------

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || isItemValid(index, resource.toStack());
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        if (resource.isEmpty()) return getSlotLimit(index);
        return Math.min(getSlotLimit(index), resource.getMaxStackSize());
    }


    // ---------- metody slotowe ----------

    @Override
    public int getSlots() {
        return size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        ItemStack old = stacks.set(slot, stack);
        onContentsChanged(slot, old);
    }

    public void setSize(int size) {
        setStacks(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Tx.open()) {
            int inserted = insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack current = stacks.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemResource res = ItemResource.of(current);
        try (Transaction tx = Tx.open()) {
            int got = extract(slot, res, amount, tx);
            if (!simulate) tx.commit();
            return res.toStack(got);
        }
    }

    // ---------- zapis ----------

    public void save(ValueOutput output) {
        serialize(output);
    }

    public void load(ValueInput input) {
        int size = size();
        if (input.list(VALUE_IO_KEY, ItemStack.OPTIONAL_CODEC).isEmpty()) {
            loadLegacy(input, size);
            onLoad();
            return;
        }
        deserialize(input);
        if (size() != size) {
            // starsze zapisy mogły mieć inną liczbę slotów - zachowaj rozmiar inwentarza
            NonNullList<ItemStack> fixed = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(size, size()); i++) fixed.set(i, stacks.get(i));
            setStacks(fixed);
        }
        onLoad();
    }

    /** Zapis z wersji 1.21.1 (ItemStackHandler NeoForge): {Items: [{Slot, id, count, ...}], Size}. */
    private void loadLegacy(ValueInput input, int size) {
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        for (ValueInput child : input.childrenListOrEmpty("Items")) {
            int slot = child.getIntOr("Slot", -1);
            if (slot < 0 || slot >= size) continue;
            child.read(ItemStack.MAP_CODEC).ifPresent(stack -> list.set(slot, stack));
        }
        setStacks(list);
    }

}
