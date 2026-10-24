package pl.geesio.redcircuit.util.transfer;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/** Prosty magazyn energii (transakcyjny). */
public class EnergyStorage extends SimpleEnergyHandler implements IEnergyStorage {
    public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    @Override
    public boolean canReceive() {
        return maxInsert > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }
}
