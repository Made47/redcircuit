package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;

/** Energia RedCircuit (FE) widziana przez Team Reborn Energy (1 FE = 1 E). */
public final class EnergyExport implements team.reborn.energy.api.EnergyStorage {
    private static final String IN = "in", OUT = "out";
    private final IEnergyStorage handler;
    private final Deferred deferred = new Deferred();

    public EnergyExport(IEnergyStorage handler) {
        this.handler = handler;
    }

    public IEnergyStorage handler() {
        return handler;
    }

    @Override
    public boolean supportsInsertion() {
        return handler.canReceive();
    }

    @Override
    public long insert(long maxAmount, TransactionContext tx) {
        StoragePreconditions.notNegative(maxAmount);
        if (!handler.canReceive() || maxAmount == 0) return 0;
        Deferred.State st = deferred.state(tx);
        if (st.conflicts(IN, true, IN)) return 0;
        long pend = st.pending(IN, true, IN);
        int sim = handler.receiveEnergy((int) Math.min(Integer.MAX_VALUE, pend + maxAmount), true);
        long accepted = sim - pend;
        if (accepted <= 0) return 0;
        final int n = (int) accepted;
        deferred.add(tx, st, IN, true, IN, accepted, () -> handler.receiveEnergy(n, false));
        return accepted;
    }

    @Override
    public boolean supportsExtraction() {
        return handler.canExtract();
    }

    @Override
    public long extract(long maxAmount, TransactionContext tx) {
        StoragePreconditions.notNegative(maxAmount);
        if (!handler.canExtract() || maxAmount == 0) return 0;
        Deferred.State st = deferred.state(tx);
        if (st.conflicts(OUT, false, OUT)) return 0;
        long pend = st.pending(OUT, false, OUT);
        int sim = handler.extractEnergy((int) Math.min(Integer.MAX_VALUE, pend + maxAmount), true);
        long accepted = sim - pend;
        if (accepted <= 0) return 0;
        final int n = (int) accepted;
        deferred.add(tx, st, OUT, false, OUT, accepted, () -> handler.extractEnergy(n, false));
        return accepted;
    }

    @Override
    public long getAmount() {
        return handler.getEnergyStored();
    }

    @Override
    public long getCapacity() {
        return handler.getMaxEnergyStored();
    }
}
