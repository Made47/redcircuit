package pl.geesio.redcircuit.craft;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import pl.geesio.redcircuit.platform.items.ItemHandlerHelper;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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

    /** Wywoływane przed interakcją z blokiem (Fabric: UseBlockCallback w RedCircuit). */
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = ctx.getPlayer();
        if (player != null && !player.mayBuild()) return InteractionResult.PASS;

        boolean sneaking = player != null && player.isShiftKeyDown();
        if (sneaking && state.getBlock() instanceof GateBlock) {
            if (!level.isClientSide) pickUpGate(level, pos, state, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockState rotated = rotate(level, pos, state, sneaking);
        if (rotated == null) return InteractionResult.PASS;

        if (!level.isClientSide) {
            level.setBlock(pos, rotated, Block.UPDATE_ALL);
            // Blok sam przelicza swoje wejścia/wyjścia po zmianie kierunku
            level.neighborChanged(pos, rotated.getBlock(), pos);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Zbiera bramkę do ekwipunku gracza; niestandardowe ustawienia zapisuje w przedmiocie. */
    private static void pickUpGate(Level level, BlockPos pos, BlockState state, Player player) {
        ItemStack drop = new ItemStack(state.getBlock().asItem());
        if (level.getBlockEntity(pos) instanceof GateBlockEntity be
                && withStateSettings(be, state).hasCustomSettings()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Interval", be.interval);
            tag.putInt("MaxValue", be.max);
            tag.putBoolean("Muted", be.muted);
            tag.putBoolean("Mirrored", be.mirrored);
            tag.putInt("Disabled", be.disabled);
            tag.putBoolean("Quiet", be.quiet);
            BlockItem.setBlockEntityData(drop, ModBlockEntities.GATE.get(), tag);
        }
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8f, 1.0f);
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

        DirectionProperty prop = findFacing(state);
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

    private static DirectionProperty findFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) return BlockStateProperties.FACING;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return BlockStateProperties.HORIZONTAL_FACING;
        if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) return BlockStateProperties.FACING_HOPPER;
        for (Property<?> p : state.getProperties()) {
            if (p instanceof DirectionProperty dp && p.getName().equals("facing")) return dp;
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.redcircuit.screwdriver").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.redcircuit.screwdriver_gate").withStyle(ChatFormatting.GRAY));
    }
}
