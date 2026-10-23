package pl.geesio.redcircuit.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.AlloyRecipe;
import pl.geesio.redcircuit.registry.ModBlocks;

import java.util.Arrays;
import java.util.List;

/** Kategoria JEI dla receptur pieca do stopów. */
public class AlloyRecipeCategory implements IRecipeCategory<AlloyRecipe> {
    public static final RecipeType<AlloyRecipe> TYPE = RecipeType.create(RedCircuit.MODID, "alloying", AlloyRecipe.class);

    private static final int WIDTH = 130, HEIGHT = 58;
    private static final int OUT_X = 102, OUT_Y = 19;

    private final IDrawable icon;
    private final IDrawableStatic slot;

    public AlloyRecipeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.ALLOY_FURNACE.get()));
        this.slot = gui.getSlotDrawable();
    }

    @Override
    public RecipeType<AlloyRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.redcircuit.alloy_furnace");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlloyRecipe recipe, IFocusGroup focuses) {
        List<SizedIngredient> inputs = recipe.inputs();
        for (int i = 0; i < 9; i++) {
            int x = 1 + (i % 3) * 18, y = 1 + (i / 3) * 18;
            var slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, x, y);
            if (i < inputs.size()) {
                slotBuilder.addItemStacks(Arrays.asList(inputs.get(i).getItems()));
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.result());
    }

    @Override
    public void draw(AlloyRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        for (int i = 0; i < 9; i++) {
            slot.draw(g, (i % 3) * 18, (i / 3) * 18);
        }
        slot.draw(g, OUT_X - 1, OUT_Y - 1);
        // Strzałka (rysowana ręcznie, bez zależności od wersji API)
        int ax = 66, ay = 19;
        g.fill(ax, ay + 6, ax + 16, ay + 10, 0xFF8B8B8B);
        for (int i = 0; i < 6; i++) g.fill(ax + 16 + i, ay + 2 + i, ax + 17 + i, ay + 14 - i, 0xFF8B8B8B);
        String time = String.format(java.util.Locale.ROOT, "%.1f s", recipe.cookTime() / 20.0);
        var font = Minecraft.getInstance().font;
        g.drawString(font, time, 66 + (24 - font.width(time)) / 2, 40, 0xFF808080, false);
    }
}
