package pl.geesio.redcircuit.tube;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Bloki, do których rura podłącza się wizualnie, mimo że nie są inwentarzem. */
public interface TubeConnectable {
    /**
     * @param state  stan tego bloku
     * @param fromTube kierunek od rury do tego bloku
     */
    boolean connectsToTube(BlockState state, Direction fromTube);
}
