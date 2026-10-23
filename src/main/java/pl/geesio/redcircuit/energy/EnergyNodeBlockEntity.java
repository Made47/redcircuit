package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import pl.geesio.redcircuit.wire.FaceWireBlock;

/**
 * Węzeł sieci Azurity (przewód lub akumulator).
 * Sąsiednie węzły wyrównują poziom naładowania (procentowo), a energia jest
 * wypychana do odbiorców (np. piec elektryczny lub maszyny innych modów).
 */
public abstract class EnergyNodeBlockEntity extends BlockEntity {
    protected final ModEnergyStorage energy;
    private final int rate;

    protected EnergyNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int rate) {
        super(type, pos, state);
        this.rate = rate;
        this.energy = new ModEnergyStorage(capacity, rate, rate, this::setChanged);
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyNodeBlockEntity be) {
        if (level.isClientSide) return;
        be.distribute(level, pos);
    }

    private void distribute(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (energy.getEnergyStored() <= 0) return;
            BlockPos np = pos.relative(d);
            if (!level.isLoaded(np)) continue;
            BlockEntity nbe = level.getBlockEntity(np);
            if (nbe instanceof EnergyNodeBlockEntity other) {
                equalize(other);
            } else if (nbe != null) {
                IEnergyStorage dst = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.EnergyStorage.BLOCK, np, d.getOpposite());
                if (dst != null && dst.canReceive()) {
                    int sent = dst.receiveEnergy(Math.min(energy.getEnergyStored(), rate), false);
                    if (sent > 0) energy.setEnergy(energy.getEnergyStored() - sent);
                }
            }
        }

        // Przewody połączone przez narożniki zewnętrzne (po przekątnej).
        BlockState state = getBlockState();
        if (state.getBlock() instanceof FaceWireBlock wire) {
            for (Direction side : Direction.values()) {
                if (!FaceWireBlock.hasFace(state, side)) continue;
                for (Direction dir : Direction.values()) {
                    if (dir.getAxis() == side.getAxis()) continue;
                    BlockPos cp = pos.relative(dir).relative(side);
                    if (!level.isLoaded(cp) || !level.isLoaded(pos.relative(dir))) continue;
                    if (wire.link(level, pos, state, side, dir) != FaceWireBlock.Link.CORNER) continue;
                    if (level.getBlockEntity(cp) instanceof EnergyNodeBlockEntity other) equalize(other);
                }
            }
        }
    }

    /** Przesyła energię do sąsiedniego węzła, jeśli ten jest procentowo mniej naładowany. */
    private void equalize(EnergyNodeBlockEntity other) {
        long e1 = energy.getEnergyStored(), c1 = energy.getMaxEnergyStored();
        long e2 = other.energy.getEnergyStored(), c2 = other.energy.getMaxEnergyStored();
        if (e1 * c2 <= e2 * c1) return;
        long target1 = (e1 + e2) * c1 / (c1 + c2);
        int move = (int) Math.min(e1 - target1, Math.min(rate, other.rate));
        if (move <= 0) return;
        int accepted = other.energy.addInternal(move);
        if (accepted > 0) energy.setEnergy((int) (e1 - accepted));
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
