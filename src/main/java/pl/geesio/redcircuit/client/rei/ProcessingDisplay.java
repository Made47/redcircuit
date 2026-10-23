package pl.geesio.redcircuit.client.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/** Macerator / tartak w REI: składnik -> wynik (+ dodatek z szansą). */
public class ProcessingDisplay extends BasicDisplay {
    private final CategoryIdentifier<ProcessingDisplay> category;
    private final float bonusChance;
    private final int time;
    private final boolean hasBonus;

    public ProcessingDisplay(CategoryIdentifier<ProcessingDisplay> category, Ingredient ingredient, ItemStack result,
                             ItemStack bonus, float bonusChance, int time) {
        super(List.of(EntryIngredients.ofIngredient(ingredient)), outputs(result, bonus));
        this.category = category;
        this.bonusChance = bonusChance;
        this.time = time;
        this.hasBonus = !bonus.isEmpty();
    }

    private static List<EntryIngredient> outputs(ItemStack result, ItemStack bonus) {
        List<EntryIngredient> list = new ArrayList<>();
        list.add(EntryIngredients.of(result));
        if (!bonus.isEmpty()) list.add(EntryIngredients.of(bonus));
        return list;
    }

    public float bonusChance() {
        return bonusChance;
    }

    public int time() {
        return time;
    }

    public boolean hasBonus() {
        return hasBonus;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }
}
