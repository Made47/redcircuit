package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import pl.geesio.redcircuit.platform.fluids.FluidStack;
import pl.geesio.redcircuit.platform.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Zbiornik RedCircuit ({@link IFluidHandler}, mB) widziany przez Fabric Transfer API (droplety). */
public final class FluidHandlerStorage implements Storage<FluidVariant> {
    private static final String TANKS = "tanks";
    private final IFluidHandler handler;
    private final Deferred deferred = new Deferred();

    public FluidHandlerStorage(IFluidHandler handler) {
        this.handler = handler;
    }

    public IFluidHandler handler() {
        return handler;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext tx) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        int mb = (int) Math.min(Integer.MAX_VALUE, maxAmount / FluidStack.DROPLETS_PER_MB);
        if (mb <= 0) return 0;
        Deferred.State st = deferred.state(tx);
        if (st.conflicts(TANKS, true, resource)) return 0;
        long pend = st.pending(TANKS, true, resource);
        int sim = handler.fill(new FluidStack(resource.getFluid(), (int) (pend + mb)), IFluidHandler.FluidAction.SIMULATE);
        long accepted = sim - pend;
        if (accepted <= 0) return 0;
        final int n = (int) accepted;
        deferred.add(tx, st, TANKS, true, resource, accepted,
                () -> handler.fill(new FluidStack(resource.getFluid(), n), IFluidHandler.FluidAction.EXECUTE));
        return accepted * FluidStack.DROPLETS_PER_MB;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext tx) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        int mb = (int) Math.min(Integer.MAX_VALUE, maxAmount / FluidStack.DROPLETS_PER_MB);
        if (mb <= 0) return 0;
        Deferred.State st = deferred.state(tx);
        if (st.conflicts(TANKS, false, resource)) return 0;
        long pend = st.pending(TANKS, false, resource);
        FluidStack sim = handler.drain(new FluidStack(resource.getFluid(), (int) (pend + mb)), IFluidHandler.FluidAction.SIMULATE);
        if (sim.isEmpty() || sim.getFluid() != resource.getFluid()) return 0;
        long accepted = sim.getAmount() - pend;
        if (accepted <= 0) return 0;
        final int n = (int) accepted;
        deferred.add(tx, st, TANKS, false, resource, accepted,
                () -> handler.drain(new FluidStack(resource.getFluid(), n), IFluidHandler.FluidAction.EXECUTE));
        return accepted * FluidStack.DROPLETS_PER_MB;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        List<StorageView<FluidVariant>> views = new ArrayList<>();
        for (int t = 0; t < handler.getTanks(); t++) views.add(new TankView(t));
        return views.iterator();
    }

    private final class TankView implements StorageView<FluidVariant> {
        private final int tank;

        TankView(int tank) {
            this.tank = tank;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext tx) {
            return FluidHandlerStorage.this.extract(resource, maxAmount, tx);
        }

        @Override
        public boolean isResourceBlank() {
            return handler.getFluidInTank(tank).isEmpty();
        }

        @Override
        public FluidVariant getResource() {
            return handler.getFluidInTank(tank).toVariant();
        }

        @Override
        public long getAmount() {
            return handler.getFluidInTank(tank).dropletAmount();
        }

        @Override
        public long getCapacity() {
            return (long) handler.getTankCapacity(tank) * FluidStack.DROPLETS_PER_MB;
        }
    }
}
