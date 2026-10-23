package pl.geesio.redcircuit.alloy;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/** Składnik receptury pieca do stopów: nazwa ze słownika rud (OreDictionary) albo konkretny przedmiot + ilość. */
public final class AlloyIngredient {
    private final String ore;
    private final ItemStack stack;
    public final int count;

    private AlloyIngredient(String ore, ItemStack stack, int count) {
        this.ore = ore;
        this.stack = stack;
        this.count = count;
    }

    public static AlloyIngredient ore(String name, int count) {
        return new AlloyIngredient(name, null, count);
    }

    /** Przedmiot; metadane {@link OreDictionary#WILDCARD_VALUE} = dowolne. */
    public static AlloyIngredient item(ItemStack stack, int count) {
        return new AlloyIngredient(null, stack.copy(), count);
    }

    public boolean matches(ItemStack s) {
        if (s == null) return false;
        if (ore != null) {
            for (ItemStack o : OreDictionary.getOres(ore)) {
                if (OreDictionary.itemMatches(o, s, false)) return true;
            }
            return false;
        }
        return OreDictionary.itemMatches(stack, s, false);
    }

    /** Przedmioty pasujące do składnika (np. do podglądu w NEI). */
    public List<ItemStack> examples() {
        List<ItemStack> out = new ArrayList<>();
        if (ore != null) {
            for (ItemStack o : OreDictionary.getOres(ore)) {
                ItemStack c = o.copy();
                c.stackSize = count;
                out.add(c);
            }
        } else {
            ItemStack c = stack.copy();
            c.stackSize = count;
            out.add(c);
        }
        return out;
    }
}
