package pl.geesio.redcircuit.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Wspólna obsługa slotów na baterie (i inne przedmioty z energią FE) w urządzeniach. */
public final class BatteryHelper {
    private BatteryHelper() {}

    @Nullable
    public static IEnergyStorage energyOf(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    /** Czy przedmiot można włożyć do slotu baterii. */
    public static boolean isBattery(ItemStack stack) {
        return energyOf(stack) != null;
    }

    /** Bateria -> urządzenie. Zwraca przesłaną ilość FE. */
    public static int discharge(ItemStack stack, ModEnergyStorage target, int maxRate) {
        IEnergyStorage item = energyOf(stack);
        if (item == null || !item.canExtract()) return 0;
        int space = Math.min(maxRate, target.getMaxEnergyStored() - target.getEnergyStored());
        if (space <= 0) return 0;
        int taken = item.extractEnergy(space, false);
        if (taken > 0) target.addInternal(taken);
        return taken;
    }

    /** Urządzenie -> bateria. Zwraca przesłaną ilość FE. */
    public static int charge(ItemStack stack, ModEnergyStorage source, int maxRate) {
        IEnergyStorage item = energyOf(stack);
        if (item == null || !item.canReceive()) return 0;
        int avail = Math.min(maxRate, source.getEnergyStored());
        if (avail <= 0) return 0;
        int given = item.receiveEnergy(avail, false);
        if (given > 0) source.setEnergy(source.getEnergyStored() - given);
        return given;
    }

    /** Czy bateria jest pełna (albo nie przyjmuje energii). */
    public static boolean isFull(ItemStack stack) {
        IEnergyStorage item = energyOf(stack);
        return item == null || !item.canReceive() || item.getEnergyStored() >= item.getMaxEnergyStored();
    }

    /** Czy bateria jest pusta (albo nie oddaje energii). */
    public static boolean isEmpty(ItemStack stack) {
        IEnergyStorage item = energyOf(stack);
        return item == null || !item.canExtract() || item.getEnergyStored() <= 0;
    }

    /**
     * Wczytuje sloty z zapisu, zachowując rozmiar handlera
     * (stare zapisy miały mniej slotów - deserializeNBT zmniejszyłby handler).
     */
    public static void loadItems(ItemStackHandler items, HolderLookup.Provider registries, CompoundTag tag) {
        ItemStackHandler tmp = new ItemStackHandler(items.getSlots());
        tmp.deserializeNBT(registries, tag);
        for (int i = 0; i < items.getSlots(); i++) {
            items.setStackInSlot(i, i < tmp.getSlots() ? tmp.getStackInSlot(i) : ItemStack.EMPTY);
        }
    }
}
