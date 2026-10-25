package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
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
        return level.isClientSide() ? null : TickerUtil.create(type, ModBlockEntities.BATTERY.get(), BatteryBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        IEnergyStorage item = pl.geesio.redcircuit.platform.capabilities.CapLookup.item(stack, Capabilities.EnergyStorage.ITEM);
        if (item == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BatteryBlockEntity be)) return InteractionResult.FAIL;

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
        player.sendOverlayMessage(Component.translatable("message.redcircuit.battery_item",
                stack.getHoverName(), item.getEnergyStored(), item.getMaxEnergyStored(),
                box.getEnergyStored(), box.getMaxEnergyStored()));
        return InteractionResult.CONSUME;
    }
}
