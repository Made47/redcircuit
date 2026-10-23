package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.energy.ContainerEnergyMachine;
import pl.geesio.redcircuit.energy.GuiLayout;
import pl.geesio.redcircuit.energy.TileEnergyInv;

import java.util.Collections;

/** Okno maszyny energii - tło rysowane w kodzie (jak w wersji 1.21): sloty, pasek energii, postęp, płomień. */
public class GuiEnergyMachine extends GuiContainer {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;

    private final TileEnergyInv te;

    public GuiEnergyMachine(InventoryPlayer inv, TileEnergyInv te) {
        super(new ContainerEnergyMachine(inv, te));
        this.te = te;
        xSize = 176;
        ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = StatCollector.translateToLocal(te.getInventoryName());
        fontRendererObj.drawString(title, (xSize - fontRendererObj.getStringWidth(title)) / 2, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int[] b = te.layout().energyBar;
        if (b == null) return;
        int rx = mouseX - guiLeft, ry = mouseY - guiTop;
        if (rx >= b[0] && rx < b[0] + b[2] && ry >= b[1] && ry < b[1] + b[3]) {
            drawHoveringText(Collections.singletonList(StatCollector.translateToLocalFormatted("message.redcircuit.energy",
                    te.shown(TileEnergyInv.F_ENERGY), te.shown(TileEnergyInv.F_CAPACITY))), mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft, y = guiTop;
        GuiLayout layout = te.layout();
        drawRect(x, y, x + xSize, y + ySize, 0xFF000000);
        drawRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, LIGHT);
        drawRect(x + 3, y + 3, x + xSize - 1, y + ySize - 1, DARK);
        drawRect(x + 3, y + 3, x + xSize - 3, y + ySize - 3, BG);

        int machineSlots = layout.slots.size();
        for (int i = 0; i < inventorySlots.inventorySlots.size(); i++) {
            Slot slot = (Slot) inventorySlots.inventorySlots.get(i);
            boolean big = i < machineSlots && layout.slots.get(i).kind == GuiLayout.Kind.OUTPUT;
            int sx = x + slot.xDisplayPosition - (big ? 5 : 1), sy = y + slot.yDisplayPosition - (big ? 5 : 1);
            int size = big ? 26 : 18;
            drawRect(sx, sy, sx + size, sy + size, DARK);
            drawRect(sx + 1, sy + 1, sx + size, sy + size, LIGHT);
            drawRect(sx + 1, sy + 1, sx + size - 1, sy + size - 1, SLOT);
            if (i < machineSlots && layout.slots.get(i).kind == GuiLayout.Kind.BATTERY && !slot.getHasStack()) {
                drawBatteryIcon(x + slot.xDisplayPosition, y + slot.yDisplayPosition);
            }
        }

        if (layout.energyBar != null) {
            int[] b = layout.energyBar;
            drawRect(x + b[0] - 1, y + b[1] - 1, x + b[0] + b[2] + 1, y + b[1] + b[3] + 1, 0xFF373737);
            drawRect(x + b[0], y + b[1], x + b[0] + b[2], y + b[1] + b[3], 0xFF1A1A2E);
            int h = (int) ((long) te.shown(TileEnergyInv.F_ENERGY) * b[3] / Math.max(1, te.shown(TileEnergyInv.F_CAPACITY)));
            if (h > 0) drawRect(x + b[0], y + b[1] + b[3] - h, x + b[0] + b[2], y + b[1] + b[3], 0xFF3A7BFF);
        }

        if (layout.flame != null) {
            int fx = x + layout.flame[0], fy = y + layout.flame[1], f = 14;
            drawRect(fx, fy, fx + f, fy + f, SLOT);
            int burn = te.shown(TileEnergyInv.F_BURN), total = Math.max(1, te.shown(TileEnergyInv.F_BURN_TOTAL));
            int h = burn > 0 ? Math.max(1, (int) ((long) burn * f / total)) : 0;
            if (h > 0) {
                drawRect(fx + 2, fy + f - h, fx + f - 2, fy + f, 0xFFFF8A1C);
                drawRect(fx + 5, fy + f - Math.max(1, h * 2 / 3), fx + f - 5, fy + f, 0xFFFFE04A);
            }
        }

        if (layout.arrow != null) {
            int ax = x + layout.arrow[0], ay = y + layout.arrow[1], aw = 24;
            drawRect(ax, ay + 6, ax + aw - 6, ay + 10, DARK);
            for (int i = 0; i < 6; i++) drawRect(ax + aw - 6 + i, ay + 2 + i, ax + aw - 5 + i, ay + 14 - i, DARK);
            int p = te.shown(TileEnergyInv.F_PROGRESS) * aw / Math.max(1, te.shown(TileEnergyInv.F_MAX_PROGRESS));
            if (p > 0) {
                drawRect(ax, ay + 6, ax + Math.min(p, aw - 6), ay + 10, LIGHT);
                for (int i = 0; i < 6 && aw - 6 + i < p; i++) drawRect(ax + aw - 6 + i, ay + 2 + i, ax + aw - 5 + i, ay + 14 - i, LIGHT);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Szary zarys baterii w pustym slocie. */
    static void drawBatteryIcon(int sx, int sy) {
        int c = 0x60000000;
        drawRect(sx + 6, sy + 2, sx + 10, sy + 4, c);
        drawRect(sx + 4, sy + 4, sx + 12, sy + 14, c);
        drawRect(sx + 7, sy + 7, sx + 9, sy + 11, 0x30FFFFFF);
        drawRect(sx + 6, sy + 8, sx + 10, sy + 10, 0x30FFFFFF);
    }
}
