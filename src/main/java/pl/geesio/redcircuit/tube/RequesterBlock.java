package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Requester: tył podłączony do sieci rur, przód - wyjście (inwentarz, rura albo ziemia).
 * Stawia się go, klikając na rurę. PPM otwiera filtr (9 slotów), Shift + PPM pokazuje energię.
 */
public class RequesterBlock extends ExtractorBlock implements EntityBlock, TargetOnly {

    public RequesterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RequesterBlockEntity(pos, state);
    }

    @Override
    public boolean connectsToTube(BlockState state, Direction fromTube) {
        // Tył (sieć, z której ściąga) i przód (wyjście).
        Direction facing = state.getValue(FACING);
        return fromTube == facing || fromTube == facing.getOpposite();
    }

    @Override
    protected void transfer(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof RequesterBlockEntity be) be.retrieve(level, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RequesterBlockEntity be) {
            if (player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("message.redcircuit.energy",
                        be.getEnergy().getEnergyStored(), be.getEnergy().getMaxEnergyStored()), true);
            } else {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inv, be.filter, 1),
                        Component.translatable("container.redcircuit.requester")));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RequesterBlockEntity be) {
            Containers.dropContents(level, pos, be.filter);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
