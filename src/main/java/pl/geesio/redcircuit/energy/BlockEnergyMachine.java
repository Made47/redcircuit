package pl.geesio.redcircuit.energy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.InvUtil;

import java.util.function.Supplier;

/**
 * Blok maszyny energii. Metadane: bity 0-2 = przód (2-5, jeśli maszyna ma przód), bit 3 = pracuje (światło).
 * Tekstury według {@link Faces}: name_front(_on), name_side(_on), name_top, name_bottom.
 */
public class BlockEnergyMachine extends Block {
    /** Zestawy tekstur. */
    public enum Faces {
        /** przód / przód_on / bok / góra (piece, generator na paliwo) */
        FURNACE,
        /** przód / bok / góra (generator kinetyczny) */
        FRONT,
        /** bok / bok_on / góra (ładowarka) */
        LIT_SIDES,
        /** bok / góra / spód (akumulator) */
        BOX,
        /** bok / góra (termostos, panel słoneczny) */
        PILLAR
    }

    protected final String name;
    private final Faces faces;
    private final Supplier<TileEntity> factory;
    private final int litLight;

    @SideOnly(Side.CLIENT)
    private IIcon front, frontOn, side, sideOn, top, bottom;

    public BlockEnergyMachine(String name, Faces faces, Supplier<TileEntity> factory, int litLight) {
        super(Material.iron);
        this.name = name;
        this.faces = faces;
        this.factory = factory;
        this.litLight = litLight;
        setBlockName(RedCircuit.MODID + "." + name);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(RCTab.TAB);
        setHarvestLevel("pickaxe", 0);
    }

    public boolean hasFacing() {
        return faces == Faces.FURNACE || faces == Faces.FRONT;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return factory.get();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (!hasFacing()) return;
        int dir = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int facing = switch (dir) {
            case 0 -> 2;
            case 1 -> 5;
            case 2 -> 3;
            default -> 4;
        };
        world.setBlockMetadataWithNotify(x, y, z, facing, 2);
    }

    public static ForgeDirection facing(IBlockAccess world, int x, int y, int z) {
        int f = world.getBlockMetadata(x, y, z) & 7;
        return f >= 2 && f <= 5 ? ForgeDirection.getOrientation(f) : ForgeDirection.NORTH;
    }

    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        if (!hasFacing() || (axis != ForgeDirection.UP && axis != ForgeDirection.DOWN)) return false;
        int meta = world.getBlockMetadata(x, y, z);
        ForgeDirection f = facing(world, x, y, z);
        f = axis == ForgeDirection.UP ? Dirs.cw(f) : Dirs.ccw(f);
        world.setBlockMetadataWithNotify(x, y, z, f.ordinal() | (meta & 8), 3);
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int s, float hx, float hy, float hz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEnergyInv) {
            if (!world.isRemote) player.openGui(RedCircuit.instance, GuiHandler.MACHINE, world, x, y, z);
            return true;
        }
        if (te instanceof IInfoTile info) {
            if (!world.isRemote) info.showInfo(player);
            return true;
        }
        return false;
    }

    public static void setLit(World world, int x, int y, int z, boolean lit) {
        int meta = world.getBlockMetadata(x, y, z);
        int nm = (meta & 7) | (lit ? 8 : 0);
        if (nm == meta) return;
        world.setBlockMetadataWithNotify(x, y, z, nm, 3);
        world.updateLightByType(EnumSkyBlock.Block, x, y, z);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return litLight > 0 && world.getBlock(x, y, z) == this && (world.getBlockMetadata(x, y, z) & 8) != 0 ? litLight : 0;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof IInventory inv) InvUtil.dropAll(world, x, y, z, inv);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int s) {
        TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof IInventory inv ? Container.calcRedstoneFromInventory(inv) : 0;
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        String p = RedCircuit.MODID + ":" + name;
        side = reg.registerIcon(p + "_side");
        top = reg.registerIcon(p + "_top");
        bottom = top;
        switch (faces) {
            case FURNACE -> {
                front = reg.registerIcon(p + "_front");
                frontOn = reg.registerIcon(p + "_front_on");
            }
            case FRONT -> front = frontOn = reg.registerIcon(p + "_front");
            case LIT_SIDES -> sideOn = reg.registerIcon(p + "_side_on");
            case BOX -> bottom = reg.registerIcon(p + "_bottom");
            default -> { }
        }
        blockIcon = side;
    }

    @SideOnly(Side.CLIENT)
    private IIcon icon(int s, int meta, int facing) {
        boolean lit = (meta & 8) != 0;
        if (s == 0) return bottom;
        if (s == 1) return top;
        if (hasFacing() && s == facing) return lit ? frontOn : front;
        if (faces == Faces.LIT_SIDES && lit) return sideOn;
        return side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int s, int meta) {
        return icon(s, meta & 8, 3);   // przedmiot: przód do gracza
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int s) {
        int meta = world.getBlockMetadata(x, y, z);
        return icon(s, meta, meta & 7);
    }
}
