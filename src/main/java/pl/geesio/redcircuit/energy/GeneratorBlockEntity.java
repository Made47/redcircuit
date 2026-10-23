package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/**
 * Generatory Azurity:
 * <ul>
 *   <li><b>Termostos (Thermopile)</b> - różnica temperatur: każda para "gorący" (lawa, ogień, magma)
 *       + "zimny" (woda, lód, śnieg) wśród 6 sąsiadów daje 8 FE/t.</li>
 *   <li><b>Generator</b> - wiatr: pracuje pod gołym niebem, mocniej wysoko,
 *       w deszczu i podczas burzy; zasłonięte boki zmniejszają produkcję.</li>
 * </ul>
 */
public class GeneratorBlockEntity extends BlockEntity {
    public enum Kind { THERMOPILE, KINETIC }

    private final Kind kind;
    private final ModEnergyStorage energy = new ModEnergyStorage(10_000, 0, 400, this::setChanged);
    private int lastRate;

    public static GeneratorBlockEntity thermopile(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(ModBlockEntities.THERMOPILE.get(), pos, state, Kind.THERMOPILE);
    }

    public static GeneratorBlockEntity kinetic(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(ModBlockEntities.KINETIC_GENERATOR.get(), pos, state, Kind.KINETIC);
    }

    private GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Kind kind) {
        super(type, pos, state);
        this.kind = kind;
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    public int getLastRate() {
        return lastRate;
    }

    // ------------------------------------------------------------------ produkcja

    private static boolean isHot(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getFluidState().is(FluidTags.LAVA)) return true;
        if (s.is(BlockTags.FIRE) || s.is(Blocks.MAGMA_BLOCK)) return true;
        return s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT);
    }

    private static boolean isCold(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getFluidState().is(FluidTags.WATER)) return true;
        return s.is(BlockTags.ICE) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW);
    }

    private int computeRate(Level level, BlockPos pos) {
        if (kind == Kind.THERMOPILE) {
            int hot = 0, cold = 0;
            for (Direction d : Direction.values()) {
                BlockPos np = pos.relative(d);
                if (!level.isLoaded(np)) continue;
                if (isHot(level, np)) hot++;
                else if (isCold(level, np)) cold++;
            }
            return Math.min(hot, cold) * pl.geesio.redcircuit.config.RedCircuitConfig.THERMOPILE_FE_PER_PAIR.get();
        }
        // KINETIC - zależy od zamontowanego wirnika
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof KineticGeneratorBlock)) return 0;
        KineticGeneratorBlock.Rotor rotor = state.getValue(KineticGeneratorBlock.ROTOR);
        Direction facing = state.getValue(KineticGeneratorBlock.FACING);
        BlockPos center = pos.relative(facing);
        Direction side = facing.getClockWise();
        if (rotor == KineticGeneratorBlock.Rotor.WINDMILL) {
            // Wiatrak 5x5 przed generatorem: cała przestrzeń musi być pusta, pod gołym niebem.
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    BlockPos p = center.relative(side, a).above(b);
                    if (!level.isLoaded(p) || !level.getBlockState(p).isAir()) return 0;
                }
            }
            if (!level.canSeeSky(center.above(2))) return 0;
            int height = Math.max(0, pos.getY() - level.getSeaLevel());
            double rate = Math.min(pl.geesio.redcircuit.config.RedCircuitConfig.WINDMILL_MAX_FE.get(), pl.geesio.redcircuit.config.RedCircuitConfig.WINDMILL_BASE_FE.get() + height / 6.0);   // domyślnie 6 FE/t nad morzem, do 32 FE/t wysoko
            if (level.isThundering()) rate *= 2;
            else if (level.isRaining()) rate *= 1.5;
            return (int) Math.round(rate);
        }
        if (rotor == KineticGeneratorBlock.Rotor.WATER_WHEEL) {
            // Koło wodne 3x3: płynąca woda daje 4 FE/t za blok, stojąca (źródło) 1 FE/t.
            int rate = 0;
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos p = center.relative(side, a).above(b);
                    if (!level.isLoaded(p)) continue;
                    var fluid = level.getFluidState(p);
                    if (fluid.is(FluidTags.WATER)) rate += fluid.isSource() ? pl.geesio.redcircuit.config.RedCircuitConfig.WATER_WHEEL_SOURCE_FE.get() : pl.geesio.redcircuit.config.RedCircuitConfig.WATER_WHEEL_FLOWING_FE.get();
                    else if (!level.getBlockState(p).isAir()) return 0;   // koło zablokowane
                }
            }
            return Math.min(rate, pl.geesio.redcircuit.config.RedCircuitConfig.WATER_WHEEL_MAX_FE.get());
        }
        return 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        if (level.isClientSide) return;
        // Termostos przelicza otoczenie co 20 ticków, generator co 40.
        if (level.getGameTime() % (be.kind == Kind.THERMOPILE ? 20 : 40) == 0) {
            be.lastRate = be.computeRate(level, pos);
        }
        if (be.lastRate > 0) be.energy.addInternal(be.lastRate);
        if (be.kind == Kind.KINETIC && state.getBlock() instanceof KineticGeneratorBlock) {
            boolean running = be.lastRate > 0;
            if (state.getValue(KineticGeneratorBlock.RUNNING) != running) {
                level.setBlock(pos, state.setValue(KineticGeneratorBlock.RUNNING, running), 2);
            }
        }

        for (Direction d : Direction.values()) {
            if (be.energy.getEnergyStored() <= 0) break;
            BlockPos np = pos.relative(d);
            if (!level.isLoaded(np)) continue;
            IEnergyStorage dst = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.EnergyStorage.BLOCK, np, d.getOpposite());
            if (dst != null && dst.canReceive()) {
                int sent = dst.receiveEnergy(Math.min(be.energy.getEnergyStored(), 400), false);
                if (sent > 0) be.energy.setEnergy(be.energy.getEnergyStored() - sent);
            }
        }
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
