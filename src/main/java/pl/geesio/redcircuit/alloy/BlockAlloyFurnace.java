package pl.geesio.redcircuit.alloy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
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
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

import java.util.Random;

/** Piec do stopów. Metadane: bity 0-2 = kierunek przodu (2-5), bit 3 = pali się. */
public class BlockAlloyFurnace extends BlockContainer {
    private IIcon front, frontOn, side, top;

    private final boolean electric;

    public BlockAlloyFurnace() {
        this(false);
    }

    /** @param electric elektryczny piec do stopów (energia zamiast paliwa) */
    public BlockAlloyFurnace(boolean electric) {
        super(electric ? Material.iron : Material.rock);
        this.electric = electric;
        setHardness(3.5F);
        setStepSound(electric ? soundTypeMetal : soundTypeStone);
        setBlockName(RedCircuit.MODID + (electric ? ".power_alloy_furnace" : ".alloy_furnace"));
        setCreativeTab(RCTab.TAB);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileAlloyFurnace(electric);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        front = register.registerIcon(RedCircuit.MODID + ":" + (electric ? "power_" : "") + "alloy_furnace_front");
        frontOn = register.registerIcon(RedCircuit.MODID + ":" + (electric ? "power_" : "") + "alloy_furnace_front_on");
        side = register.registerIcon(RedCircuit.MODID + ":" + (electric ? "power_" : "") + "alloy_furnace_side");
        top = register.registerIcon(RedCircuit.MODID + ":" + (electric ? "power_" : "") + "alloy_furnace_top");
        blockIcon = side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int s, int meta) {
        if (s == 0 || s == 1) return top;
        int facing = meta & 7;
        if (facing < 2) facing = 3;   // przedmiot w ekwipunku: przód od strony gracza
        if (s != facing) return side;
        return (meta & 8) != 0 ? frontOn : front;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int dir = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int facing = switch (dir) {
            case 0 -> 2;
            case 1 -> 5;
            case 2 -> 3;
            default -> 4;
        };
        world.setBlockMetadataWithNotify(x, y, z, facing, 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int s, float hx, float hy, float hz) {
        if (!world.isRemote) player.openGui(RedCircuit.instance, GuiHandler.ALLOY_FURNACE, world, x, y, z);
        return true;
    }

    /** Zmiana stanu "pali się" bez wymiany bloku-encji. */
    /** Obrót śrubokrętem wokół osi pionowej (UP w prawo, DOWN w lewo). */
    @Override
    public boolean rotateBlock(World world, int x, int y, int z, net.minecraftforge.common.util.ForgeDirection axis) {
        if (axis != net.minecraftforge.common.util.ForgeDirection.UP && axis != net.minecraftforge.common.util.ForgeDirection.DOWN) return false;
        int meta = world.getBlockMetadata(x, y, z);
        net.minecraftforge.common.util.ForgeDirection f = net.minecraftforge.common.util.ForgeDirection.getOrientation(meta & 7);
        if (f.offsetY != 0 || f == net.minecraftforge.common.util.ForgeDirection.UNKNOWN) return false;
        f = axis == net.minecraftforge.common.util.ForgeDirection.UP ? pl.geesio.redcircuit.util.Dirs.cw(f) : pl.geesio.redcircuit.util.Dirs.ccw(f);
        world.setBlockMetadataWithNotify(x, y, z, f.ordinal() | (meta & 8), 3);
        return true;
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
        return world.getBlock(x, y, z) == this && (world.getBlockMetadata(x, y, z) & 8) != 0 ? 13 : 0;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (world.getTileEntity(x, y, z) instanceof IInventory inv) {
            Random rand = world.rand;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s == null) continue;
                EntityItem e = new EntityItem(world, x + 0.5 + rand.nextFloat() * 0.4 - 0.2, y + 0.5,
                        z + 0.5 + rand.nextFloat() * 0.4 - 0.2, s.copy());
                e.motionX = rand.nextGaussian() * 0.05;
                e.motionY = rand.nextGaussian() * 0.05 + 0.2;
                e.motionZ = rand.nextGaussian() * 0.05;
                world.spawnEntityInWorld(e);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int s) {
        return Container.calcRedstoneFromInventory((IInventory) world.getTileEntity(x, y, z));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & 8) == 0) return;
        int facing = meta & 7;
        double fx = x + 0.5, fy = y + 0.2 + rand.nextFloat() * 0.5, fz = z + 0.5;
        double off = 0.52, along = rand.nextFloat() * 0.6 - 0.3;
        switch (facing) {
            case 2 -> fz -= off;
            case 3 -> fz += off;
            case 4 -> fx -= off;
            default -> fx += off;
        }
        if (facing == 2 || facing == 3) fx += along;
        else fz += along;
        world.spawnParticle("smoke", fx, fy, fz, 0, 0, 0);
        world.spawnParticle("flame", fx, fy, fz, 0, 0, 0);
    }
}
