package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import pl.geesio.redcircuit.platform.fluids.FluidStack;
import pl.geesio.redcircuit.platform.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/** Zbiornik z Fabric Transfer API (inne mody, kocioł) widziany jako {@link IFluidHandler} w mB. */
public final class StorageFluidHandler implements IFluidHandler {
    private final Storage<FluidVariant> storage;
    private final List<StorageView<FluidVariant>> views = new ArrayList<>();

    public StorageFluidHandler(Storage<FluidVariant> storage) {
        this.storage = storage;
        for (StorageView<FluidVariant> v : storage) views.add(v);
    }

    @Override
    public int getTanks() {
        return Math.max(1, views.size());
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank >= views.size()) return FluidStack.EMPTY;
        StorageView<FluidVariant> v = views.get(tank);
        return v.isResourceBlank() ? FluidStack.EMPTY : FluidStack.of(v.getResource(), v.getAmount());
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank >= views.size()) return 0;
        return (int) Math.min(Integer.MAX_VALUE, views.get(tank).getCapacity() / FluidStack.DROPLETS_PER_MB);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return fill(stack.copyWithAmount(1), FluidAction.SIMULATE) > 0;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !storage.supportsInsertion()) return 0;
        try (Transaction tx = Deferred.open()) {
            long n = storage.insert(resource.toVariant(), resource.dropletAmount(), tx);
            if (action.execute()) tx.commit();
            return (int) (n / FluidStack.DROPLETS_PER_MB);
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !storage.supportsExtraction()) return FluidStack.EMPTY;
        try (Transaction tx = Deferred.open()) {
            long n = storage.extract(resource.toVariant(), resource.dropletAmount(), tx);
            int mb = (int) (n / FluidStack.DROPLETS_PER_MB);
            if (mb <= 0) return FluidStack.EMPTY;
            if (action.execute()) tx.commit();
            return new FluidStack(resource.getFluid(), mb);
        }
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        for (StorageView<FluidVariant> v : views) {
            if (v.isResourceBlank() || v.getAmount() <= 0) continue;
            return drain(new FluidStack(v.getResource().getFluid(), maxDrain), action);
        }
        return FluidStack.EMPTY;
    }
}
