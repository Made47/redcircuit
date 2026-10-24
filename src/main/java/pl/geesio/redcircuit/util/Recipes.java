package pl.geesio.redcircuit.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Wyszukiwanie receptur (od 1.21.2 pełny menedżer receptur jest tylko po stronie serwera). */
public final class Recipes {
    private Recipes() {}

    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> find(Level level, RecipeType<T> type, I input) {
        if (level instanceof ServerLevel server) return server.recipeAccess().getRecipeFor(type, input, level);
        return Optional.empty();
    }

    /** Pozostałość po zużyciu przedmiotu w recepturze (np. puste wiadro). */
    public static ItemStack remainder(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStackTemplate t = stack.getItem().getCraftingRemainder(stack);
        return t == null ? ItemStack.EMPTY : t.create();
    }
}
