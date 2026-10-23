package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.alloy.ContainerAlloyFurnace;
import pl.geesio.redcircuit.alloy.TileAlloyFurnace;

/** Ekran pieca do stopów - tło rysowane w kodzie (bez pliku tekstury), jak w wersji 1.21. */
public class GuiAlloyFurnace extends GuiContainer {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;
    private static final int BAR_X = 9, BAR_Y = 36, BAR_W = 14, BAR_H = 14;
    private static final int EBAR_X = 10, EBAR_Y = 17, EBAR_W = 12, EBAR_H = 32;

    private final TileAlloyFurnace furnace;

    public GuiAlloyFurnace(InventoryPlayer inv, TileAlloyFurnace furnace) {
        super(new ContainerAlloyFurnace(inv, furnace));
        this.furnace = furnace;
        xSize = 176;
        ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = StatCollector.translateToLocal(furnace.getInventoryName());
        fontRendererObj.drawString(title, (xSize - fontRendererObj.getStringWidth(title)) / 2, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int rx = mouseX - guiLeft, ry = mouseY - guiTop;
        if (furnace.electric && rx >= EBAR_X && rx < EBAR_X + EBAR_W && ry >= EBAR_Y && ry < EBAR_Y + EBAR_H) {
            drawHoveringText(java.util.Collections.singletonList(StatCollector.translateToLocalFormatted("message.redcircuit.energy",
                    furnace.clientEnergy, TileAlloyFurnace.ENERGY_CAPACITY)), mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft, y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, 0xFF000000);
        drawRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, LIGHT);
        drawRect(x + 3, y + 3, x + xSize - 1, y + ySize - 1, DARK);
        drawRect(x + 3, y + 3, x + xSize - 3, y + ySize - 3, BG);

        for (Object o : inventorySlots.inventorySlots) {
            Slot slot = (Slot) o;
            boolean big = slot.xDisplayPosition == ContainerAlloyFurnace.OUT_X && slot.yDisplayPosition == ContainerAlloyFurnace.OUT_Y;
            int sx = x + slot.xDisplayPosition - (big ? 5 : 1), sy = y + slot.yDisplayPosition - (big ? 5 : 1);
            int size = big ? 26 : 18;
            drawRect(sx, sy, sx + size, sy + size, DARK);
            drawRect(sx + 1, sy + 1, sx + size, sy + size, LIGHT);
            drawRect(sx + 1, sy + 1, sx + size - 1, sy + size - 1, SLOT);
        }

        if (furnace.electric) {
            // Pasek energii nad slotem baterii
            drawRect(x + EBAR_X - 1, y + EBAR_Y - 1, x + EBAR_X + EBAR_W + 1, y + EBAR_Y + EBAR_H + 1, 0xFF373737);
            drawRect(x + EBAR_X, y + EBAR_Y, x + EBAR_X + EBAR_W, y + EBAR_Y + EBAR_H, 0xFF1A1A2E);
            int eh = (int) ((long) furnace.clientEnergy * EBAR_H / TileAlloyFurnace.ENERGY_CAPACITY);
            if (eh > 0) drawRect(x + EBAR_X, y + EBAR_Y + EBAR_H - eh, x + EBAR_X + EBAR_W, y + EBAR_Y + EBAR_H, 0xFF3A7BFF);
            if (furnace.getStackInSlot(TileAlloyFurnace.FUEL) == null) {
                GuiEnergyMachine.drawBatteryIcon(x + ContainerAlloyFurnace.FUEL_X, y + ContainerAlloyFurnace.FUEL_Y);
            }
        } else {
        // Płomień: wysokość = pozostałe paliwo
        drawRect(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF8B8B8B);
        int h = (int) ((long) furnace.burnTime * BAR_H / Math.max(1, furnace.burnTotal));
        if (furnace.burnTime > 0 && h > 0) {
            drawRect(x + BAR_X + 2, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W - 2, y + BAR_Y + BAR_H, 0xFFFF8A1C);
            drawRect(x + BAR_X + 5, y + BAR_Y + BAR_H - Math.max(1, h * 2 / 3), x + BAR_X + BAR_W - 5, y + BAR_Y + BAR_H, 0xFFFFE04A);
        }
        }

        // Strzałka postępu
        int ax = x + 90, ay = y + 35, aw = 24;
        drawRect(ax, ay + 6, ax + aw - 6, ay + 10, DARK);
        for (int i = 0; i < 6; i++) drawRect(ax + aw - 6 + i, ay + 2 + i, ax + aw - 5 + i, ay + 14 - i, DARK);
        int p = furnace.progress * aw / Math.max(1, furnace.cookTime);
        if (p > 0) {
            drawRect(ax, ay + 6, ax + Math.min(p, aw - 6), ay + 10, LIGHT);
            for (int i = 0; i < 6 && aw - 6 + i < p; i++) drawRect(ax + aw - 6 + i, ay + 2 + i, ax + aw - 5 + i, ay + 14 - i, LIGHT);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
