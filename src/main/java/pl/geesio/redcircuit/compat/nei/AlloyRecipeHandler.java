package pl.geesio.redcircuit.compat.nei;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.lib.gui.GuiDraw;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import pl.geesio.redcircuit.alloy.AlloyIngredient;
import pl.geesio.redcircuit.alloy.AlloyRecipe;
import pl.geesio.redcircuit.alloy.AlloyRecipes;

import java.util.ArrayList;
import java.util.List;

/** Receptury pieca do stopów (zwykłego i elektrycznego). */
public class AlloyRecipeHandler extends RCRecipeHandler {
    private static final int GRID_X = 20, GRID_Y = 6, OUT_X = 111, OUT_Y = 24;

    public class CachedAlloy extends CachedRecipe {
        final List<PositionedStack> inputs = new ArrayList<>();
        final PositionedStack result;
        final AlloyRecipe recipe;

        CachedAlloy(AlloyRecipe r) {
            recipe = r;
            int i = 0;
            for (AlloyIngredient ing : r.inputs) {
                List<ItemStack> ex = ing.examples();
                if (ex.isEmpty()) continue;
                PositionedStack ps = new PositionedStack(ex, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18);
                inputs.add(ps);
                i++;
            }
            result = new PositionedStack(r.result(), OUT_X, OUT_Y);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, inputs);
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("tile.redcircuit.alloy_furnace.name");
    }

    @Override
    public String getOverlayIdentifier() {
        return NEIRedCircuitConfig.id("alloy");
    }

    @Override
    int arrowX() {
        return 80;
    }

    @Override
    int[][] slots(int recipe) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < 9; i++) out.add(new int[]{GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18});
        out.add(new int[]{OUT_X, OUT_Y});
        return out.toArray(new int[0][]);
    }

    @Override
    public void drawExtras(int recipe) {
        super.drawExtras(recipe);
        if (recipe < arecipes.size() && arecipes.get(recipe) instanceof CachedAlloy c) {
            String t = String.format("%.1f s", c.recipe.cookTime / 20.0);
            GuiDraw.drawString(t, 80, 44, 0x404040, false);
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals(getOverlayIdentifier())) {
            for (AlloyRecipe r : AlloyRecipes.all()) arecipes.add(new CachedAlloy(r));
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (AlloyRecipe r : AlloyRecipes.all()) {
            if (NEIServerUtils.areStacksSameTypeCrafting(r.result(), result)) arecipes.add(new CachedAlloy(r));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (AlloyRecipe r : AlloyRecipes.all()) {
            for (AlloyIngredient ing : r.inputs) {
                if (ing.matches(ingredient)) {
                    arecipes.add(new CachedAlloy(r));
                    break;
                }
            }
        }
    }
}
