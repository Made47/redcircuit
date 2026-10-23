package pl.geesio.redcircuit.alloy;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import pl.geesio.redcircuit.item.ModItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Rejestr receptur pieca do stopów. Inne mody (albo MineTweaker - później) mogą dodawać receptury przez {@link #add}.
 */
public final class AlloyRecipes {
    private AlloyRecipes() {}

    private static final List<AlloyRecipe> RECIPES = new ArrayList<>();

    public static void add(AlloyRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static void add(ItemStack result, int cookTime, AlloyIngredient... inputs) {
        add(new AlloyRecipe(Arrays.asList(inputs), result, cookTime));
    }

    public static List<AlloyRecipe> all() {
        return Collections.unmodifiableList(RECIPES);
    }

    /** Pierwsza receptura, dla której w siatce są wszystkie składniki. */
    public static AlloyRecipe find(ItemStack[] grid) {
        for (AlloyRecipe r : RECIPES) {
            if (r.allocate(grid) != null) return r;
        }
        return null;
    }

    public static void registerDefaults() {
        add(new ItemStack(ModItems.brassIngot, 4), 200,
                AlloyIngredient.ore("ingotCopper", 3), AlloyIngredient.ore("ingotTin", 1));
        add(new ItemStack(ModItems.signalAlloyIngot), 200,
                AlloyIngredient.ore("ingotCopper", 1), AlloyIngredient.ore("dustRedstone", 4));
        add(new ItemStack(ModItems.powerAlloyIngot), 200,
                AlloyIngredient.ore("ingotSilver", 1), AlloyIngredient.ore("dustAzurite", 4));
        add(new ItemStack(ModItems.steelIngot), 400,
                AlloyIngredient.ore("ingotIron", 1), AlloyIngredient.item(new ItemStack(Items.coal, 1, OreDictionary.WILDCARD_VALUE), 2));
    }
}
