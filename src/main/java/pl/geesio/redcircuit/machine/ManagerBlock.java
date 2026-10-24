package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.tube.TargetOnly;
import pl.geesio.redcircuit.tube.ExtractorBlock;
import pl.geesio.redcircuit.util.TickerUtil;

/** Manager - stawia się go, klikając na pilnowany inwentarz; przód łączy się z siecią rur. */
public class ManagerBlock extends ExtractorBlock implements EntityBlock, TargetOnly {
    public ManagerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManagerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : TickerUtil.create(type, ModBlockEntities.MANAGER.get(), ManagerBlockEntity::tick);
    }

    @Override
    protected void transfer(Level level, BlockPos pos, BlockState state) {
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ManagerBlockEntity be) {
            if (player.isShiftKeyDown()) {
                player.sendOverlayMessage(Component.translatable("message.redcircuit.energy",
                        be.getEnergy().getEnergyStored(), be.getEnergy().getMaxEnergyStored()));
            } else {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inv, be.filter, 1),
                        Component.translatable("container.redcircuit.manager")));
            }
        }
        return InteractionResult.SUCCESS;
    }

}
