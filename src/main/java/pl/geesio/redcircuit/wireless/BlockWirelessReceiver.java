package pl.geesio.redcircuit.wireless;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Odbiornik: daje sygnał 15 na wszystkie strony (mocny - w blok pod spodem), gdy jego częstotliwość jest aktywna. */
public class BlockWirelessReceiver extends BlockWireless {
    public BlockWirelessReceiver() {
        super("wireless_receiver");
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileWirelessReceiver();
    }

    void setOutput(World world, int x, int y, int z, boolean on) {
        int meta = on ? 1 : 0;
        if (world.getBlockMetadata(x, y, z) == meta) return;
        world.setBlockMetadataWithNotify(x, y, z, meta, 3);
        world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote) dropIfUnsupported(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        if (meta != 0) world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlockMetadata(x, y, z) != 0 ? 15 : 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlockMetadata(x, y, z) != 0 && side == 1 ? 15 : 0;
    }
}
