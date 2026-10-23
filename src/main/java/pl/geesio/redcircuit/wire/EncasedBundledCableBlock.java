package pl.geesio.redcircuit.wire;

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
    protected boolean connectsToDevice(net.minecraft.world.level.LevelAccessor level, BlockPos pos, net.minecraft.core.Direction dir,
                                       BlockPos neighborPos, BlockState neighbor) {
        return BundledIO.isDevice(level, neighborPos, neighbor);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BundledCableBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && (WireTypes.typeOf(neighborBlock) != null
                || BundledIO.isDevice(level, neighborPos, level.getBlockState(neighborPos)))) WireNetwork.update(level, pos);
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
