package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import pl.geesio.redcircuit.energy.SawmillMenu;

/** Ekran tartaku: wejście, bateria, strzałka postępu, wynik i produkt uboczny, pasek energii. */
public class SawmillScreen extends AbstractContainerScreen<SawmillMenu> {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;
    private static final int BAR_X = 32, BAR_Y = 17, BAR_W = 12, BAR_H = 52;

    public SawmillScreen(SawmillMenu menu, Inventory inventory, Component title) {
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
        if (!menu.getSlot(3).hasItem()) ElectricFurnaceScreen.drawBatteryIcon(g, x + 56, y + 53);

        // Tarcza piły między wejściem a baterią
        int cx = x + 64, cy = y + 44;
        g.fill(cx - 5, cy - 1, cx + 6, cy + 2, 0xFF8A8A8A);
        g.fill(cx - 1, cy - 5, cx + 2, cy + 6, 0xFF8A8A8A);
        g.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xFFA8A8A8);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF505050);

        // Strzałka postępu
        int ax = x + 80, ay = y + 35;
        g.fill(ax, ay + 6, ax + 16, ay + 10, 0xFF8B8B8B);
        for (int i = 0; i < 6; i++) g.fill(ax + 16 + i, ay + 2 + i, ax + 17 + i, ay + 14 - i, 0xFF8B8B8B);
        int max = Math.max(1, menu.getMaxProgress());
        int w = menu.getProgress() * 22 / max;
        if (w > 0) {
            g.enableScissor(ax, ay, ax + w, ay + 16);
            g.fill(ax, ay + 6, ax + 16, ay + 10, 0xFFFFFFFF);
            for (int i = 0; i < 6; i++) g.fill(ax + 16 + i, ay + 2 + i, ax + 17 + i, ay + 14 - i, 0xFFFFFFFF);
            g.disableScissor();
        }

        g.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_W + 1, y + BAR_Y + BAR_H + 1, 0xFF373737);
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF1A1A2E);
        int h = (int) ((long) menu.getEnergy() * BAR_H / Math.max(1, menu.getMaxEnergy()));
        if (h > 0) g.fill(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF3A7BFF);
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
