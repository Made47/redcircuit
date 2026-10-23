package pl.geesio.redcircuit.craft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * Piła ręczna: narzędzie używane w siatce wytwarzania (np. składnik tartaku).
 * Po każdym wytworzeniu zostaje w siatce i traci 1 punkt wytrzymałości.
 */
public class HandsawItem extends Item {
    private final Predicate<ItemStack> repairMaterial;

    public HandsawItem(Properties properties, Predicate<ItemStack> repairMaterial) {
        super(properties);
        this.repairMaterial = repairMaterial;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setDamageValue(copy.getDamageValue() + 1);
        return copy.getDamageValue() >= copy.getMaxDamage() ? ItemStack.EMPTY : copy;
    }

    /** Wytrzymałość zawsze widoczna w opisie (nie tylko z F3+H). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int max = stack.getMaxDamage();
        int left = max - stack.getDamageValue();
        ChatFormatting color = left > max / 2 ? ChatFormatting.GREEN : left > max / 5 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.add(Component.translatable("tooltip.redcircuit.durability", left, max).withStyle(color));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairMaterial.test(repairCandidate);
    }
}
