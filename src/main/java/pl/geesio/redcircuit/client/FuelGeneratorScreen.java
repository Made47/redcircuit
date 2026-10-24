package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import pl.geesio.redcircuit.energy.FuelGeneratorMenu;

/** Ekran generatora na paliwo stałe: slot paliwa, płomień (pozostały czas) i pasek energii. */
public class FuelGeneratorScreen extends AbstractContainerScreen<FuelGeneratorMenu> {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;
    private static final int BAR_X = 120, BAR_Y = 17, BAR_W = 16, BAR_H = 52;
    private static final int FLAME_X = 81, FLAME_Y = 33, FLAME = 14;

    public FuelGeneratorScreen(FuelGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, LIGHT);
        g.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, DARK);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, BG);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, DARK);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, LIGHT);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
        }

        // Płomień: tło (zgaszony) i wypełnienie od dołu wg pozostałego czasu spalania
        int fx = x + FLAME_X, fy = y + FLAME_Y;
        drawFlame(g, fx, fy, FLAME, 0xFF6B6B6B);
        int h = Math.round(menu.getBurnFraction() * FLAME);
        if (h > 0) {
            g.enableScissor(fx, fy + FLAME - h, fx + FLAME, fy + FLAME);
            drawFlame(g, fx, fy, FLAME, 0xFFFF9A1F);
            g.disableScissor();
        }

        g.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_W + 1, y + BAR_Y + BAR_H + 1, 0xFF373737);
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF1A1A2E);
        int eh = (int) ((long) menu.getEnergy() * BAR_H / Math.max(1, menu.getMaxEnergy()));
        if (eh > 0) g.fill(x + BAR_X, y + BAR_Y + BAR_H - eh, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF3A7BFF);
    }

    /** Prosty płomień z prostokątów (szerszy u dołu, węższy u góry). */
    private static void drawFlame(GuiGraphicsExtractor g, int x, int y, int size, int color) {
        for (int row = 0; row < size; row++) {
            float t = row / (float) (size - 1);              // 0 = góra, 1 = dół
            int half = Math.max(1, Math.round((size / 2f) * (0.25f + 0.75f * (float) Math.sin(t * Math.PI * 0.62))));
            int cx = x + size / 2;
            g.fill(cx - half, y + row, cx + half, y + row + 1, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, Component.translatable("gui.redcircuit.generating", menu.getRate()), 8, 22, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= BAR_X && rx < BAR_X + BAR_W && ry >= BAR_Y && ry < BAR_Y + BAR_H) {
            g.setTooltipForNextFrame(font, Component.translatable("message.redcircuit.energy", menu.getEnergy(), menu.getMaxEnergy()), mouseX, mouseY);
        }
    }
}
