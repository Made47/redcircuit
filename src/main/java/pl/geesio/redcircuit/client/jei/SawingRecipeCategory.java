package pl.geesio.redcircuit.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.SawingRecipe;
import pl.geesio.redcircuit.registry.ModBlocks;

import java.util.Arrays;

/** Kategoria JEI dla receptur tartaku. */
public class SawingRecipeCategory implements IRecipeCategory<SawingRecipe> {
    public static final IRecipeType<SawingRecipe> TYPE = IRecipeType.create(RedCircuit.MODID, "sawing", SawingRecipe.class);

    private static final int WIDTH = 110, HEIGHT = 34;

    private final IDrawable icon;
    private final IDrawableStatic slot;

    public SawingRecipeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.SAWMILL.get()));
        this.slot = gui.getSlotDrawable();
    }

    @Override
    public IRecipeType<SawingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.redcircuit.sawmill");
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
    public void setRecipe(IRecipeLayoutBuilder builder, SawingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).add(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 1).add(recipe.result());
        if (!recipe.bonus().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 87, 1).add(recipe.bonus());
        }
    }

    @Override
    public void draw(SawingRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        slot.draw(g, 0, 0);
        slot.draw(g, 60, 0);
        int ax = 26, ay = 1;
        g.fill(ax, ay + 6, ax + 16, ay + 10, 0xFF8B8B8B);
        for (int i = 0; i < 6; i++) g.fill(ax + 16 + i, ay + 2 + i, ax + 17 + i, ay + 14 - i, 0xFF8B8B8B);
        var font = Minecraft.getInstance().font;
        if (!recipe.bonus().isEmpty()) {
            slot.draw(g, 86, 0);
            String pct = Math.round(recipe.bonusChance() * 100) + "%";
            g.text(font, pct, 87 + (16 - font.width(pct)) / 2, 22, 0xFF808080, false);
        }
        String time = String.format(java.util.Locale.ROOT, "%.1f s", recipe.time() / 20.0);
        g.text(font, time, 24 + (24 - font.width(time)) / 2, 22, 0xFF808080, false);
    }
}
