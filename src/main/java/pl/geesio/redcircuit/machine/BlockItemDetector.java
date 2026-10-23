package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.IItemAcceptor;

import java.util.Random;

/**
 * Detektor przedmiotów: wstawia się go w linię rur (tył = wejście, przód = wyjście). Każdy przejeżdżający
 * stos wysyła krótki impuls redstone (na wszystkie strony). Bit 8 metadanych = aktywny.
 */
public class BlockItemDetector extends BlockFacing {
    public BlockItemDetector() {
        super("item_detector");
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new Tile();
    }

    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        ForgeDirection f = facing(world, x, y, z);
        return fromTube == f || fromTube == f.getOpposite();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        // Nie reaguje na sygnał - sam go wysyła
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & POWERED) != 0) world.setBlockMetadataWithNotify(x, y, z, meta & ~POWERED, 3);
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return (world.getBlockMetadata(x, y, z) & POWERED) != 0 ? 15 : 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    /** Wejście od tyłu: przedmiot od razu wychodzi przodem, a detektor wysyła impuls. */
    public static class Tile extends TileEntity implements IItemAcceptor {
        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public ItemStack accept(ItemStack stack, ForgeDirection side, boolean simulate) {
            int meta = getBlockMetadata();
            ForgeDirection facing = facing(meta);
            if (side != facing.getOpposite() || stack == null) return stack;
            int n = deliverableCount(worldObj, xCoord, yCoord, zCoord, facing, stack, -1);
            if (n <= 0) return stack;
            ItemStack rest = stack.copy();
            ItemStack out = rest.splitStack(Math.min(n, rest.stackSize));
            if (!simulate) {
                deliver(worldObj, xCoord, yCoord, zCoord, facing, out, -1);
                Block b = getBlockType();
                if ((meta & POWERED) == 0) worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, meta | POWERED, 3);
                worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, b, 4);
            }
            return rest.stackSize > 0 ? rest : null;
        }
    }
}
