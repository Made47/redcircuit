package pl.geesio.redcircuit.platform;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.ItemStack;

/** Czas palenia paliwa w tickach (zamiast ItemStack#getBurnTime z NeoForge). */
public final class Fuel {
    private Fuel() {}

    public static int burnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Integer t = FuelRegistry.INSTANCE.get(stack.getItem());
        return t == null ? 0 : t;
    }
}
