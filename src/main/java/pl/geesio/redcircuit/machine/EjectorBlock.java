package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Ejector: 9 slotów; na impuls redstone wypycha przodem jeden stos (sloty po kolei). */
public class EjectorBlock extends ContainerMachineBlock {
    public EjectorBlock(Properties properties, Supplier<BlockEntityType<MachineBlockEntity>> type) {
        super(properties, type, 1, "container.redcircuit.ejector");
    }

    @Override
    protected void pulse(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        int slot = MachineUtil.pushOne(level, pos, state.getValue(FACING), be.items, be.next);
        if (slot >= 0) {
            be.next = (slot + 1) % be.items.getContainerSize();
            be.setChanged();
        }
    }
}
