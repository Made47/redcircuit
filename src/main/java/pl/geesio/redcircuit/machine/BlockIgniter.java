package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/** Zapalarka: gdy zasilona, zapala ogień przed sobą (i podtrzymuje go); po wyłączeniu gasi. */
public class BlockIgniter extends BlockFacing {
    public BlockIgniter() {
        super("igniter");
    }

    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
        if (powered == ((meta & POWERED) != 0)) return;
        world.setBlockMetadataWithNotify(x, y, z, powered ? meta | POWERED : meta & ~POWERED, 4);
        if (powered) {
            world.scheduleBlockUpdate(x, y, z, this, 1);
        } else {
            ForgeDirection f = facing(meta);
            int fx = x + f.offsetX, fy = y + f.offsetY, fz = z + f.offsetZ;
            if (world.getBlock(fx, fy, fz) == Blocks.fire) world.setBlockToAir(fx, fy, fz);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & POWERED) == 0) return;
        ForgeDirection f = facing(meta);
        int fx = x + f.offsetX, fy = y + f.offsetY, fz = z + f.offsetZ;
        if (world.isAirBlock(fx, fy, fz) && Blocks.fire.canPlaceBlockAt(world, fx, fy, fz)) {
            world.setBlock(fx, fy, fz, Blocks.fire);
            world.playSoundEffect(fx + 0.5, fy + 0.5, fz + 0.5, "fire.ignite", 1.0F, 1.0F);
        }
        world.scheduleBlockUpdate(x, y, z, this, 20);   // podtrzymuj ogień, dopóki jest sygnał
    }

    /** Zapalarka podtrzymuje ogień na swoim przodzie jak netherrack. */
    @Override
    public boolean isFireSource(World world, int x, int y, int z, ForgeDirection side) {
        return side == facing(world, x, y, z) && (world.getBlockMetadata(x, y, z) & POWERED) != 0;
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
    }
}
