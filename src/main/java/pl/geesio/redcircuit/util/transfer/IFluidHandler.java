package pl.geesio.redcircuit.util.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/** Zbiornik płynów z metodami fill/drain, widoczny też jako {@link ResourceHandler} płynów. */
public interface IFluidHandler extends ResourceHandler<FluidResource> {
    enum FluidAction {
        EXECUTE, SIMULATE;

        public boolean execute() {
            return this == EXECUTE;
        }

        public boolean simulate() {
            return this == SIMULATE;
        }
    }

    default int getTanks() {
        return size();
    }

    default FluidStack getFluidInTank(int tank) {
        FluidResource r = getResource(tank);
        return r.isEmpty() ? FluidStack.EMPTY : r.toStack(getAmountAsInt(tank));
    }

    default int getTankCapacity(int tank) {
        return getCapacityAsInt(tank, FluidResource.EMPTY);
    }

    default int fill(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) return 0;
        try (Transaction tx = Tx.open()) {
            int n = insert(FluidResource.of(stack), stack.getAmount(), tx);
            if (action.execute()) tx.commit();
            return n;
        }
    }

    default FluidStack drain(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        FluidResource r = FluidResource.of(stack);
        try (Transaction tx = Tx.open()) {
            int n = extract(r, stack.getAmount(), tx);
            if (action.execute()) tx.commit();
            return n <= 0 ? FluidStack.EMPTY : r.toStack(n);
        }
    }

    default FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        for (int i = 0; i < size(); i++) {
            FluidResource r = getResource(i);
            if (r.isEmpty() || getAmountAsLong(i) <= 0) continue;
            try (Transaction tx = Tx.open()) {
                int n = extract(r, maxDrain, tx);
                if (n > 0) {
                    if (action.execute()) tx.commit();
                    return r.toStack(n);
                }
            }
        }
        return FluidStack.EMPTY;
    }

    @Nullable
    static IFluidHandler wrap(@Nullable ResourceHandler<FluidResource> handler) {
        if (handler == null) return null;
        if (handler instanceof IFluidHandler f) return f;
        return new Wrapper(handler);
    }

    record Wrapper(ResourceHandler<FluidResource> h) implements IFluidHandler {
        @Override
        public int size() {
            return h.size();
        }

        @Override
        public FluidResource getResource(int index) {
            return h.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return h.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return h.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return h.isValid(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return h.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return h.extract(index, resource, amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            return h.insert(resource, amount, transaction);
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            return h.extract(resource, amount, transaction);
        }
    }
}
