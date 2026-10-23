package pl.geesio.redcircuit.machine;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.tube.ITubeConnectable;
import pl.geesio.redcircuit.tube.TileTube;
import pl.geesio.redcircuit.util.InvUtil;

import java.util.Random;

/**
 * Maszyna kierunkowa (port ExtractorBlock z 1.21): metadane 0-5 = przód (wyjście), bit 8 = zasilona.
 * Stawiana klikiem na bloku - przód odwrócony od klikniętej ściany. Impuls redstone (zbocze narastające)
 * wywołuje {@link #transfer} po 2 tickach. Tekstury: name_front, name_back, name_side.
 */
public abstract class BlockFacing extends Block implements ITubeConnectable, pl.geesio.redcircuit.wire.BlockRedWire.IRedstoneDevice {
    public static final int POWERED = 8;
    protected final String name;

    @SideOnly(Side.CLIENT)
    private IIcon front, back, side;

    protected BlockFacing(String name, Material material) {
        super(material);
        this.name = name;
        setBlockName(RedCircuit.MODID + "." + name);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(material == Material.iron ? soundTypeMetal : soundTypeStone);
        setCreativeTab(RCTab.TAB);
    }

    protected BlockFacing(String name) {
        this(name, Material.rock);
    }

    public static ForgeDirection facing(int meta) {
        int f = meta & 7;
        return f < 6 ? ForgeDirection.getOrientation(f) : ForgeDirection.NORTH;
    }

    public static ForgeDirection facing(IBlockAccess world, int x, int y, int z) {
        return facing(world.getBlockMetadata(x, y, z));
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hx, float hy, float hz, int meta) {
        return side;
    }

    /** Rura podłącza się do przodu (wyjścia). */
    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        return fromTube == facing(world, x, y, z).getOpposite();
    }

    // ------------------------------------------------------------------ sygnał

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
        if (powered == ((meta & POWERED) != 0)) return;
        world.setBlockMetadataWithNotify(x, y, z, powered ? meta | POWERED : meta & ~POWERED, 4);
        if (powered) world.scheduleBlockUpdate(x, y, z, this, 2);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!world.isRemote) transfer(world, x, y, z);
    }

    /** Reakcja na impuls redstone. */
    protected abstract void transfer(World world, int x, int y, int z);

    // ------------------------------------------------------------------ obrót

    /** Śrubokręt: kliknięta ściana staje się przodem (drugi raz - tyłem). */
    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        if (axis == ForgeDirection.UNKNOWN) return false;
        int meta = world.getBlockMetadata(x, y, z);
        int f = axis.ordinal();
        if (f == (meta & 7)) f = axis.getOpposite().ordinal();
        world.setBlockMetadataWithNotify(x, y, z, (meta & POWERED) | f, 3);
        return true;
    }

    @Override
    public ForgeDirection[] getValidRotations(World world, int x, int y, int z) {
        return ForgeDirection.VALID_DIRECTIONS;
    }

    // ------------------------------------------------------------------ dostarczanie przedmiotów

    /** Ile sztuk stosu można wypchnąć przodem {@code facing} maszyny w (x, y, z). */
    public static int deliverableCount(World world, int x, int y, int z, ForgeDirection facing, ItemStack stack, int color) {
        int fx = x + facing.offsetX, fy = y + facing.offsetY, fz = z + facing.offsetZ;
        TileEntity te = world.getTileEntity(fx, fy, fz);
        if (te instanceof TileTube tube) return tube.canAccept(color) ? stack.stackSize : 0;
        if (InvUtil.isItemBlock(world, fx, fy, fz)) return InvUtil.fits(world, fx, fy, fz, stack, facing.getOpposite());
        return InvUtil.open(world, fx, fy, fz) ? stack.stackSize : 0;
    }

    /** Wypycha stos przodem: do rury, inwentarza albo na ziemię. */
    public static void deliver(World world, int x, int y, int z, ForgeDirection facing, ItemStack stack, int color) {
        int fx = x + facing.offsetX, fy = y + facing.offsetY, fz = z + facing.offsetZ;
        TileEntity te = world.getTileEntity(fx, fy, fz);
        if (te instanceof TileTube tube) {
            tube.insert(stack, facing.getOpposite(), color);
            return;
        }
        if (InvUtil.isItemBlock(world, fx, fy, fz)) {
            stack = InvUtil.insert(world, fx, fy, fz, stack, facing.getOpposite(), false);
            if (stack == null) return;
        }
        InvUtil.spawn(world, fx, fy, fz, stack, facing);
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        String p = RedCircuit.MODID + ":" + name;
        front = reg.registerIcon(p + "_front");
        back = reg.registerIcon(p + "_back");
        side = reg.registerIcon(p + "_side");
        blockIcon = side;
    }

    @SideOnly(Side.CLIENT)
    private IIcon icon(int s, int facing) {
        if (s == facing) return front;
        if (s == ForgeDirection.getOrientation(facing).getOpposite().ordinal()) return back;
        return side;
    }

    /** Przedmiot w ekwipunku: przód na południe (do gracza). */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int s, int meta) {
        return icon(s, 3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int s) {
        return icon(s, facing(world, x, y, z).ordinal());
    }
}
