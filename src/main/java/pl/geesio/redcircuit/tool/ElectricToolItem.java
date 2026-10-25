package pl.geesio.redcircuit.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.energy.ComponentEnergyStorage;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import pl.geesio.redcircuit.registry.ModDataComponents;

import java.util.List;

/**
 * Narzędzie elektryczne (wiertło, piła łańcuchowa): zamiast wytrzymałości zużywa energię (FE).
 * Bez energii kopie bardzo wolno. Ładuje się w ładowarce, akumulatorze albo ładowarkach innych modów.
 */
public class ElectricToolItem extends Item {
    public static final int CAPACITY = 100_000, RATE = 1_000, COST_PER_BLOCK = 50, COST_PER_HIT = 100;

    /** Poziom jak diament: kopie obsydian, prędkość 10. Zamiast wytrzymałości zużywa energię. */
    public static final float SPEED = 10.0f;
    public static final float DAMAGE_BONUS = 3.0f;

    private final boolean areaCapable;
    /** Zapobiega rekurencji: rozbijanie bloków obok wywołuje mineBlock ponownie. */
    private static final ThreadLocal<Boolean> BREAKING_AREA = ThreadLocal.withInitial(() -> false);

    public ElectricToolItem(TagKey<Block> blocks, float attackDamage, float attackSpeed, Properties properties) {
        this(blocks, attackDamage, attackSpeed, properties, false);
    }

    /** @param areaCapable czy narzędzie ma tryb 3x3 (wiertarka) */
    public ElectricToolItem(TagKey<Block> blocks, float attackDamage, float attackSpeed, Properties properties, boolean areaCapable) {
        super(toolProperties(properties.stacksTo(1), blocks, attackDamage, attackSpeed));
        this.areaCapable = areaCapable;
    }

    private static Properties toolProperties(Properties properties, TagKey<Block> blocks, float attackDamage, float attackSpeed) {
        HolderGetter<Block> lookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        return properties
                .enchantable(12)
                .component(DataComponents.TOOL, new Tool(List.of(
                        Tool.Rule.deniesDrops(lookup.getOrThrow(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)),
                        Tool.Rule.minesAndDrops(lookup.getOrThrow(blocks), SPEED)), 1.0f, 0, true))
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                                attackDamage + DAMAGE_BONUS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                                attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    public boolean isAreaMode(ItemStack stack) {
        return areaCapable && stack.getOrDefault(ModDataComponents.DRILL_AREA.get(), false);
    }

    // ------------------------------------------------------------------ przełączanie trybu

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!areaCapable || !player.isShiftKeyDown()) return InteractionResult.PASS;
        toggle(level, player, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (areaCapable && player != null && player.isShiftKeyDown()) {
            toggle(ctx.getLevel(), player, ctx.getItemInHand());
            return InteractionResult.SUCCESS;
        }
        return super.useOn(ctx);
    }

    private void toggle(Level level, Player player, ItemStack stack) {
        if (level.isClientSide()) return;
        boolean area = !isAreaMode(stack);
        stack.set(ModDataComponents.DRILL_AREA.get(), area);
        player.sendOverlayMessage(Component.translatable(area ? "message.redcircuit.drill_3x3" : "message.redcircuit.drill_1x1")
                .withStyle(ChatFormatting.AQUA));
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, area ? 1.4f : 1.0f);
    }

    // ------------------------------------------------------------------ kopanie 3x3

    /** Rozbija 8 bloków wokół wykopanego (w płaszczyźnie prostopadłej do ściany, w którą celuje gracz). */
    private void breakArea(ItemStack stack, Level level, BlockState center, BlockPos pos, ServerPlayer player) {
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        Direction face = hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : Direction.UP;
        Direction.Axis axis = face.getAxis();
        float centerHardness = center.getDestroySpeed(level, pos);
        BREAKING_AREA.set(true);
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos p = switch (axis) {
                        case X -> pos.offset(0, a, b);
                        case Y -> pos.offset(a, 0, b);
                        case Z -> pos.offset(a, b, 0);
                    };
                    if (energy(stack) < COST_PER_BLOCK) return;
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || !isCorrectToolForDrops(stack, s)) continue;
                    float h = s.getDestroySpeed(level, p);
                    if (h < 0 || h > centerHardness * 1.5f + 0.5f) continue;   // nie rozbijaj dużo twardszych bloków
                    player.gameMode.destroyBlock(p);
                }
            }
        } finally {
            BREAKING_AREA.set(false);
        }
    }

    public static IEnergyStorage storage(ItemStack stack) {
        return new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(), CAPACITY, RATE, RATE);
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    private static void use(ItemStack stack, int amount) {
        stack.set(ModDataComponents.ENERGY.get(), Math.max(0, energy(stack) - amount));
    }

    public static ItemStack charged(Item item) {
        ItemStack s = new ItemStack(item);
        s.set(ModDataComponents.ENERGY.get(), CAPACITY);
        return s;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (energy(stack) < COST_PER_BLOCK) return 1.0f;
        float speed = super.getDestroySpeed(stack, state);
        return isAreaMode(stack) ? speed * 0.4f : speed;   // 3x3 kopie wolniej
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0f) use(stack, COST_PER_BLOCK);
        if (!level.isClientSide() && isAreaMode(stack) && !BREAKING_AREA.get() && miner instanceof ServerPlayer player
                && !player.isShiftKeyDown() && isCorrectToolForDrops(stack, state)) {
            breakArea(stack, level, state, pos, player);   // Shift podczas kopania = tylko jeden blok
        }
        return true;   // bez zużycia wytrzymałości
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        use(stack, COST_PER_HIT);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * Mth.clamp(energy(stack) / (float) CAPACITY, 0f, 1f));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3C8CFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.redcircuit.battery", energy(stack), CAPACITY).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.redcircuit.electric_tool", COST_PER_BLOCK).withStyle(ChatFormatting.GRAY));
        if (areaCapable) {
            tooltip.accept(Component.translatable(isAreaMode(stack) ? "tooltip.redcircuit.drill_mode_3x3" : "tooltip.redcircuit.drill_mode_1x1")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.accept(Component.translatable("tooltip.redcircuit.drill_toggle").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
