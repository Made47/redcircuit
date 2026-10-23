package pl.geesio.redcircuit.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.machine.BlockFacing;

/**
 * Pompa. Stawia się ją, klikając na źródło (zbiornik / jezioro) - zasysa z tyłu, tłoczy do przodu.
 * PPM pokazuje zawartość i energię. Pracuje stale (co 10 ticków), sygnał redstone ją zatrzymuje.
 */
public class BlockPump extends BlockFacing {
    public BlockPump() {
        super("pump", Material.iron);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TilePump();
    }

    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        // Stan redstone sprawdza pompa przy każdym cyklu
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TilePump te) {
            player.addChatComponentMessage(new ChatComponentTranslation("message.redcircuit.pump", FluidUtil.name(te.tank.getFluid()),
                    te.tank.getFluidAmount(), TilePump.CAPACITY, te.energy.get()));
        }
        return true;
    }
}
