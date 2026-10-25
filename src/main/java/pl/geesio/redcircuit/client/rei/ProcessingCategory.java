package pl.geesio.redcircuit.client.rei;

import pl.geesio.redcircuit.compat.rei.AlloyDisplay;
import pl.geesio.redcircuit.compat.rei.ProcessingDisplay;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kategoria REI maceratora / tartaku: składnik -> wynik (+ dodatek z szansą). */
public class ProcessingCategory implements DisplayCategory<ProcessingDisplay> {
    private final CategoryIdentifier<ProcessingDisplay> id;
    private final ItemLike icon;
    private final String titleKey;

    public ProcessingCategory(CategoryIdentifier<ProcessingDisplay> id, ItemLike icon, String titleKey) {
        this.id = id;
        this.icon = icon;
        this.titleKey = titleKey;
    }

    @Override
    public CategoryIdentifier<? extends ProcessingDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(titleKey);
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }

    @Override
    public int getDisplayHeight() {
        return 44;
    }

    @Override
    public int getDisplayWidth(ProcessingDisplay display) {
        return 120;
    }

    @Override
    public List<Widget> setupDisplay(ProcessingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        int x = bounds.x + 5, y = bounds.y + 5;
        widgets.add(Widgets.createSlot(new Point(x + 1, y + 1)).entries(display.getInputEntries().get(0)).markInput());
        widgets.add(Widgets.createArrow(new Point(x + 26, y + 1)).animationDurationTicks(Math.max(1, display.time())));
        widgets.add(Widgets.createSlot(new Point(x + 61, y + 1)).entries(display.getOutputEntries().get(0)).markOutput());
        if (display.hasBonus()) {
            widgets.add(Widgets.createSlot(new Point(x + 87, y + 1)).entries(display.getOutputEntries().get(1)).markOutput());
            widgets.add(Widgets.createLabel(new Point(x + 95, y + 22),
                    Component.literal(Math.round(display.bonusChance() * 100) + "%")).noShadow().color(0xFF404040, 0xFFBBBBBB));
        }
        widgets.add(Widgets.createLabel(new Point(x + 38, y + 22),
                Component.literal(String.format(Locale.ROOT, "%.1f s", display.time() / 20.0))).noShadow().color(0xFF404040, 0xFFBBBBBB));
        return widgets;
    }
}
