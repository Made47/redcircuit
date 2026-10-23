package pl.geesio.redcircuit.compat.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.recipe.TemplateRecipeHandler;

import java.awt.Rectangle;

/** Wspólne rysowanie: tło slotów i strzałka postępu wycięte z tekstury pieca z Minecrafta. */
abstract class RCRecipeHandler extends TemplateRecipeHandler {
    static final String FURNACE = "textures/gui/container/furnace.png";
    static final int ARROW_X = 74, ARROW_Y = 23;

    @Override
    public String getGuiTexture() {
        return FURNACE;
    }

    /** Pozycje slotów (lewy górny róg przedmiotu) do narysowania. */
    abstract int[][] slots(int recipe);

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(arrowX(), ARROW_Y, 24, 18), getOverlayIdentifier()));
    }

    int arrowX() {
        return ARROW_X;
    }

    @Override
    public void drawBackground(int recipe) {
        GuiDraw.changeTexture(FURNACE);
        for (int[] s : slots(recipe)) GuiDraw.drawTexturedModalRect(s[0] - 1, s[1] - 1, 55, 16, 18, 18);
        GuiDraw.drawTexturedModalRect(arrowX(), ARROW_Y, 79, 34, 24, 17);
    }

    @Override
    public void drawExtras(int recipe) {
        GuiDraw.changeTexture(FURNACE);
        int w = (cycleticks % 48) / 2;   // animowana strzałka
        GuiDraw.drawTexturedModalRect(arrowX(), ARROW_Y, 176, 14, w, 17);
    }
}
