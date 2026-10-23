package pl.geesio.redcircuit.fluid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import pl.geesio.redcircuit.platform.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import pl.geesio.redcircuit.platform.fluids.SimpleFluidContent;
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
    /** Światło świecącej cieczy w środku (ustawiane przez blok-encję - Fabric nie ma światła zależnego od pozycji). */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    public FluidTankBlock(Properties properties) {
        super(properties.lightLevel(s -> s.getValue(LIGHT)));
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : TickerUtil.create(type, ModBlockEntities.FLUID_TANK.get(), FluidTankBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        Storage<FluidVariant> held = FluidStorage.ITEM.find(stack, ContainerItemContext.forPlayerInteraction(player, hand));
        if (held == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        Storage<FluidVariant> tank = FluidStorage.SIDED.find(level, pos, hit.getDirection());
        if (tank != null && FluidStorageUtil.interactWithFluidStorage(tank, player, hand)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidTankBlockEntity be) {
            player.displayClientMessage(describe(be.getTank().getFluid()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component describe(FluidStack fs) {
        Component name = fs.isEmpty() ? Component.translatable("message.redcircuit.pump_empty") : fs.getHoverName();
        return Component.translatable("message.redcircuit.pipe", name, fs.getAmount(), FluidTankBlockEntity.CAPACITY);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity be) || be.getTank().isEmpty()) return 0;
        return 1 + Math.round(be.fill() * 14);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleFluidContent content = stack.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY);
        tooltip.add(describe(content.copy()).copy().withStyle(ChatFormatting.AQUA));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }
}
