package pl.geesio.redcircuit.energy;

import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;

/**
 * Czy przewód energetyczny ma się połączyć z sąsiednim blokiem (ma on zdolność FE).
 * Działa także przy renderowaniu po stronie klienta, gdzie zamiast {@link Level} dostajemy
 * wycinek świata (RenderChunkRegion) - wtedy pytamy przez poziom bloku-encji sąsiada.
 */
public final class EnergyConnect {
    private EnergyConnect() {}

    public static boolean hasEnergy(BlockGetter level, BlockPos np, BlockState ns, Direction side) {
        if (ns.isAir() || !ns.hasBlockEntity()) return false;
        Level lvl = level instanceof Level l ? l : null;
        BlockEntity be = level.getBlockEntity(np);
        if (lvl == null && be != null) lvl = be.getLevel();
        if (lvl == null) return false;
        return pl.geesio.redcircuit.platform.capabilities.CapLookup.block(lvl, Capabilities.EnergyStorage.BLOCK, np, ns, be, side) != null;
    }
}
