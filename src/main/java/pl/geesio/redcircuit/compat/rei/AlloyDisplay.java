package pl.geesio.redcircuit.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import pl.geesio.redcircuit.alloy.AlloyRecipe;
import pl.geesio.redcircuit.platform.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

/** Receptura pieca do stopów w REI. */
public class AlloyDisplay extends BasicDisplay {
    /** Zapis/odczyt wyświetlenia (np. historia REI) przez kodek samej receptury. */
    public static final DisplaySerializer<AlloyDisplay> SERIALIZER = DisplaySerializer.of(
            AlloyRecipe.Serializer.INSTANCE.codec().xmap(AlloyDisplay::new, AlloyDisplay::recipe),
            AlloyRecipe.Serializer.INSTANCE.streamCodec().map(AlloyDisplay::new, AlloyDisplay::recipe));

    private final AlloyRecipe recipe;
    private final int cookTime;

    public AlloyDisplay(AlloyRecipe recipe) {
        super(inputs(recipe), List.of(EntryIngredients.of(recipe.result())));
        this.recipe = recipe;
        this.cookTime = recipe.cookTime();
    }

    public AlloyRecipe recipe() {
        return recipe;
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }

    private static List<EntryIngredient> inputs(AlloyRecipe recipe) {
        List<EntryIngredient> list = new ArrayList<>();
        for (SizedIngredient s : recipe.inputs()) list.add(EntryIngredients.ofItemStacks(List.of(s.getItems())));
        return list;
    }

    public int cookTime() {
        return cookTime;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return ReiIds.ALLOYING;
    }
}
