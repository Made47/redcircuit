package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.craft.ContainerBlueprintTable;
import pl.geesio.redcircuit.craft.TileBlueprintTable;

import java.util.Collections;

/** Ekran stołu kreślarskiego - tło rysowane w kodzie (bez własnej tekstury GUI), jak w wersji 1.21. */
public class GuiBlueprintTable extends GuiContainer {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;
    private GuiButton planButton;

    public GuiBlueprintTable(InventoryPlayer inv, TileBlueprintTable te) {
        super(new ContainerBlueprintTable(inv, te));
        xSize = 176;
        ySize = 222;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        planButton = new GuiButton(0, guiLeft + 9, guiTop + 57, 32, 20, StatCollector.translateToLocal("item.redcircuit.plan.name"));
        buttonList.add(planButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == planButton) mc.playerController.sendEnchantPacket(inventorySlots.windowId, ContainerBlueprintTable.BUTTON_PLAN);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (planButton != null && mouseX >= planButton.xPosition && mouseX < planButton.xPosition + 32
                && mouseY >= planButton.yPosition && mouseY < planButton.yPosition + 20) {
            drawHoveringText(Collections.singletonList(StatCollector.translateToLocal("gui.redcircuit.blueprint_table.plan")),
                    mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = StatCollector.translateToLocal("tile.redcircuit.blueprint_table.name");
        fontRendererObj.drawString(title, (xSize - fontRendererObj.getStringWidth(title)) / 2, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.redcircuit.blueprint_table.storage"), 8, 79, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft, y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, 0xFF000000);
        drawRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, LIGHT);
        drawRect(x + 3, y + 3, x + xSize - 1, y + ySize - 1, DARK);
        drawRect(x + 3, y + 3, x + xSize - 3, y + ySize - 3, BG);
        for (int i = 0; i < inventorySlots.inventorySlots.size(); i++) {
            Slot slot = (Slot) inventorySlots.inventorySlots.get(i);
            boolean big = i == ContainerBlueprintTable.RESULT;
            int sx = x + slot.xDisplayPosition - (big ? 5 : 1), sy = y + slot.yDisplayPosition - (big ? 5 : 1);
            int size = big ? 26 : 18;
            drawRect(sx, sy, sx + size, sy + size, DARK);
            drawRect(sx + 1, sy + 1, sx + size, sy + size, LIGHT);
            drawRect(sx + 1, sy + 1, sx + size - 1, sy + size - 1, SLOT);
        }
        // Strzałka siatka -> wynik
        int ax = x + 110, ay = y + 40;
        drawRect(ax, ay, ax + 16, ay + 4, DARK);
        for (int i = 0; i < 5; i++) drawRect(ax + 16 + i, ay - 4 + i, ax + 17 + i, ay + 8 - i, DARK);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }
}
