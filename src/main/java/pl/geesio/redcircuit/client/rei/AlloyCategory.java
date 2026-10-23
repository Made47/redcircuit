package pl.geesio.redcircuit.client.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import pl.geesio.redcircuit.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kategoria REI "Piec do stopów": siatka 3x3 składników, strzałka z czasem, wynik. */
public class AlloyCategory implements DisplayCategory<AlloyDisplay> {
    @Override
    public CategoryIdentifier<? extends AlloyDisplay> getCategoryIdentifier() {
        return RedCircuitReiPlugin.ALLOYING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.redcircuit.alloy_furnace");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ModBlocks.ALLOY_FURNACE.get());
    }

    @Override
    public int getDisplayHeight() {
        return 66;
    }

    @Override
    public int getDisplayWidth(AlloyDisplay display) {
        return 140;
    }

    @Override
    public List<Widget> setupDisplay(AlloyDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        int x = bounds.x + 5, y = bounds.y + 5;
        List<EntryIngredient> inputs = display.getInputEntries();
        for (int i = 0; i < 9; i++) {
            var slot = Widgets.createSlot(new Point(x + (i % 3) * 18 + 1, y + (i / 3) * 18 + 1)).markInput();
            if (i < inputs.size()) slot.entries(inputs.get(i));
            widgets.add(slot);
        }
        widgets.add(Widgets.createArrow(new Point(x + 64, y + 18)).animationDurationTicks(Math.max(1, display.cookTime())));
        widgets.add(Widgets.createResultSlotBackground(new Point(x + 100, y + 19)));
        widgets.add(Widgets.createSlot(new Point(x + 100, y + 19)).entries(display.getOutputEntries().get(0))
                .disableBackground().markOutput());
        widgets.add(Widgets.createLabel(new Point(x + 76, y + 42),
                Component.literal(String.format(Locale.ROOT, "%.1f s", display.cookTime() / 20.0))).noShadow().color(0xFF404040, 0xFFBBBBBB));
        return widgets;
    }
}
