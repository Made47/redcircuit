package pl.geesio.redcircuit.craft;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zapisany plan (projekt) receptury. Przechowuje 9 składników siatki i wynik
 * w komponencie kontenera: indeksy 0..8 = siatka 3x3, indeks 9 = wynik.
 */
public class PlanItem extends Item {
    public static final int RESULT_INDEX = 9;

    public PlanItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(ItemStack planItem, List<ItemStack> grid, ItemStack result) {
        NonNullList<ItemStack> data = NonNullList.withSize(10, ItemStack.EMPTY);
        for (int i = 0; i < 9 && i < grid.size(); i++) {
            data.set(i, grid.get(i).copyWithCount(Math.min(1, grid.get(i).getCount())));
        }
        data.set(RESULT_INDEX, result.copy());
        ItemStack plan = planItem.copyWithCount(1);
        plan.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(data));
        return plan;
    }

    /** Zwraca 10 stosów: 9 składników + wynik (pusta lista, jeśli plan nie ma zapisu). */
    public static NonNullList<ItemStack> read(ItemStack plan) {
        NonNullList<ItemStack> data = NonNullList.withSize(10, ItemStack.EMPTY);
        ItemContainerContents contents = plan.get(DataComponents.CONTAINER);
        if (contents != null) contents.copyInto(data);
        return data;
    }

    public static ItemStack result(ItemStack plan) {
        return read(plan).get(RESULT_INDEX);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack result = result(stack);
        if (result.isEmpty()) return super.getName(stack);
        return Component.translatable("item.redcircuit.plan.named", result.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        NonNullList<ItemStack> data = read(stack);
        ItemStack result = data.get(RESULT_INDEX);
        if (result.isEmpty()) return;
        tooltip.accept(Component.translatable("tooltip.redcircuit.plan.result", result.getCount(), result.getHoverName())
                .withStyle(ChatFormatting.GRAY));
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Component> names = new LinkedHashMap<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = data.get(i);
            if (s.isEmpty()) continue;
            String key = s.getItem().toString();
            counts.merge(key, 1, Integer::sum);
            names.putIfAbsent(key, s.getHoverName());
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            tooltip.accept(Component.literal(" " + e.getValue() + " x ").append(names.get(e.getKey()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
