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
import pl.geesio.redcircuit.tube.ExtractorBlock;
import pl.geesio.redcircuit.util.TickerUtil;

import java.util.function.Supplier;

/**
 * Maszyna kierunkowa z inwentarzem (9 albo 18 slotów, okno jak skrzynia).
 * Przód = wyjście; impuls redstone wywołuje {@link #transfer}.
 */
public abstract class ContainerMachineBlock extends ExtractorBlock implements EntityBlock {
    private final Supplier<BlockEntityType<MachineBlockEntity>> type;
    private final int rows;
    private final String title;

    protected ContainerMachineBlock(Properties properties, Supplier<BlockEntityType<MachineBlockEntity>> type, int rows, String title) {
        super(properties);
        this.type = type;
        this.rows = rows;
        this.title = title;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(type.get(), pos, state, rows * 9);
    }

    /** Czy maszyna potrzebuje tickowania co tick (domyślnie nie). */
    protected boolean ticks() {
        return false;
    }

    protected void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide || !ticks()) return null;
        return TickerUtil.create(t, type.get(), (l, p, s, be) -> serverTick(l, p, s, be));
    }

    @Override
    protected void transfer(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) pulse(level, pos, state, be);
    }

    /** Reakcja na impuls redstone. */
    protected abstract void pulse(Level level, BlockPos pos, BlockState state, MachineBlockEntity be);

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            MenuType<?> menu = rows == 2 ? MenuType.GENERIC_9x2 : MenuType.GENERIC_9x1;
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ChestMenu(menu, id, inv, be.items, rows),
                    Component.translatable(title)));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            Containers.dropContents(level, pos, be.items);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
