package pl.geesio.redcircuit.tube;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.RenderIds;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Dyes;
import pl.geesio.redcircuit.util.IItemAcceptor;
import pl.geesio.redcircuit.util.InvUtil;
import pl.geesio.redcircuit.wire.WireTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Rura pneumatyczna transportująca przedmioty. PPM barwnikiem maluje rurę - pomalowana przyjmuje tylko
 * przedmioty w tym kolorze. Shift + PPM pustą ręką usuwa farbę. Rura restrykcyjna jest "droga" przy wyznaczaniu trasy.
 */
public class BlockTube extends Block {
    public static final float HALF = 3.0F;
    public static final int RESTRICTION_COST = 1000;

    /** Rodzaje rur: zwykła, restrykcyjna (droga przy wyznaczaniu trasy), akcelerator (przyspiesza za energię). */
    public enum Kind { TUBE, RESTRICTION, ACCELERATOR }

    private final Kind kind;
    private final boolean restriction;

    @SideOnly(Side.CLIENT)
    private IIcon base;
    @SideOnly(Side.CLIENT)
    private IIcon[] painted;

    public BlockTube(boolean restriction) {
        this(restriction ? Kind.RESTRICTION : Kind.TUBE);
    }

    public BlockTube(Kind kind) {
        super(Material.iron);
        this.kind = kind;
        this.restriction = kind == Kind.RESTRICTION;
        setBlockName(RedCircuit.MODID + "." + texture());
        setHardness(restriction ? 0.6F : 0.5F);
        setStepSound(soundTypeMetal);
        setCreativeTab(RCTab.TAB);
    }

    private String texture() {
        return switch (kind) {
            case RESTRICTION -> "restriction_tube";
            case ACCELERATOR -> "accelerator";
            default -> "tube";
        };
    }

    /** Koszt przejazdu przez tę rurę przy wyznaczaniu trasy. */
    public int routeCost() {
        return restriction ? RESTRICTION_COST : 1;
    }

    public static TileTube tile(IBlockAccess world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileTube tube ? tube : null;
    }

    public static int paint(IBlockAccess world, int x, int y, int z) {
        TileTube t = tile(world, x, y, z);
        return t == null ? TileTube.NO_PAINT : t.paint;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return kind == Kind.ACCELERATOR ? new TileAccelerator() : new TileTube();
    }

    // ------------------------------------------------------------------ połączenia

    /** Czy rura w (x, y, z) jest połączona w kierunku {@code dir}. */
    public static boolean connected(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
        return world.getBlock(x, y, z) instanceof BlockTube && connects(world, x, y, z, dir);
    }

    public static boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
        int nx = x + dir.offsetX, ny = y + dir.offsetY, nz = z + dir.offsetZ;
        if (ny < 0 || ny >= 256) return false;
        Block b = world.getBlock(nx, ny, nz);
        if (b instanceof BlockTube) {
            int p1 = paint(world, x, y, z), p2 = paint(world, nx, ny, nz);
            return p1 == TileTube.NO_PAINT || p2 == TileTube.NO_PAINT || p1 == p2;
        }
        if (b instanceof ITubeConnectable tc) return tc.connectsToTube(world, nx, ny, nz, dir);
        if (b instanceof WireTypes.IWire) return false;
        TileEntity te = world.getTileEntity(nx, ny, nz);
        return te instanceof IInventory || te instanceof IItemAcceptor;
    }

    /** Prostopadłościany (piksele): środek + ramiona do połączonych sąsiadów. */
    public static List<float[]> boxes(IBlockAccess world, int x, int y, int z) {
        List<float[]> out = new ArrayList<>();
        float a = 8 - HALF, b = 8 + HALF;
        out.add(new float[]{a, a, a, b, b, b});
        for (ForgeDirection d : Dirs.ALL) {
            if (world == null ? Dirs.axis(d) != 1 : !connects(world, x, y, z, d)) continue;
            out.add(arm(d, a, b));
        }
        return out;
    }

    public static float[] arm(ForgeDirection d, float a, float b) {
        return switch (d) {
            case DOWN -> new float[]{a, 0, a, b, a, b};
            case UP -> new float[]{a, b, a, b, 16, b};
            case NORTH -> new float[]{a, a, 0, b, b, a};
            case SOUTH -> new float[]{a, a, b, b, b, 16};
            case WEST -> new float[]{0, a, a, a, b, b};
            default -> new float[]{b, a, a, 16, b, b};
        };
    }

    // ------------------------------------------------------------------ kształt

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
        return RenderIds.tube;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float[] u = {16, 16, 16, 0, 0, 0};
        for (float[] b : boxes(world, x, y, z)) {
            for (int i = 0; i < 3; i++) {
                u[i] = Math.min(u[i], b[i]);
                u[i + 3] = Math.max(u[i + 3], b[i + 3]);
            }
        }
        setBlockBounds(u[0] / 16F, u[1] / 16F, u[2] / 16F, u[3] / 16F, u[4] / 16F, u[5] / 16F);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0, (8 - HALF) / 16F, (8 - HALF) / 16F, 1, (8 + HALF) / 16F, (8 + HALF) / 16F);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        for (float[] b : boxes(world, x, y, z)) {
            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(x + b[0] / 16, y + b[1] / 16, z + b[2] / 16,
                    x + b[3] / 16, y + b[4] / 16, z + b[5] / 16);
            if (mask.intersectsWith(bb)) list.add(bb);
        }
    }

    // ------------------------------------------------------------------ malowanie

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        TileTube t = tile(world, x, y, z);
        if (t == null) return false;
        ItemStack held = player.getHeldItem();
        int color = Dyes.colorOf(held);
        if (color >= 0) {
            if (t.paint == color) return false;
            if (!world.isRemote) repaint(world, x, y, z, t, color);
            return true;
        }
        if (held == null && player.isSneaking() && t.paint != TileTube.NO_PAINT) {
            if (!world.isRemote) repaint(world, x, y, z, t, TileTube.NO_PAINT);
            return true;
        }
        if (held == null && !player.isSneaking() && t instanceof TileAccelerator acc) {
            if (!world.isRemote) pl.geesio.redcircuit.energy.EnergyUtil.showEnergy(player, acc.energy.get(), acc.energy.capacity);
            return true;
        }
        return false;
    }

    private void repaint(World world, int x, int y, int z, TileTube t, int paint) {
        t.paint = paint;
        t.sync();
        world.notifyBlocksOfNeighborChange(x, y, z, this);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileTube t = tile(world, x, y, z);
        if (t != null && !world.isRemote) {
            for (TubeItem item : t.getItems()) InvUtil.spawnRandom(world, x, y, z, item.stack);
            t.getItems().clear();
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        base = reg.registerIcon(RedCircuit.MODID + ":" + texture());
        blockIcon = base;
        painted = new IIcon[16];
        String[] colors = pl.geesio.redcircuit.light.BlockLamp.COLORS;
        for (int i = 0; i < 16; i++) painted[i] = reg.registerIcon(RedCircuit.MODID + ":tube_" + colors[i]);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return base;
    }

    /** Tekstura środka rury: kolor farby albo zwykła. */
    @SideOnly(Side.CLIENT)
    public IIcon centerIcon(int paint) {
        return paint >= 0 && paint < 16 ? painted[paint] : base;
    }
}
