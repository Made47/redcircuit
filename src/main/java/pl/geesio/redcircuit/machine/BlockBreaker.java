package pl.geesio.redcircuit.machine;

import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** Kruszarka bloków: na impuls redstone rozbija blok przed sobą; dropy wychodzą tyłem (rura, inwentarz, ziemia). */
public class BlockBreaker extends BlockFacing {
    public BlockBreaker() {
        super("block_breaker");
    }

    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        return fromTube == facing(world, x, y, z);   // rura z tyłu
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
        ForgeDirection facing = facing(world, x, y, z);
        for (ItemStack drop : MachineUtil.breakBlock(world, x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)) {
            deliver(world, x, y, z, facing.getOpposite(), drop, -1);
        }
    }
}
