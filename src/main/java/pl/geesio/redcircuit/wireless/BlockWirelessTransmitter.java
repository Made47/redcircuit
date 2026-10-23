package pl.geesio.redcircuit.wireless;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** Nadajnik: gdy dostaje sygnał redstone, nadaje na swojej częstotliwości. */
public class BlockWirelessTransmitter extends BlockWireless {
    public BlockWirelessTransmitter() {
        super("wireless_transmitter");
    }

    private void update(World world, int x, int y, int z) {
        if (world.isRemote) return;
        boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
        int meta = powered ? 1 : 0;
        if (world.getBlockMetadata(x, y, z) != meta) world.setBlockMetadataWithNotify(x, y, z, meta, 2);
        TileWireless t = tile(world, x, y, z);
        if (t != null) {
            WirelessNetwork net = WirelessNetwork.get();
            net.removeTransmitter(world, x, y, z);
            if (powered) net.setTransmitter(world, x, y, z, t.getFrequency(), true);
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote || dropIfUnsupported(world, x, y, z)) return;
        update(world, x, y, z);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        update(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote) WirelessNetwork.get().removeTransmitter(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void applyFrequency(World world, int x, int y, int z, int freq) {
        super.applyFrequency(world, x, y, z, freq);
        update(world, x, y, z);
    }
}
