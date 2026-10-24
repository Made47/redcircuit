package pl.geesio.redcircuit.util.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Widok slotowy, którego operacje nie dają się cofnąć (np. przedmiot wpada do rury).
 * W transakcji tylko symuluje, a prawdziwą operację wykonuje dopiero po zatwierdzeniu transakcji głównej.
 */
public abstract class DeferredItemHandler implements IItemHandler {
    private final List<Runnable> pending = new ArrayList<>();
    private final SnapshotJournal<Integer> journal = new SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return pending.size();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            while (pending.size() > snapshot) pending.removeLast();
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            List<Runnable> run = new ArrayList<>(pending);
            pending.clear();
            run.forEach(Runnable::run);
        }
    };

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;
        ItemStack stack = resource.toStack(amount);
        int accepted = amount - insertItem(index, stack, true).getCount();
        if (accepted <= 0) return 0;
        journal.updateSnapshots(transaction);
        pending.add(() -> insertItem(index, resource.toStack(accepted), false));
        return accepted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0 || !resource.matches(getStackInSlot(index))) return 0;
        int got = extractItem(index, amount, true).getCount();
        if (got <= 0) return 0;
        journal.updateSnapshots(transaction);
        pending.add(() -> extractItem(index, got, false));
        return got;
    }
}
