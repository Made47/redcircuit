package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.TickerUtil;

/**
 * Akcelerator: wstawia się go w linię rur. Zasilony energią (FE) przyspiesza każdy przejeżdżający
 * przedmiot do 4x - przedmiot stopniowo zwalnia na kolejnych rurach.
 */
public class AcceleratorBlock extends TubeBlock {
    public AcceleratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AcceleratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return TickerUtil.create(type, ModBlockEntities.ACCELERATOR.get(), TubeBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AcceleratorBlockEntity be) {
                player.sendOverlayMessage(Component.translatable("message.redcircuit.energy",
                        be.getEnergy().getEnergyStored(), be.getEnergy().getMaxEnergyStored()));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
