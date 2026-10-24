package pl.geesio.redcircuit.craft;

import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStackTemplate;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Piła ręczna: narzędzie używane w siatce wytwarzania (np. składnik tartaku).
 * Po każdym wytworzeniu zostaje w siatce i traci 1 punkt wytrzymałości.
 */
public class HandsawItem extends Item {

    public HandsawItem(Properties properties) {
        super(properties);
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        if (!(instance instanceof ItemStack stack)) return new ItemStackTemplate(this);
        ItemStack copy = stack.copyWithCount(1);
        copy.setDamageValue(copy.getDamageValue() + 1);
        return copy.getDamageValue() >= copy.getMaxDamage() ? null : ItemStackTemplate.fromNonEmptyStack(copy);
    }

    /** Wytrzymałość zawsze widoczna w opisie (nie tylko z F3+H). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int max = stack.getMaxDamage();
        int left = max - stack.getDamageValue();
        ChatFormatting color = left > max / 2 ? ChatFormatting.GREEN : left > max / 5 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.accept(Component.translatable("tooltip.redcircuit.durability", left, max).withStyle(color));
    }

}
