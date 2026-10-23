package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Maszyna kierunkowa z inwentarzem (9 albo 18 slotów, okno jak skrzynia). Przód = wyjście;
 * impuls redstone wywołuje {@link #pulse}. Rury podłączają się z każdej strony (z boków i z tyłu - wejście).
 */
public abstract class BlockContainerMachine extends BlockFacing {
    private final int rows;

    protected BlockContainerMachine(String name, Material material, int rows) {
        super(name, material);
        this.rows = rows;
    }

    public static TileMachine tile(IBlockAccess world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileMachine m ? m : null;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileMachine(rows * 9, "container.redcircuit." + name, true);
    }

    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        return true;
    }

    /** Czy maszyna tyka co tick (domyślnie nie). */
    protected boolean ticks() {
        return false;
    }

    protected void serverTick(World world, int x, int y, int z, TileMachine te) {
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
        TileMachine te = tile(world, x, y, z);
        if (te != null) pulse(world, x, y, z, te);
    }

    /** Reakcja na impuls redstone. */
    protected abstract void pulse(World world, int x, int y, int z, TileMachine te);

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (!world.isRemote) player.openGui(RedCircuit.instance, GuiHandler.CHEST, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileMachine te = tile(world, x, y, z);
        if (te != null) InvUtil.dropAll(world, x, y, z, te.items);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
