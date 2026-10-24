package pl.geesio.redcircuit.energy;

import pl.geesio.redcircuit.util.transfer.EnergyStorage;

/** Magazyn energii z powiadamianiem o zmianach i bezpośrednim ustawianiem wartości. */
public class ModEnergyStorage extends EnergyStorage {
    private final Runnable onChange;

    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChange.run();
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
