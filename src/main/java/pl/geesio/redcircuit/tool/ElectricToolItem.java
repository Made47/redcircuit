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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
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
public class ElectricToolItem extends DiggerItem {
    public static final int CAPACITY = 100_000, RATE = 1_000, COST_PER_BLOCK = 50, COST_PER_HIT = 100;

    /** Poziom jak diament: kopie obsydian, prędkość 10. Wytrzymałość nieużywana (energia zamiast niej). */
    public static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 1_000_000;
        }

        @Override
        public float getSpeed() {
            return 10.0f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 3.0f;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    private final boolean areaCapable;
    /** Zapobiega rekurencji: rozbijanie bloków obok wywołuje mineBlock ponownie. */
    private static final ThreadLocal<Boolean> BREAKING_AREA = ThreadLocal.withInitial(() -> false);

    public ElectricToolItem(TagKey<Block> blocks, float attackDamage, float attackSpeed, Properties properties) {
        this(blocks, attackDamage, attackSpeed, properties, false);
    }

    /** @param areaCapable czy narzędzie ma tryb 3x3 (wiertarka) */
    public ElectricToolItem(TagKey<Block> blocks, float attackDamage, float attackSpeed, Properties properties, boolean areaCapable) {
        super(TIER, blocks, properties.stacksTo(1).attributes(DiggerItem.createAttributes(TIER, attackDamage, attackSpeed)));
        this.areaCapable = areaCapable;
    }

    public boolean isAreaMode(ItemStack stack) {
        return areaCapable && stack.getOrDefault(ModDataComponents.DRILL_AREA.get(), false);
    }

    // ------------------------------------------------------------------ przełączanie trybu

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!areaCapable || !player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        toggle(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (areaCapable && player != null && player.isShiftKeyDown()) {
            toggle(ctx.getLevel(), player, ctx.getItemInHand());
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        return super.useOn(ctx);
    }

    private void toggle(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;
        boolean area = !isAreaMode(stack);
        stack.set(ModDataComponents.DRILL_AREA.get(), area);
        player.displayClientMessage(Component.translatable(area ? "message.redcircuit.drill_3x3" : "message.redcircuit.drill_1x1")
                .withStyle(ChatFormatting.AQUA), true);
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
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0f) use(stack, COST_PER_BLOCK);
        if (!level.isClientSide && isAreaMode(stack) && !BREAKING_AREA.get() && miner instanceof ServerPlayer player
                && !player.isShiftKeyDown() && isCorrectToolForDrops(stack, state)) {
            breakArea(stack, level, state, pos, player);   // Shift podczas kopania = tylko jeden blok
        }
        return true;   // bez zużycia wytrzymałości
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
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
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.redcircuit.battery", energy(stack), CAPACITY).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.redcircuit.electric_tool", COST_PER_BLOCK).withStyle(ChatFormatting.GRAY));
        if (areaCapable) {
            tooltip.add(Component.translatable(isAreaMode(stack) ? "tooltip.redcircuit.drill_mode_3x3" : "tooltip.redcircuit.drill_mode_1x1")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("tooltip.redcircuit.drill_toggle").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
