package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Kabel wiązkowy: 16 niezależnych kanałów (po jednym na kolor), przyklejany do ścian.
 * Przewód izolowany koloru X podłączony do kabla zasila/odczytuje kanał X.
 */
public class BundledCableBlock extends FaceWireBlock implements EntityBlock {

    public BundledCableBlock(Properties properties) {
        super(properties, 6f, 4f);
        registerDefaultState(noFaces(stateDefinition.any()).setValue(BlockStateProperties.DOWN, true));
    }

    @Override
    public boolean compatible(FaceWireBlock other) {
        if (other instanceof BundledCableBlock) return true;
        return other instanceof RedWireBlock wire && wire.isInsulated();
    }

    @Override
    protected boolean connectsToDevice(net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction side,
                                       net.minecraft.core.Direction dir, BlockPos neighborPos, BlockState neighbor) {
        return BundledIO.isDevice(level, neighborPos, neighbor);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BundledCableBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && (neighborBlock instanceof FaceWireBlock
                || BundledIO.isDevice(level, neighborPos, level.getBlockState(neighborPos)))) {
            WireNetwork.update(level, pos);
        }
    }

    @Override
    protected void onWireChanged(Level level, BlockPos pos) {
        WireNetwork.onWireChanged(level, pos, this);
    }

    @Override
    protected void onWireRemoved(Level level, BlockPos pos) {
        WireNetwork.onWireRemoved(level, pos, this);
    }
}
