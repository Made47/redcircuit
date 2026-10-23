package pl.geesio.redcircuit.platform.energy;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/** Energia zapisana w komponencie danych przedmiotu (jak ComponentEnergyStorage z NeoForge). */
public class ComponentEnergyStorage implements IEnergyStorage {
    protected final ItemStack parent;
    protected final DataComponentType<Integer> energyComponent;
    protected final int capacity;
    protected final int maxReceive;
    protected final int maxExtract;

    public ComponentEnergyStorage(ItemStack parent, DataComponentType<Integer> energyComponent,
                                  int capacity, int maxReceive, int maxExtract) {
        this.parent = parent;
        this.energyComponent = energyComponent;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public ComponentEnergyStorage(ItemStack parent, DataComponentType<Integer> energyComponent, int capacity) {
        this(parent, energyComponent, capacity, capacity, capacity);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!canReceive() || toReceive <= 0) return 0;
        int energy = getEnergyStored();
        int received = Math.min(capacity - energy, Math.min(maxReceive, toReceive));
        if (!simulate && received > 0) setEnergy(energy + received);
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (!canExtract() || toExtract <= 0) return 0;
        int energy = getEnergyStored();
        int extracted = Math.min(energy, Math.min(maxExtract, toExtract));
        if (!simulate && extracted > 0) setEnergy(energy - extracted);
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        int e = parent.getOrDefault(energyComponent, 0);
        return Math.max(0, Math.min(capacity, e));
    }

    protected void setEnergy(int energy) {
        parent.set(energyComponent, Math.max(0, Math.min(capacity, energy)));
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
}
