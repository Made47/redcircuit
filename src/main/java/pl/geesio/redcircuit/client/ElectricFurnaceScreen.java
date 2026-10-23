package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import pl.geesio.redcircuit.energy.ElectricFurnaceMenu;

public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    private static final int BAR_X = 32, BAR_Y = 17, BAR_W = 12, BAR_H = 52;

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight);

        // Zakryj płomień; slot paliwa z tła służy teraz jako slot baterii.
        g.fill(x + 54, y + 35, x + 76, y + 52, 0xFFC6C6C6);
        if (!menu.getSlot(2).hasItem()) drawBatteryIcon(g, x + 56, y + 53);
        g.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_W + 1, y + BAR_Y + BAR_H + 1, 0xFF373737);
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF1A1A2E);
        int max = Math.max(1, menu.getMaxEnergy());
        int h = (int) ((long) menu.getEnergy() * BAR_H / max);
        if (h > 0) {
            g.fill(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF3A7BFF);
        }

        // Postęp przetapiania - pasek pod strzałką.
        int maxP = Math.max(1, menu.getMaxProgress());
        int w = menu.getProgress() * 22 / maxP;
        if (w > 0) {
            g.fill(x + 80, y + 51, x + 80 + w, y + 54, 0xFFFF7A2E);
        }
    }

    /** Szary zarys baterii w pustym slocie. */
    static void drawBatteryIcon(GuiGraphics g, int sx, int sy) {
        int c = 0x60000000;
        g.fill(sx + 6, sy + 2, sx + 10, sy + 4, c);
        g.fill(sx + 4, sy + 4, sx + 12, sy + 14, c);
        g.fill(sx + 7, sy + 7, sx + 9, sy + 11, 0x30FFFFFF);
        g.fill(sx + 6, sy + 8, sx + 10, sy + 10, 0x30FFFFFF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= BAR_X && rx < BAR_X + BAR_W && ry >= BAR_Y && ry < BAR_Y + BAR_H) {
            g.renderTooltip(font, Component.translatable("message.redcircuit.energy",
                    menu.getEnergy(), menu.getMaxEnergy()), mouseX, mouseY);
        }
    }
}
