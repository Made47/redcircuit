package pl.geesio.redcircuit.wire;

import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.util.CableBlock;

/**
 * Przewód w obudowie (Encased): biegnie środkiem bloku jak rura, w twardej obudowie.
 * Łączy się z przewodami tego samego rodzaju - także z przewodami przyklejonymi do ścian.
 */
public abstract class EncasedWireBlock extends CableBlock {

    protected EncasedWireBlock(Properties properties, double thicknessPx) {
        super(properties, thicknessPx);
    }

    /** Rodzaj przewodu wg {@link WireTypes}. */
    public abstract int wireType();

    /** Połączenie z urządzeniem niebędącym przewodem. */
    protected boolean connectsToDevice(LevelReader level, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        return false;
    }

    @Override
    protected boolean canConnectTo(LevelReader level, BlockPos pos, BlockState self, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        Block b = neighbor.getBlock();
        Integer t = WireTypes.typeOf(b);
        if (t != null) {
            if (!WireTypes.compatible(wireType(), t)) return false;
            if (b instanceof FaceWireBlock) {
                // Przewód na ścianie łączy się z nami, jeśli leży na ścianie prostopadłej do kierunku połączenia
                for (Direction side : Direction.values()) {
                    if (side.getAxis() != dir.getAxis() && FaceWireBlock.hasFace(neighbor, side)) return true;
                }
                return false;
            }
            return true;
        }
        if (PartWires.isHost(level, neighborPos)) {
            // Przewody-części CB Multipart na ścianach prostopadłych do połączenia
            for (Direction side : Direction.values()) {
                if (side.getAxis() == dir.getAxis()) continue;
                Integer pt = PartWires.wireAt(level, neighborPos, side);
                if (pt != null && WireTypes.compatible(wireType(), pt)) return true;
            }
        }
        if (b instanceof CableBlock) return false;
        return connectsToDevice(level, pos, dir, neighborPos, neighbor);
    }

    protected void onNetworkChanged(Level level, BlockPos pos) {}

    protected void onNetworkRemoved(Level level, BlockPos pos) {}

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) onNetworkChanged(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        onNetworkRemoved(level, pos);
    }
}
