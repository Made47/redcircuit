package pl.geesio.redcircuit.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Czas palenia paliwa w tickach (wartości paliw są od 1.21.2 przechowywane w świecie). */
public final class Fuel {
    private Fuel() {}

    public static int burnTime(ItemStack stack, @Nullable Level level) {
        if (stack.isEmpty() || level == null) return 0;
        return level.fuelValues().burnDuration(stack);
    }
}
