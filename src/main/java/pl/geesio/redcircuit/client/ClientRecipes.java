package pl.geesio.redcircuit.client;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.List;

/**
 * Receptury moda otrzymane z serwera (od 1.21.2 klient nie zna receptur, dopóki serwer ich nie wyśle -
 * wysyłamy je w {@code OnDatapackSyncEvent}). Używane przez integrację z JEI.
 */
public final class ClientRecipes {
    private static RecipeMap recipes = RecipeMap.EMPTY;

    private ClientRecipes() {}

    public static void onRecipesReceived(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<T> all(RecipeType<T> type) {
        return recipes.byType(type).stream().map(RecipeHolder::value).toList();
    }
}
