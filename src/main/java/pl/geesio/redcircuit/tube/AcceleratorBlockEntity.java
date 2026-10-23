package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.config.RedCircuitConfig;
import pl.geesio.redcircuit.energy.ModEnergyStorage;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/** Akcelerator: odcinek rury, który za energię przyspiesza przejeżdżające przedmioty. */
public class AcceleratorBlockEntity extends TubeBlockEntity {
    public static final float BOOST_SPEED = 0.5f;

    private final ModEnergyStorage energy = new ModEnergyStorage(5_000, 200, 0, this::setChanged);

    public AcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACCELERATOR.get(), pos, state);
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    @Override
    protected void onItemEnter(TubeItem item) {
        if (level == null || level.isClientSide) return;
        int cost = RedCircuitConfig.ACCELERATOR_FE.get();
        if (energy.getEnergyStored() < cost) return;
        energy.setEnergy(energy.getEnergyStored() - cost);
        item.speed = Math.max(item.speed, BOOST_SPEED);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
    }
}
