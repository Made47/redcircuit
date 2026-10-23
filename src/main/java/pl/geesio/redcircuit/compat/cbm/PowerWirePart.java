package pl.geesio.redcircuit.compat.cbm;

import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.part.TickablePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import pl.geesio.redcircuit.energy.EnergyNodeBlockEntity;
import pl.geesio.redcircuit.energy.ModEnergyStorage;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.WireLinks;
import pl.geesio.redcircuit.wire.WireTypes;

/** Przewód ze stopu energetycznego jako część CB Multipart: przesyła energię (FE). */
public class PowerWirePart extends RCWirePart implements TickablePart {
    public static final int CAPACITY = 2_000, RATE = 500;

    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, RATE, RATE, () -> {
        if (hasTile()) tile().setChanged();
    });

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    @Override
    public MultipartType<?> getType() {
        return CbmCompat.POWER_WIRE.get();
    }

    @Override
    public FaceWireBlock hostBlock() {
        return ModBlocks.POWER_ALLOY_WIRE.get();
    }

    @Override
    public int wireType() {
        return WireTypes.POWER;
    }

    @Override
    protected boolean device(BlockGetter level, BlockPos np, BlockState ns, Direction dir) {
        return pl.geesio.redcircuit.energy.EnergyConnect.hasEnergy(level, np, ns, dir.getOpposite());
    }

    @Override
    public void tick() {
        Level level = level();
        if (level.isClientSide || energy.getEnergyStored() <= 0) return;
        BlockPos pos = pos();
        Direction side = side();
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == side.getAxis()) continue;
            WireLinks.Target t = target(level, dir);
            if (t.pos() == null) {
                if (t.link() == FaceWireBlock.Link.DEVICE) push(level, pos.relative(dir), dir.getOpposite());
                continue;
            }
            if (t.face() >= 0) {
                if (CbmWireHost.part(level, t.pos(), t.face()) instanceof PowerWirePart other) equalize(energy, other.energy);
            } else if (level.getBlockEntity(t.pos()) instanceof EnergyNodeBlockEntity node) {
                equalize(energy, node.getEnergy());
            }
        }
        // Blok, na którym leży przewód (np. maszyna)
        push(level, pos.relative(side), side.getOpposite());
    }

    private void push(Level level, BlockPos target, Direction from) {
        if (level.getBlockEntity(target) instanceof EnergyNodeBlockEntity node) {
            equalize(energy, node.getEnergy());
            return;
        }
        IEnergyStorage dst = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, from);
        if (dst == null || !dst.canReceive()) return;
        int sent = dst.receiveEnergy(Math.min(energy.getEnergyStored(), RATE), false);
        if (sent > 0) energy.setEnergy(energy.getEnergyStored() - sent);
    }

    /** Wyrównuje procentowe naładowanie dwóch magazynów (w obie strony). */
    static void equalize(ModEnergyStorage a, ModEnergyStorage b) {
        long e1 = a.getEnergyStored(), c1 = a.getMaxEnergyStored();
        long e2 = b.getEnergyStored(), c2 = b.getMaxEnergyStored();
        long target1 = (e1 + e2) * c1 / (c1 + c2);
        long diff = e1 - target1;
        if (diff > 0) {
            int moved = b.addInternal((int) Math.min(diff, RATE));
            a.setEnergy((int) (e1 - moved));
        } else if (diff < 0) {
            int moved = a.addInternal((int) Math.min(-diff, RATE));
            b.setEnergy((int) (e2 - moved));
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        super.save(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        super.load(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
    }
}
