package pl.geesio.redcircuit.tube;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.energy.EnergyUtil;
import pl.geesio.redcircuit.machine.BlockFacing;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Requester: tył podłączony do sieci rur, przód - wyjście (inwentarz, rura albo ziemia). Stawia się go, klikając na rurę.
 * PPM otwiera filtr (9 slotów), Shift + PPM pokazuje energię.
 */
public class BlockRequester extends BlockFacing implements ITargetOnly {
    public BlockRequester() {
        super("requester", Material.iron);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileRequester();
    }

    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        ForgeDirection f = facing(world, x, y, z);
        return fromTube == f || fromTube == f.getOpposite();
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
        if (world.getTileEntity(x, y, z) instanceof TileRequester te) te.retrieve();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (world.isRemote) return true;
        if (!(world.getTileEntity(x, y, z) instanceof TileRequester te)) return false;
        if (player.isSneaking()) EnergyUtil.showEnergy(player, te.energy.get(), te.energy.capacity);
        else player.openGui(RedCircuit.instance, GuiHandler.CHEST, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (world.getTileEntity(x, y, z) instanceof TileRequester te) InvUtil.dropAll(world, x, y, z, te.filter);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
