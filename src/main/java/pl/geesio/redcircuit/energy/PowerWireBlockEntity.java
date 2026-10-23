package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.registry.ModBlockEntities;

public class PowerWireBlockEntity extends EnergyNodeBlockEntity {
    public PowerWireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_WIRE.get(), pos, state, 2_000, 500);
    }
}
