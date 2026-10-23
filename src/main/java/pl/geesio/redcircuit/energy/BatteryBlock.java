package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.TickerUtil;

/**
 * Akumulator Azurity (1 000 000 FE). PPM otwiera okno ze slotami na baterie.
 * PPM baterią (lub innym przedmiotem z energią FE) ładuje ją z akumulatora,
 * Shift + PPM rozładowuje ją do akumulatora.
 */
public class BatteryBlock extends Block implements EntityBlock {

    public BatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : TickerUtil.create(type, ModBlockEntities.BATTERY.get(), BatteryBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BatteryBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BatteryBlockEntity be) {
            for (int i = 0; i < be.getItems().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getItems().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        IEnergyStorage item = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (item == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BatteryBlockEntity be)) return ItemInteractionResult.FAIL;

        ModEnergyStorage box = be.getEnergy();
        int moved = 0;
        // Pętla, bo przedmioty mają ograniczenie FE na jedno wywołanie
        for (int i = 0; i < 1000; i++) {
            int step;
            if (player.isShiftKeyDown()) {
                int space = box.getMaxEnergyStored() - box.getEnergyStored();
                step = item.extractEnergy(space, false);
                if (step > 0) box.setEnergy(box.getEnergyStored() + step);
            } else {
                step = item.receiveEnergy(box.getEnergyStored(), false);
                if (step > 0) box.setEnergy(box.getEnergyStored() - step);
            }
            if (step <= 0) break;
            moved += step;
        }
        if (moved > 0) {
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.4f, player.isShiftKeyDown() ? 0.8f : 1.6f);
        }
        player.displayClientMessage(Component.translatable("message.redcircuit.battery_item",
                stack.getHoverName(), item.getEnergyStored(), item.getMaxEnergyStored(),
                box.getEnergyStored(), box.getMaxEnergyStored()), true);
        return ItemInteractionResult.CONSUME;
    }
}
