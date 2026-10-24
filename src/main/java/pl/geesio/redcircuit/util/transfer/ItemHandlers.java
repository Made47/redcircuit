package pl.geesio.redcircuit.util.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/** Konwersje między {@link ResourceHandler} (zdolności NeoForge) a slotowym {@link IItemHandler}. */
public final class ItemHandlers {
    private ItemHandlers() {}

    /** Widok slotowy na dowolny handler przedmiotów (np. z {@code level.getCapability}). */
    @Nullable
    public static IItemHandler wrap(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler == null) return null;
        if (handler instanceof IItemHandler h) return h;
        return new Wrapper(handler);
    }

    /** Widok slotowy na zwykły kontener (skrzynia, inwentarz bloku). */
    public static IItemHandler of(Container container) {
        return new Wrapper(VanillaContainerWrapper.of(container));
    }

    private record Wrapper(ResourceHandler<ItemResource> h) implements IItemHandler {
        @Override
        public int getSlots() {
            return h.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemResource r = h.getResource(slot);
            return r.isEmpty() ? ItemStack.EMPTY : r.toStack(h.getAmountAsInt(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            try (Transaction tx = Tx.open()) {
                int n = h.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
                return n >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - n);
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemResource r = h.getResource(slot);
            if (r.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            try (Transaction tx = Tx.open()) {
                int n = h.extract(slot, r, amount, tx);
                if (!simulate) tx.commit();
                return r.toStack(n);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return h.getCapacityAsInt(slot, ItemResource.EMPTY);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return h.isValid(slot, ItemResource.of(stack));
        }

        // bezpośrednie przekazanie - zachowuje pełną transakcyjność oryginału
        @Override
        public int size() {
            return h.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return h.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return h.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return h.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return h.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return h.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return h.extract(index, resource, amount, transaction);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return h.insert(resource, amount, transaction);
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return h.extract(resource, amount, transaction);
        }
    }
}
