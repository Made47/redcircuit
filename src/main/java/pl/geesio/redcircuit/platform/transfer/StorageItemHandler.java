package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import pl.geesio.redcircuit.platform.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Inwentarz z Fabric Transfer API (skrzynie, inne mody) widziany jako {@link IItemHandler}.
 * Każda operacja to osobna transakcja: zatwierdzana przy wykonaniu, wycofywana przy symulacji.
 */
public final class StorageItemHandler implements IItemHandler {
    private final Storage<ItemVariant> storage;
    private final SlottedStorage<ItemVariant> slotted;
    private final List<StorageView<ItemVariant>> views;

    @SuppressWarnings("unchecked")
    public StorageItemHandler(Storage<ItemVariant> storage) {
        this.storage = storage;
        this.slotted = storage instanceof SlottedStorage<?> s ? (SlottedStorage<ItemVariant>) s : null;
        this.views = new ArrayList<>();
        if (slotted == null) {
            for (StorageView<ItemVariant> v : storage) views.add(v);
        }
    }

    private StorageView<ItemVariant> view(int slot) {
        if (slotted != null) return slotted.getSlot(slot);
        return slot < views.size() ? views.get(slot) : null;
    }

    @Override
    public int getSlots() {
        return slotted != null ? slotted.getSlotCount() : views.size() + 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        StorageView<ItemVariant> v = view(slot);
        if (v == null || v.isResourceBlank() || v.getAmount() <= 0) return ItemStack.EMPTY;
        return v.getResource().toStack((int) Math.min(Integer.MAX_VALUE, v.getAmount()));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !storage.supportsInsertion()) return stack;
        ItemVariant variant = ItemVariant.of(stack);
        try (Transaction tx = Deferred.open()) {
            long inserted = slotted != null ? slotted.getSlot(slot).insert(variant, stack.getCount(), tx)
                    : storage.insert(variant, stack.getCount(), tx);
            if (!simulate) tx.commit();
            if (inserted <= 0) return stack;
            return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) inserted);
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        StorageView<ItemVariant> v = view(slot);
        if (amount <= 0 || v == null || v.isResourceBlank()) return ItemStack.EMPTY;
        ItemVariant variant = v.getResource();
        try (Transaction tx = Deferred.open()) {
            long taken = v.extract(variant, amount, tx);
            if (!simulate) tx.commit();
            return taken <= 0 ? ItemStack.EMPTY : variant.toStack((int) taken);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        StorageView<ItemVariant> v = view(slot);
        if (v == null) return 64;
        return (int) Math.min(Integer.MAX_VALUE, v.getCapacity());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        try (Transaction tx = Deferred.open()) {
            ItemVariant variant = ItemVariant.of(stack);
            long n = slotted != null ? slotted.getSlot(slot).insert(variant, 1, tx) : storage.insert(variant, 1, tx);
            return n > 0;
        }
    }
}
