package pl.geesio.redcircuit.wire;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.Pos;

/**
 * Kabel wiązkowy: 16 niezależnych kanałów (po jednym na kolor), przyklejany do ścian.
 * Przewód izolowany koloru X podłączony do kabla zasila/odczytuje kanał X.
 */
public class BlockBundledCable extends BlockFaceWire {
    public BlockBundledCable() {
        super(Material.circuits, 6f, 4f);
        setStepSound(soundTypeCloth);
        setBlockName(RedCircuit.MODID + ".bundled_cable");
        setCreativeTab(RCTab.TAB);
    }

    @Override
    public int wireType(int meta) {
        return WireTypes.BUNDLED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(RedCircuit.MODID + ":bundled_cable");
    }

    @Override
    public boolean connectsToDevice(IBlockAccess world, Pos pos, ForgeDirection side, ForgeDirection dir, Pos np, Block nb) {
        return BundledIO.isDevice(world, np, nb);
    }

    @Override
    protected void onSignalNeighborChanged(World world, int x, int y, int z, Block neighbor) {
        if (neighbor instanceof WireTypes.IWire || BundledIO.active()) WireNetwork.update(world, x, y, z);
    }

    @Override
    public void onWireChanged(World world, int x, int y, int z) {
        WireNetwork.onWireChanged(world, x, y, z);
    }

    @Override
    protected void onWireRemoved(World world, int x, int y, int z) {
        WireNetwork.onWireRemoved(world, x, y, z, this);
    }
}
