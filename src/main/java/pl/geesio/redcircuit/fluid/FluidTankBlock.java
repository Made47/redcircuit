package pl.geesio.redcircuit.fluid;

import net.minecraft.core.Direction;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModDataComponents;
import pl.geesio.redcircuit.util.TickerUtil;

import java.util.List;

/**
 * Zbiornik na ciecz. PPM wiadrem (lub innym pojemnikiem) nalewa/zabiera ciecz, pustą ręką pokazuje zawartość.
 * Rury i pompy łączą się z nim z każdej strony. Komparator odczytuje poziom, świecące ciecze (lawa) dają światło.
 */
public class FluidTankBlock extends Block implements EntityBlock {

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : TickerUtil.create(type, ModBlockEntities.FLUID_TANK.get(), FluidTankBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || ItemAccess.forPlayerInteraction(player, hand).getCapability(Capabilities.Fluid.ITEM) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection(), null)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity be) {
            player.sendOverlayMessage(describe(be.getTank().getFluid()));
        }
        return InteractionResult.SUCCESS;
    }

    private static Component describe(FluidStack fs) {
        Component name = fs.isEmpty() ? Component.translatable("message.redcircuit.pump_empty") : fs.getHoverName();
        return Component.translatable("message.redcircuit.pipe", name, fs.getAmount(), FluidTankBlockEntity.CAPACITY);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FluidTankBlockEntity be ? be.lightLevel() : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity be) || be.getTank().isEmpty()) return 0;
        return 1 + Math.round(be.fill() * 14);
    }

    /** Opis przedmiotu zbiornika (wywoływany przez BlockItem w ModItems - bloki nie mają już własnych podpowiedzi). */
    public static void appendTooltip(ItemStack stack, Consumer<Component> tooltip) {
        SimpleFluidContent content = stack.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY);
        tooltip.accept(describe(content.copy()).copy().withStyle(ChatFormatting.AQUA));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }
}
