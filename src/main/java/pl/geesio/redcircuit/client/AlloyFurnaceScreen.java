package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import pl.geesio.redcircuit.alloy.AlloyFurnaceMenu;

/** Ekran pieca do stopów - tło rysowane w kodzie. */
public class AlloyFurnaceScreen extends AbstractContainerScreen<AlloyFurnaceMenu> {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;
    private static final int BAR_X = 9, BAR_Y = 36, BAR_W = 14, BAR_H = 14;   // płomień
    private static final int EBAR_X = 10, EBAR_Y = 17, EBAR_W = 12, EBAR_H = 32; // energia (pod nim slot baterii)

    public AlloyFurnaceScreen(AlloyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, LIGHT);
        g.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, DARK);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, BG);

        for (Slot slot : menu.slots) {
            boolean big = slot.x == AlloyFurnaceMenu.OUT_X && slot.y == AlloyFurnaceMenu.OUT_Y;
            int sx = x + slot.x - (big ? 5 : 1), sy = y + slot.y - (big ? 5 : 1);
            int size = big ? 26 : 18;
            g.fill(sx, sy, sx + size, sy + size, DARK);
            g.fill(sx + 1, sy + 1, sx + size, sy + size, LIGHT);
            g.fill(sx + 1, sy + 1, sx + size - 1, sy + size - 1, SLOT);
        }

        if (menu.isElectric()) {
            // Pasek energii
            g.fill(x + EBAR_X - 1, y + EBAR_Y - 1, x + EBAR_X + EBAR_W + 1, y + EBAR_Y + EBAR_H + 1, 0xFF373737);
            g.fill(x + EBAR_X, y + EBAR_Y, x + EBAR_X + EBAR_W, y + EBAR_Y + EBAR_H, 0xFF1A1A2E);
            int h = (int) ((long) menu.power() * EBAR_H / Math.max(1, menu.powerMax()));
            if (h > 0) g.fill(x + EBAR_X, y + EBAR_Y + EBAR_H - h, x + EBAR_X + EBAR_W, y + EBAR_Y + EBAR_H, 0xFF3A7BFF);
            if (!menu.getSlot(10).hasItem()) {
                ElectricFurnaceScreen.drawBatteryIcon(g, x + AlloyFurnaceMenu.FUEL_X, y + AlloyFurnaceMenu.FUEL_Y);
            }
        } else {
            // Płomień: wysokość = pozostałe paliwo
            g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF8B8B8B);
            int h = (int) ((long) menu.power() * BAR_H / Math.max(1, menu.powerMax()));
            if (h > 0) {
                g.fill(x + BAR_X + 2, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W - 2, y + BAR_Y + BAR_H, 0xFFFF8A1C);
                g.fill(x + BAR_X + 5, y + BAR_Y + BAR_H - Math.max(1, h * 2 / 3), x + BAR_X + BAR_W - 5, y + BAR_Y + BAR_H, 0xFFFFE04A);
            }
        }

        // Strzałka postępu
        int ax = x + 90, ay = y + 35, aw = 24;
        g.fill(ax, ay + 6, ax + aw - 6, ay + 10, DARK);
        for (int i = 0; i < 6; i++) g.fill(ax + aw - 6 + i, ay + 2 + i, ax + aw - 5 + i, ay + 14 - i, DARK);
        int p = menu.progress() * aw / Math.max(1, menu.cookTime());
        if (p > 0) {
            g.fill(ax, ay + 6, ax + Math.min(p, aw - 6), ay + 10, LIGHT);
            for (int i = 0; i < 6 && aw - 6 + i < p; i++) g.fill(ax + aw - 6 + i, ay + 2 + i, ax + aw - 5 + i, ay + 14 - i, LIGHT);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (menu.isElectric()) {
            int rx = mouseX - leftPos, ry = mouseY - topPos;
            if (rx >= EBAR_X && rx < EBAR_X + EBAR_W && ry >= EBAR_Y && ry < EBAR_Y + EBAR_H) {
                g.renderTooltip(font, Component.translatable("message.redcircuit.energy", menu.power(), menu.powerMax()), mouseX, mouseY);
            }
        }
    }
}
