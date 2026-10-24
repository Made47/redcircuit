package pl.geesio.redcircuit.energy;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import pl.geesio.redcircuit.util.transfer.IEnergyStorage;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/** Panel słoneczny: produkuje energię w dzień pod gołym niebem. */
public class SolarPanelBlockEntity extends BlockEntity {
    public static final int GENERATION = 10;

    private final ModEnergyStorage energy = new ModEnergyStorage(1_000, 0, 200, this::setChanged);
    private int lastGeneration;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    public int getLastGeneration() {
        return lastGeneration;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        if (level.isClientSide()) return;
        int gen = 0;
        if (level.isBrightOutside() && level.canSeeSky(pos.above())) {
            gen = level.isRaining() ? pl.geesio.redcircuit.config.RedCircuitConfig.SOLAR_FE.get() / 3 : pl.geesio.redcircuit.config.RedCircuitConfig.SOLAR_FE.get();
        }
        be.lastGeneration = gen;
        if (gen > 0) be.energy.addInternal(gen);

        for (Direction d : Direction.values()) {
            if (be.energy.getEnergyStored() <= 0) break;
            BlockPos np = pos.relative(d);
            if (!level.isLoaded(np)) continue;
            IEnergyStorage dst = IEnergyStorage.wrap(level.getCapability(Capabilities.Energy.BLOCK, np, d.getOpposite()));
            if (dst != null && dst.canReceive()) {
                int sent = dst.receiveEnergy(Math.min(be.energy.getEnergyStored(), 200), false);
                if (sent > 0) be.energy.setEnergy(be.energy.getEnergyStored() - sent);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setEnergy(input.getIntOr("Energy", 0));
    }
}
