package pl.geesio.redcircuit.compat.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import pl.geesio.redcircuit.energy.ProcessingRecipes;

import java.util.List;

/** Receptury rozdrabniacza i tartaku (wynik + dodatkowy produkt z szansą). */
public abstract class ProcessingRecipeHandler extends RCRecipeHandler {
    private static final int IN_X = 51, IN_Y = 24, OUT_X = 107, OUT_Y = 24, BONUS_X = 131;

    abstract List<ProcessingRecipes.Recipe> recipes();

    abstract String blockKey();

    abstract String name();

    public class CachedProcessing extends CachedRecipe {
        final ProcessingRecipes.Recipe recipe;
        final PositionedStack input, result, bonus;

        CachedProcessing(ProcessingRecipes.Recipe r) {
            recipe = r;
            input = new PositionedStack(r.inputs(), IN_X, IN_Y);
            result = new PositionedStack(r.result, OUT_X, OUT_Y);
            bonus = r.bonus == null ? null : new PositionedStack(r.bonus, BONUS_X, OUT_Y);
        }

        @Override
        public PositionedStack getIngredient() {
            input.setPermutationToRender((cycleticks / 20) % input.items.length);
            return input;
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }

        @Override
        public PositionedStack getOtherStack() {
            return bonus;
        }
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal(blockKey());
    }

    @Override
    public String getOverlayIdentifier() {
        return NEIRedCircuitConfig.id(name());
    }

    @Override
    int[][] slots(int recipe) {
        boolean bonus = recipe < arecipes.size() && arecipes.get(recipe) instanceof CachedProcessing c && c.bonus != null;
        return bonus ? new int[][]{{IN_X, IN_Y}, {OUT_X, OUT_Y}, {BONUS_X, OUT_Y}} : new int[][]{{IN_X, IN_Y}, {OUT_X, OUT_Y}};
    }

    @Override
    public void drawExtras(int recipe) {
        super.drawExtras(recipe);
        if (recipe < arecipes.size() && arecipes.get(recipe) instanceof CachedProcessing c) {
            GuiDraw.drawString(String.format("%.1f s", c.recipe.time / 20.0), ARROW_X, 44, 0x404040, false);
            if (c.bonus != null) {
                GuiDraw.drawString(Math.round(c.recipe.bonusChance * 100) + "%", BONUS_X, 44, 0x404040, false);
            }
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals(getOverlayIdentifier())) {
            for (ProcessingRecipes.Recipe r : recipes()) if (!r.inputs().isEmpty()) arecipes.add(new CachedProcessing(r));
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (ProcessingRecipes.Recipe r : recipes()) {
            if (r.inputs().isEmpty()) continue;
            if (NEIServerUtils.areStacksSameTypeCrafting(r.result, result)
                    || (r.bonus != null && NEIServerUtils.areStacksSameTypeCrafting(r.bonus, result))) {
                arecipes.add(new CachedProcessing(r));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (ProcessingRecipes.Recipe r : recipes()) {
            if (!r.inputs().isEmpty() && r.matches(ingredient)) arecipes.add(new CachedProcessing(r));
        }
    }

    public static class Macerating extends ProcessingRecipeHandler {
        @Override
        List<ProcessingRecipes.Recipe> recipes() {
            return ProcessingRecipes.MACERATING;
        }

        @Override
        String blockKey() {
            return "tile.redcircuit.macerator.name";
        }

        @Override
        String name() {
            return "macerating";
        }
    }

    public static class Sawing extends ProcessingRecipeHandler {
        @Override
        List<ProcessingRecipes.Recipe> recipes() {
            return ProcessingRecipes.SAWING;
        }

        @Override
        String blockKey() {
            return "tile.redcircuit.sawmill.name";
        }

        @Override
        String name() {
            return "sawing";
        }
    }
}
