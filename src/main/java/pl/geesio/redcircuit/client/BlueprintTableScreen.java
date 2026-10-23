package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import pl.geesio.redcircuit.craft.BlueprintTableMenu;

/** Ekran stołu kreślarskiego - tło rysowane w kodzie (bez własnej tekstury GUI). */
public class BlueprintTableScreen extends AbstractContainerScreen<BlueprintTableMenu> {
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B;

    public BlueprintTableScreen(BlueprintTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("✎"), b -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BlueprintTableMenu.BUTTON_PLAN);
                    }
                })
                .bounds(leftPos + 13, topPos + 58, 24, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.redcircuit.blueprint_table.plan")))
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // Panel z obramowaniem w stylu okien Minecrafta
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, LIGHT);
        g.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, DARK);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, BG);

        // Tła slotów
        for (Slot slot : menu.slots) {
            boolean big = slot.index == BlueprintTableMenu.RESULT;
            int sx = x + slot.x - (big ? 5 : 1), sy = y + slot.y - (big ? 5 : 1);
            int size = big ? 26 : 18;
            g.fill(sx, sy, sx + size, sy + size, DARK);
            g.fill(sx + 1, sy + 1, sx + size, sy + size, LIGHT);
            g.fill(sx + 1, sy + 1, sx + size - 1, sy + size - 1, SLOT);
        }

        // Strzałka siatka -> wynik
        int ax = x + 110, ay = y + 40;
        g.fill(ax, ay, ax + 16, ay + 4, DARK);
        for (int i = 0; i < 5; i++) {
            g.fill(ax + 16 + i, ay - 4 + i, ax + 17 + i, ay + 8 - i, DARK);
        }
        // Podpis magazynu
        g.drawString(font, Component.translatable("gui.redcircuit.blueprint_table.storage"), x + 8, y + 79, 0xFF404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
