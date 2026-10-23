package pl.geesio.redcircuit.alloy;

import net.minecraft.item.ItemStack;

import java.util.List;

/** Receptura pieca do stopów: kilka składników z ilościami (w dowolnych slotach siatki) -> wynik. */
public final class AlloyRecipe {
    public final List<AlloyIngredient> inputs;
    private final ItemStack result;
    public final int cookTime;

    public AlloyRecipe(List<AlloyIngredient> inputs, ItemStack result, int cookTime) {
        this.inputs = inputs;
        this.result = result.copy();
        this.cookTime = cookTime;
    }

    public ItemStack result() {
        return result.copy();
    }

    /** Ile sztuk z którego slotu zużyć (null, jeśli składników brakuje). */
    public int[] allocate(ItemStack[] items) {
        int[] avail = new int[items.length];
        int[] take = new int[items.length];
        for (int i = 0; i < items.length; i++) avail[i] = items[i] == null ? 0 : items[i].stackSize;
        for (AlloyIngredient ing : inputs) {
            int need = ing.count;
            for (int i = 0; i < items.length && need > 0; i++) {
                if (avail[i] <= 0 || !ing.matches(items[i])) continue;
                int t = Math.min(need, avail[i]);
                avail[i] -= t;
                take[i] += t;
                need -= t;
            }
            if (need > 0) return null;
        }
        return take;
    }
}
