package pl.geesio.redcircuit.energy;

import net.minecraft.nbt.NBTTagCompound;

/** Magazyn energii (RF) z limitami przyjmowania i oddawania - odpowiednik ModEnergyStorage z 1.21. */
public class EnergyBuffer {
    private int energy;
    public final int capacity, maxReceive, maxExtract;
    private final Runnable onChange;

    public EnergyBuffer(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.onChange = onChange;
    }

    public int get() {
        return energy;
    }

    public int space() {
        return capacity - energy;
    }

    public int receive(int amount, boolean simulate) {
        int r = Math.max(0, Math.min(Math.min(amount, maxReceive), capacity - energy));
        if (r > 0 && !simulate) {
            energy += r;
            onChange.run();
        }
        return r;
    }

    public int extract(int amount, boolean simulate) {
        int r = Math.max(0, Math.min(Math.min(amount, maxExtract), energy));
        if (r > 0 && !simulate) {
            energy -= r;
            onChange.run();
        }
        return r;
    }

    public void set(int value) {
        int v = Math.max(0, Math.min(capacity, value));
        if (v != energy) {
            energy = v;
            onChange.run();
        }
    }

    /** Dodaje energię z pominięciem limitu przyjmowania; zwraca ile faktycznie dodano. */
    public int addInternal(int amount) {
        int add = Math.max(0, Math.min(amount, capacity - energy));
        if (add > 0) {
            energy += add;
            onChange.run();
        }
        return add;
    }

    /** Zużywa {@code amount}, jeśli jest tyle energii. */
    public boolean use(int amount) {
        if (energy < amount) return false;
        energy -= amount;
        if (amount > 0) onChange.run();
        return true;
    }

    public void save(NBTTagCompound tag) {
        tag.setInteger("Energy", energy);
    }

    public void load(NBTTagCompound tag) {
        energy = Math.max(0, Math.min(capacity, tag.getInteger("Energy")));
    }
}
