package pl.geesio.redcircuit.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.RenderIds;
import pl.geesio.redcircuit.client.IBoxBlock;
import pl.geesio.redcircuit.tube.BlockTube;
import pl.geesio.redcircuit.util.Dirs;

import java.util.ArrayList;
import java.util.List;

/** Rura do płynów (mosiężna). Łączy się z innymi rurami, z pompą i z każdym blokiem przyjmującym płyny (IFluidHandler). */
public class BlockFluidPipe extends Block implements IBoxBlock {
    private static final float HALF = 3.0F;

    public BlockFluidPipe() {
        super(Material.iron);
        setBlockName(RedCircuit.MODID + ".fluid_pipe");
        setBlockTextureName(RedCircuit.MODID + ":fluid_pipe");
        setHardness(0.5F);
        setStepSound(soundTypeMetal);
        setCreativeTab(RCTab.TAB);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileFluidPipe();
    }

    public static boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
        int nx = x + dir.offsetX, ny = y + dir.offsetY, nz = z + dir.offsetZ;
        if (ny < 0 || ny >= 256) return false;
        Block b = world.getBlock(nx, ny, nz);
        if (b instanceof BlockFluidPipe || b instanceof BlockPump) return true;
        if (b instanceof BlockTube || b instanceof pl.geesio.redcircuit.wire.WireTypes.IWire) return false;
        return world.getTileEntity(nx, ny, nz) instanceof IFluidHandler;
    }

    @Override
    public List<float[]> renderBoxes(IBlockAccess world, int x, int y, int z) {
        List<float[]> out = new ArrayList<>();
        float a = 8 - HALF, b = 8 + HALF;
        out.add(new float[]{a, a, a, b, b, b});
        for (ForgeDirection d : Dirs.ALL) {
            if (world == null ? Dirs.axis(d) != 1 : !connects(world, x, y, z, d)) continue;
            out.add(BlockTube.arm(d, a, b));
        }
        return out;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return RenderIds.boxes;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float[] u = {16, 16, 16, 0, 0, 0};
        for (float[] b : renderBoxes(world, x, y, z)) {
            for (int i = 0; i < 3; i++) {
                u[i] = Math.min(u[i], b[i]);
                u[i + 3] = Math.max(u[i + 3], b[i + 3]);
            }
        }
        setBlockBounds(u[0] / 16F, u[1] / 16F, u[2] / 16F, u[3] / 16F, u[4] / 16F, u[5] / 16F);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds((8 - HALF) / 16F, (8 - HALF) / 16F, 0, (8 + HALF) / 16F, (8 + HALF) / 16F, 1);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        for (float[] b : renderBoxes(world, x, y, z)) {
            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(x + b[0] / 16, y + b[1] / 16, z + b[2] / 16,
                    x + b[3] / 16, y + b[4] / 16, z + b[5] / 16);
            if (mask.intersectsWith(bb)) list.add(bb);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (player.getHeldItem() != null) return false;
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileFluidPipe t) {
            player.addChatComponentMessage(new ChatComponentTranslation("message.redcircuit.pipe",
                    FluidUtil.name(t.tank.getFluid()), t.tank.getFluidAmount(), TileFluidPipe.CAPACITY));
        }
        return true;
    }
}
