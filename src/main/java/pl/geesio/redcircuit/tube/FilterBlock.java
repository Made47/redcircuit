package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Filter: jak Extractor, ale pobiera tylko przedmioty pasujące do wzorców (PPM otwiera 9 slotów).
 */
public class FilterBlock extends ExtractorBlock implements EntityBlock {

    public FilterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilterBlockEntity(pos, state);
    }

    @Override
    protected boolean accepts(Level level, BlockPos pos, ItemStack stack) {
        return level.getBlockEntity(pos) instanceof FilterBlockEntity be && be.matches(stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FilterBlockEntity be) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inv, be.filter, 1),
                    Component.translatable("container.redcircuit.filter")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FilterBlockEntity be) {
            Containers.dropContents(level, pos, be.filter);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
