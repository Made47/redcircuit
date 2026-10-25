package pl.geesio.redcircuit.wireless;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import pl.geesio.redcircuit.registry.ModDataComponents;

import java.util.List;

/** Pilot: trzymanie PPM nadaje na ustawionej częstotliwości; Shift + PPM otwiera ustawienia. */
public class WirelessRemoteItem extends Item {
    public WirelessRemoteItem(Properties properties) {
        super(properties);
    }

    public static int frequency(ItemStack stack) {
        return WirelessNetwork.clampFreq(stack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                pl.geesio.redcircuit.client.FrequencyScreen.openForItem(hand, frequency(stack), getDescriptionId());
            }
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && level.getServer() != null) {
            WirelessNetwork.get(level.getServer()).pulseRemote(level.getServer(), frequency(stack), player.getUUID());
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.redcircuit.frequency", frequency(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.redcircuit.remote_hint").withStyle(ChatFormatting.GRAY));
    }
}
