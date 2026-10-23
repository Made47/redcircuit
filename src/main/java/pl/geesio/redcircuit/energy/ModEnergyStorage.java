package pl.geesio.redcircuit.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/** EnergyStorage z powiadamianiem o zmianach i bezpośrednim ustawianiem wartości. */
public class ModEnergyStorage extends EnergyStorage {
    private final Runnable onChange;

    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int r = super.receiveEnergy(toReceive, simulate);
        if (r > 0 && !simulate) onChange.run();
        return r;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int r = super.extractEnergy(toExtract, simulate);
        if (r > 0 && !simulate) onChange.run();
        return r;
    }

    public void setEnergy(int value) {
        int v = Math.max(0, Math.min(capacity, value));
        if (v != energy) {
            energy = v;
            onChange.run();
        }
    }

    /** Dodaje energię z pominięciem limitu maxReceive. Zwraca ile faktycznie dodano. */
    public int addInternal(int amount) {
        int add = Math.max(0, Math.min(amount, capacity - energy));
        if (add > 0) {
            energy += add;
            onChange.run();
        }
        return add;
    }
}
