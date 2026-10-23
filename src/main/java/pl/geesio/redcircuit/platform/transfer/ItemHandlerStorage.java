package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Inwentarz RedCircuit ({@link IItemHandler}) widziany przez Fabric Transfer API. */
public final class ItemHandlerStorage implements Storage<ItemVariant> {
    private final IItemHandler handler;
    private final Deferred deferred = new Deferred();

    public ItemHandlerStorage(IItemHandler handler) {
        this.handler = handler;
    }

    public IItemHandler handler() {
        return handler;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        Deferred.State st = deferred.state(tx);
        long done = 0;
        for (int slot = 0; slot < handler.getSlots() && done < maxAmount; slot++) {
            if (st.conflicts(slot, true, resource)) continue;
            long pend = st.pending(slot, true, resource);
            int want = (int) Math.min(maxAmount - done, resource.getItem().getDefaultMaxStackSize());
            ItemStack probe = resource.toStack((int) (pend + want));
            ItemStack rest = handler.insertItem(slot, probe, true);
            long accepted = probe.getCount() - rest.getCount() - pend;
            if (accepted <= 0) continue;
            final int s = slot;
            final int n = (int) accepted;
            deferred.add(tx, st, slot, true, resource, accepted, () -> {
                ItemStack left = handler.insertItem(s, resource.toStack(n), false);
                if (!left.isEmpty()) left = ItemHandlerHelper.insertItem(handler, left, false);
                if (!left.isEmpty()) RedCircuit.LOGGER.warn("RedCircuit: {} could not be inserted after commit", left);
            });
            done += accepted;
        }
        return done;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long done = 0;
        for (int slot = 0; slot < handler.getSlots() && done < maxAmount; slot++) {
            done += extractFrom(slot, resource, maxAmount - done, tx);
        }
        return done;
    }

    long extractFrom(int slot, ItemVariant resource, long maxAmount, TransactionContext tx) {
        Deferred.State st = deferred.state(tx);
        ItemStack in = handler.getStackInSlot(slot);
        if (in.isEmpty() || !resource.matches(in) || st.conflicts(slot, false, resource)) return 0;
        long pend = st.pending(slot, false, resource);
        int want = (int) Math.min(maxAmount, in.getCount() - pend);
        if (want <= 0) return 0;
        ItemStack sim = handler.extractItem(slot, (int) (pend + want), true);
        if (!resource.matches(sim)) return 0;
        long accepted = sim.getCount() - pend;
        if (accepted <= 0) return 0;
        final int n = (int) accepted;
        deferred.add(tx, st, slot, false, resource, accepted, () -> handler.extractItem(slot, n, false));
        return accepted;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) views.add(new SlotView(slot));
        }
        return views.iterator();
    }

    private final class SlotView implements StorageView<ItemVariant> {
        private final int slot;

        SlotView(int slot) {
            this.slot = slot;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            return extractFrom(slot, resource, maxAmount, tx);
        }

        @Override
        public boolean isResourceBlank() {
            return handler.getStackInSlot(slot).isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return ItemVariant.of(handler.getStackInSlot(slot));
        }

        @Override
        public long getAmount() {
            return handler.getStackInSlot(slot).getCount();
        }

        @Override
        public long getCapacity() {
            return handler.getSlotLimit(slot);
        }
    }
}
