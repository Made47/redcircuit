package pl.geesio.redcircuit.craft;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import pl.geesio.redcircuit.util.transfer.ItemHandlerHelper;
import pl.geesio.redcircuit.gate.GateBlock;
import pl.geesio.redcircuit.gate.GateBlockEntity;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Śrubokręt: PPM obraca blok (bramki, maszyny, pompy, ekstraktory, a także bloki vanilla
 * mające kierunek). Shift + PPM obraca w przeciwną stronę, a na bramce logicznej - zbiera ją
 * do ekwipunku razem z ustawieniami (czas, maksimum licznika, wyciszenie).
 * Działa przed interakcją z blokiem, więc nie otwiera GUI i nie zmienia ustawień timera. Nie zużywa się.
 */
public class ScrewdriverItem extends Item {

    public ScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = ctx.getPlayer();
        if (player != null && !player.mayBuild()) return InteractionResult.PASS;

        boolean sneaking = player != null && player.isShiftKeyDown();
        if (sneaking && state.getBlock() instanceof GateBlock) {
            if (!level.isClientSide()) pickUpGate(level, pos, state, player);
            return InteractionResult.SUCCESS;
        }

        BlockState rotated = rotate(level, pos, state, sneaking);
        if (rotated == null) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            level.setBlock(pos, rotated, Block.UPDATE_ALL);
            // Blok sam przelicza swoje wejścia/wyjścia po zmianie kierunku
            level.neighborChanged(pos, rotated.getBlock(), null);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Zbiera bramkę do ekwipunku gracza; niestandardowe ustawienia zapisuje w przedmiocie. */
    private static void pickUpGate(Level level, BlockPos pos, BlockState state, Player player) {
        ItemStack drop = new ItemStack(state.getBlock().asItem());
        if (level.getBlockEntity(pos) instanceof GateBlockEntity be
                && withStateSettings(be, state).hasCustomSettings()) {
            TagValueOutput tag = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            tag.putInt("Interval", be.interval);
            tag.putInt("MaxValue", be.max);
            tag.putBoolean("Muted", be.muted);
            tag.putBoolean("Mirrored", be.mirrored);
            tag.putInt("Disabled", be.disabled);
            tag.putBoolean("Quiet", be.quiet);
            BlockItem.setBlockEntityData(drop, ModBlockEntities.GATE.get(), tag);
        }
        level.playSound(null, pos, state.getSoundType(level, pos, player).getBreakSound(), SoundSource.BLOCKS, 0.8f, 1.0f);
        level.removeBlock(pos, false);
        if (!player.getAbilities().instabuild || !player.getInventory().contains(drop)) {
            ItemHandlerHelper.giveItemToPlayer(player, drop);
        }
    }

    /** Wyłączone wejścia są w stanie bloku - przepisz je do danych przed zapisaniem w przedmiocie. */
    private static GateBlockEntity withStateSettings(GateBlockEntity be, BlockState state) {
        be.disabled = pl.geesio.redcircuit.gate.GateBlock.disabledMask(state);
        return be;
    }

    /** Zwraca obrócony stan albo null, gdy bloku nie da się (bezpiecznie) obrócić. */
    private static BlockState rotate(Level level, BlockPos pos, BlockState state, boolean reverse) {
        // Bloki wieloczęściowe i w trakcie pracy - nie ruszamy (łóżka, drzwi, podwójne skrzynie, wysunięte tłoki)
        if (state.hasProperty(BlockStateProperties.BED_PART)
                || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || (state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE)
                || (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED))) {
            return null;
        }

        EnumProperty<Direction> prop = findFacing(state);
        if (prop != null) {
            List<Direction> values = new ArrayList<>(prop.getPossibleValues());
            int idx = values.indexOf(state.getValue(prop));
            for (int i = 1; i < values.size(); i++) {
                int n = Math.floorMod(idx + (reverse ? -i : i), values.size());
                BlockState next = state.setValue(prop, values.get(n));
                if (next.canSurvive(level, pos)) return next;
            }
            return null;
        }

        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis[] axes = Direction.Axis.values();
            int idx = state.getValue(BlockStateProperties.AXIS).ordinal();
            return state.setValue(BlockStateProperties.AXIS, axes[Math.floorMod(idx + (reverse ? -1 : 1), 3)]);
        }
        return null;
    }

    private static EnumProperty<Direction> findFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) return BlockStateProperties.FACING;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return BlockStateProperties.HORIZONTAL_FACING;
        if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) return BlockStateProperties.FACING_HOPPER;
        for (Property<?> p : state.getProperties()) {
            if (p instanceof EnumProperty<?> ep && ep.getValueClass() == Direction.class && p.getName().equals("facing")) {
                @SuppressWarnings("unchecked") EnumProperty<Direction> dp = (EnumProperty<Direction>) ep;
                return dp;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.redcircuit.screwdriver").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.redcircuit.screwdriver_gate").withStyle(ChatFormatting.GRAY));
    }
}
