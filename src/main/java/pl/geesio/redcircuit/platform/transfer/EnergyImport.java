package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;

/** Magazyn energii innego moda (Team Reborn Energy) widziany jako FE. */
public final class EnergyImport implements IEnergyStorage {
    private final team.reborn.energy.api.EnergyStorage storage;

    public EnergyImport(team.reborn.energy.api.EnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0 || !storage.supportsInsertion()) return 0;
        try (Transaction tx = Deferred.open()) {
            long n = storage.insert(toReceive, tx);
            if (!simulate) tx.commit();
            return (int) n;
        }
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (toExtract <= 0 || !storage.supportsExtraction()) return 0;
        try (Transaction tx = Deferred.open()) {
            long n = storage.extract(toExtract, tx);
            if (!simulate) tx.commit();
            return (int) n;
        }
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, storage.getAmount());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, storage.getCapacity());
    }

    @Override
    public boolean canExtract() {
        return storage.supportsExtraction();
    }

    @Override
    public boolean canReceive() {
        return storage.supportsInsertion();
    }
}
