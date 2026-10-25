package pl.geesio.redcircuit.wire;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Kabel wiązkowy w obudowie: 16 kanałów, biegnie środkiem bloku. */
public class EncasedBundledCableBlock extends EncasedWireBlock implements EntityBlock {

    public EncasedBundledCableBlock(Properties properties) {
        super(properties, 8.0);
        registerDefaultState(disconnected(stateDefinition.any()));
    }

    @Override
    public int wireType() {
        return WireTypes.BUNDLED;
    }

    @Override
    protected boolean connectsToDevice(net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.core.Direction dir,
                                       BlockPos neighborPos, BlockState neighbor) {
        return BundledIO.isDevice(level, neighborPos, neighbor);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BundledCableBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        // 26.x nie podaje pozycji sąsiada - odświeżamy sieć przy każdej zmianie obok.
        if (!level.isClientSide()) WireNetwork.update(level, pos);
    }

    @Override
    protected void onNetworkChanged(Level level, BlockPos pos) {
        WireNetwork.onWireChanged(level, pos, this);
    }

    @Override
    protected void onNetworkRemoved(Level level, BlockPos pos) {
        WireNetwork.onWireRemoved(level, pos, this);
    }
}
