package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.tube.ExtractorBlock;

/** Block Breaker: na impuls redstone rozbija blok przed sobą; dropy wychodzą tyłem (rura, inwentarz, ziemia). */
public class BlockBreakerBlock extends ExtractorBlock {
    public BlockBreakerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean connectsToTube(BlockState state, Direction fromTube) {
        return fromTube == state.getValue(FACING);   // rura z tyłu
    }

    @Override
    protected void transfer(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel sl)) return;
        Direction facing = state.getValue(FACING);
        for (ItemStack drop : MachineUtil.breakBlock(sl, pos.relative(facing))) {
            ExtractorBlock.deliver(level, pos, facing.getOpposite(), drop, -1);
        }
    }
}
