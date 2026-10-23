package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyContainerItem;
import net.minecraft.item.ItemStack;

/** Obsługa slotów na baterie (i inne przedmioty z energią RF) w urządzeniach. */
public final class BatteryHelper {
    private BatteryHelper() {}

    public static IEnergyContainerItem energyOf(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IEnergyContainerItem e ? e : null;
    }

    public static boolean isBattery(ItemStack stack) {
        return energyOf(stack) != null;
    }

    /** Bateria -> urządzenie. Zwraca przesłaną ilość RF. */
    public static int discharge(ItemStack stack, EnergyBuffer target, int maxRate) {
        IEnergyContainerItem item = energyOf(stack);
        if (item == null) return 0;
        int space = Math.min(maxRate, target.space());
        if (space <= 0) return 0;
        int taken = item.extractEnergy(stack, space, false);
        if (taken > 0) target.addInternal(taken);
        return taken;
    }

    /** Urządzenie -> bateria. Zwraca przesłaną ilość RF. */
    public static int charge(ItemStack stack, EnergyBuffer source, int maxRate) {
        IEnergyContainerItem item = energyOf(stack);
        if (item == null) return 0;
        int avail = Math.min(maxRate, source.get());
        if (avail <= 0) return 0;
        int given = item.receiveEnergy(stack, avail, false);
        if (given > 0) source.set(source.get() - given);
        return given;
    }

    public static boolean isFull(ItemStack stack) {
        IEnergyContainerItem item = energyOf(stack);
        return item == null || item.getEnergyStored(stack) >= item.getMaxEnergyStored(stack);
    }

    public static boolean isEmpty(ItemStack stack) {
        IEnergyContainerItem item = energyOf(stack);
        return item == null || item.getEnergyStored(stack) <= 0;
    }
}
