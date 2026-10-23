package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Relay: 9 slotów, przyjmuje przedmioty z każdej strony i sam wypycha je przodem (co 4 ticki). */
public class RelayBlock extends ContainerMachineBlock {
    public RelayBlock(Properties properties, Supplier<BlockEntityType<MachineBlockEntity>> type) {
        super(properties, type, 1, "container.redcircuit.relay");
    }

    @Override
    protected boolean ticks() {
        return true;
    }

    @Override
    protected void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        if (++be.timer < 4) return;
        be.timer = 0;
        MachineUtil.pushOne(level, pos, state.getValue(FACING), be.items, 0);
    }

    @Override
    protected void pulse(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
    }
}
