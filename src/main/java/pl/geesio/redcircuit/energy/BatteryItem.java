package pl.geesio.redcircuit.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import pl.geesio.redcircuit.registry.ModDataComponents;

import java.util.List;

/**
 * Bateria: przenośny magazyn Azurity (FE). Ładuje się i rozładowuje w akumulatorze
 * (PPM / Shift+PPM), a dzięki zdolności EnergyStorage.ITEM działa też z ładowarkami innych modów.
 */
public class BatteryItem extends Item {
    public static final int CAPACITY = 50_000;
    public static final int RATE = 1_000;

    public BatteryItem(Properties properties) {
        super(properties);
    }

    public static IEnergyStorage storage(ItemStack stack) {
        return new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(), CAPACITY, RATE, RATE);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.set(ModDataComponents.ENERGY.get(), Mth.clamp(energy, 0, CAPACITY));
    }

    public static ItemStack charged(Item item) {
        ItemStack stack = new ItemStack(item);
        setEnergy(stack, CAPACITY);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3C8CFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.redcircuit.battery", getEnergy(stack), CAPACITY)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.redcircuit.battery_hint").withStyle(ChatFormatting.GRAY));
    }
}
