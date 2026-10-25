package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Maszyna sortująca: na impuls redstone pobiera pasujący stos z inwentarza za sobą,
 * nadaje mu kolor kolumny i wysyła do rury. Kolorowe przedmioty jadą tylko
 * rurami w tym samym kolorze (lub niepomalowanymi).
 */
public class SortingMachineBlock extends ExtractorBlock implements EntityBlock {

    public SortingMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SortingMachineBlockEntity(pos, state);
    }

    @Override
    protected boolean accepts(Level level, BlockPos pos, ItemStack stack) {
        return level.getBlockEntity(pos) instanceof SortingMachineBlockEntity be && be.columnFor(stack) >= 0;
    }

    @Override
    protected int colorFor(Level level, BlockPos pos, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof SortingMachineBlockEntity be) {
            return be.colorOf(be.columnFor(stack));
        }
        return -1;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SortingMachineBlockEntity be) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> ChestMenu.threeRows(id, inv, be.config),
                    Component.translatable("container.redcircuit.sorting_machine")));
        }
        return InteractionResult.SUCCESS;
    }

}
