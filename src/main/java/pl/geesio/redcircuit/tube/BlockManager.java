package pl.geesio.redcircuit.tube;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.energy.EnergyUtil;
import pl.geesio.redcircuit.machine.BlockFacing;
import pl.geesio.redcircuit.util.InvUtil;

/** Manager - stawia się go, klikając na pilnowany inwentarz; przód łączy się z siecią rur. PPM: lista, Shift + PPM: energia. */
public class BlockManager extends BlockFacing implements ITargetOnly {
    public BlockManager() {
        super("manager", Material.iron);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileManager();
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (world.isRemote) return true;
        if (!(world.getTileEntity(x, y, z) instanceof TileManager te)) return false;
        if (player.isSneaking()) EnergyUtil.showEnergy(player, te.energy.get(), te.energy.capacity);
        else player.openGui(RedCircuit.instance, GuiHandler.CHEST, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (world.getTileEntity(x, y, z) instanceof TileManager te) InvUtil.dropAll(world, x, y, z, te.filter);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
