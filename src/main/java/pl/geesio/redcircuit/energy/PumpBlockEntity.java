package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.fluids.FluidStack;
import pl.geesio.redcircuit.platform.fluids.capability.IFluidHandler;
import pl.geesio.redcircuit.platform.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Pompa: zasysa płyn z tyłu (zbiornik innego moda albo źródła płynu w świecie,
 * do 256 bloków połączonego jeziora) i tłoczy go do przodu (zbiornik / rura innego moda,
 * a gdy z przodu jest pusto - wylewa blok płynu). Sygnał redstone ją zatrzymuje.
 */
public class PumpBlockEntity extends BlockEntity {
    public static final int CAPACITY = 8_000;
    public static final int ENERGY_PER_BUCKET = 200;
    private static final int INTERVAL = 10;
    private static final int SEARCH_LIMIT = 256;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final ModEnergyStorage energy = new ModEnergyStorage(10_000, 400, 0, this::setChanged);

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity be) {
        if (level.isClientSide || level.getGameTime() % INTERVAL != 0) return;
        if (level.hasNeighborSignal(pos)) return;              // redstone = stop
        Direction facing = state.getValue(PumpBlock.FACING);
        be.intake(level, pos.relative(facing.getOpposite()), facing);
        be.output(level, pos.relative(facing), facing);
    }

    // ------------------------------------------------------------------ ssanie

    private void intake(Level level, BlockPos back, Direction facing) {
        if (tank.getSpace() < 1000 || energy.getEnergyStored() < pl.geesio.redcircuit.config.RedCircuitConfig.PUMP_FE_PER_BUCKET.get() || !level.isLoaded(back)) return;

        IFluidHandler src = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.FluidHandler.BLOCK, back, facing);
        if (src != null) {
            FluidStack sim = src.drain(Math.min(1000, tank.getSpace()), IFluidHandler.FluidAction.SIMULATE);
            if (sim.isEmpty()) return;
            int accepted = tank.fill(sim, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) return;
            FluidStack real = src.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            tank.fill(real, IFluidHandler.FluidAction.EXECUTE);
            energy.setEnergy(energy.getEnergyStored() - pl.geesio.redcircuit.config.RedCircuitConfig.PUMP_FE_PER_BUCKET.get() / 2);
            return;
        }

        FluidState fs = level.getFluidState(back);
        if (fs.isEmpty()) return;
        Fluid fluid = fs.getType() instanceof net.minecraft.world.level.material.FlowingFluid flowing ? flowing.getSource() : fs.getType();
        BlockPos source = findSource(level, back, fluid);
        if (source == null) return;
        FluidStack stack = new FluidStack(fluid, 1000);
        if (tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) < 1000) return;
        BlockState srcState = level.getBlockState(source);
        if (!(srcState.getBlock() instanceof BucketPickup pickup)) return;
        if (pickup.pickupBlock(null, level, source, srcState).isEmpty()) return;
        tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        energy.setEnergy(energy.getEnergyStored() - pl.geesio.redcircuit.config.RedCircuitConfig.PUMP_FE_PER_BUCKET.get());
    }

    /** Szuka najdalszego (ostatnio odwiedzonego) źródła płynu połączonego z punktem startowym. */
    @Nullable
    private static BlockPos findSource(Level level, BlockPos start, Fluid fluid) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        BlockPos found = null;
        while (!queue.isEmpty() && seen.size() < SEARCH_LIMIT) {
            BlockPos p = queue.poll();
            FluidState fs = level.getFluidState(p);
            if (fs.isEmpty() || !fs.getType().isSame(fluid)) continue;
            if (fs.isSource() && level.getBlockState(p).getBlock() instanceof BucketPickup) found = p;
            for (Direction d : Direction.values()) {
                if (d == Direction.DOWN) continue;              // nie schodź w dół - wypompowuj od góry
                BlockPos np = p.relative(d);
                if (level.isLoaded(np) && seen.add(np)) queue.add(np);
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ tłoczenie

    private void output(Level level, BlockPos front, Direction facing) {
        if (tank.isEmpty() || !level.isLoaded(front)) return;
        IFluidHandler dst = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.FluidHandler.BLOCK, front, facing.getOpposite());
        if (dst != null) {
            FluidStack sim = tank.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            int accepted = dst.fill(sim, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
        // Pusto z przodu - wylej blok płynu.
        if (tank.getFluidAmount() >= 1000 && level.getBlockState(front).canBeReplaced()
                && level.getFluidState(front).isEmpty()) {
            Fluid fluid = tank.getFluid().getFluid();
            if (fluid == Fluids.EMPTY) return;
            BlockState fluidBlock = fluid.defaultFluidState().createLegacyBlock();
            if (fluidBlock.isAir()) return;
            level.setBlock(front, fluidBlock, 3);
            tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // ------------------------------------------------------------------ zapis

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        energy.setEnergy(tag.getInt("Energy"));
    }
}
