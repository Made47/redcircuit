package pl.geesio.redcircuit.machine;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/** Buffer: magazyn 18 slotów dostępny z każdej strony (rury, lejki); emituje sygnał komparatora. */
public class BufferBlock extends Block implements EntityBlock {
    private final Supplier<BlockEntityType<MachineBlockEntity>> type;

    public BufferBlock(Properties properties, Supplier<BlockEntityType<MachineBlockEntity>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(type.get(), pos, state, 18);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x2, id, inv, be.items, 2),
                    Component.translatable("container.redcircuit.buffer")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MachineBlockEntity be
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(be.items) : 0;
    }

}
