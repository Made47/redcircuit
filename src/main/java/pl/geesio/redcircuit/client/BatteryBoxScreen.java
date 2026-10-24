package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import pl.geesio.redcircuit.energy.BatteryBoxMenu;

/** Ekran akumulatora: slot ładowania (góra), rozładowania (dół) i pasek energii. */
public class BatteryBoxScreen extends AbstractContainerScreen<BatteryBoxMenu> {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;
    private static final int BAR_X = 80, BAR_Y = 17, BAR_W = 16, BAR_H = 52;

    public BatteryBoxScreen(BatteryBoxMenu menu, Inventory inventory, Component title) {
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
        for (int i = 0; i < 2; i++) {
            if (!menu.getSlot(i).hasItem()) {
                ElectricFurnaceScreen.drawBatteryIcon(g, x + menu.getSlot(i).x, y + menu.getSlot(i).y);
            }
        }

        // Strzałki: akumulator -> bateria (góra), bateria -> akumulator (dół)
        arrow(g, x + 64, y + 21, false);
        arrow(g, x + 64, y + 57, true);

        g.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_W + 1, y + BAR_Y + BAR_H + 1, 0xFF373737);
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF1A1A2E);
        int h = (int) ((long) menu.getEnergy() * BAR_H / Math.max(1, menu.getMaxEnergy()));
        if (h > 0) g.fill(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF3A7BFF);
    }

    /** Mała strzałka: w lewo (do baterii) albo w prawo (do akumulatora). */
    private static void arrow(GuiGraphicsExtractor g, int ax, int ay, boolean right) {
        int c = 0xFF555555;
        g.fill(ax + 3, ay + 3, ax + 11, ay + 5, c);
        for (int i = 0; i < 4; i++) {
            int px = right ? ax + 11 + i : ax + 3 - i;
            g.fill(px, ay + i, px + 1, ay + 8 - i, c);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, Component.translatable("gui.redcircuit.charge"), 104, 21, 0xFF404040, false);
        g.text(font, Component.translatable("gui.redcircuit.discharge"), 104, 57, 0xFF404040, false);
        String fe = String.format(java.util.Locale.ROOT, "%,d FE", menu.getEnergy()).replace(',', ' ');
        g.text(font, fe, 104, 39, 0xFF2A5FD0, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= BAR_X && rx < BAR_X + BAR_W && ry >= BAR_Y && ry < BAR_Y + BAR_H) {
            g.setTooltipForNextFrame(font, Component.translatable("message.redcircuit.energy",
                    menu.getEnergy(), menu.getMaxEnergy()), mouseX, mouseY);
        }
    }
}
