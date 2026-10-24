package pl.geesio.redcircuit.util.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Inwentarz w stylu slotów (getStackInSlot / insertItem / extractItem) widziany jednocześnie
 * jako {@link ResourceHandler} nowego API transferu NeoForge.
 *
 * <p>Domyślna implementacja metod transakcyjnych wywołuje metody slotowe w transakcji zagnieżdżonej -
 * jest poprawna, gdy metody slotowe ostatecznie operują na {@link ItemStackHandler} (który sam jest transakcyjny).
 * Widoki, które robią coś nieodwracalnego, powinny dziedziczyć z {@link DeferredItemHandler}.
 */
public interface IItemHandler extends ResourceHandler<ItemResource> {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);

    // ---------- ResourceHandler ----------

    @Override
    default int size() {
        return getSlots();
    }

    @Override
    default ItemResource getResource(int index) {
        return ItemResource.of(getStackInSlot(index));
    }

    @Override
    default long getAmountAsLong(int index) {
        return getStackInSlot(index).getCount();
    }

    @Override
    default long getCapacityAsLong(int index, ItemResource resource) {
        if (resource.isEmpty()) return getSlotLimit(index);
        if (!isValid(index, resource)) return 0;
        return Math.min(getSlotLimit(index), resource.getMaxStackSize());
    }

    @Override
    default boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || isItemValid(index, resource.toStack());
    }

    @Override
    default int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;
        try (Transaction tx = Transaction.open(transaction)) {
            ItemStack rest = insertItem(index, resource.toStack(amount), false);
            tx.commit();
            return amount - rest.getCount();
        }
    }

    @Override
    default int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0 || !resource.matches(getStackInSlot(index))) return 0;
        try (Transaction tx = Transaction.open(transaction)) {
            ItemStack out = extractItem(index, amount, false);
            tx.commit();
            return out.getCount();
        }
    }
}
