package pl.geesio.redcircuit.fluid;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.InvUtil;

import java.util.ArrayList;
import java.util.Random;

/**
 * Zbiornik na ciecz. PPM wiadrem (lub innym pojemnikiem) nalewa/zabiera ciecz, pustą ręką pokazuje zawartość.
 * Rury i pompy łączą się z nim z każdej strony. Komparator odczytuje poziom, świecące ciecze (lawa) dają światło.
 * Po rozbiciu zawartość zostaje w przedmiocie (NBT "Fluid").
 */
public class BlockFluidTank extends Block {
    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockFluidTank() {
        super(Material.glass);
        setBlockName(RedCircuit.MODID + ".fluid_tank");
        setHardness(1.5F);
        setStepSound(soundTypeGlass);
        setLightOpacity(0);
        setCreativeTab(RCTab.TAB);
    }

    public static TileFluidTank tile(IBlockAccess world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileFluidTank tank ? tank : null;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileFluidTank();
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
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        TileFluidTank t = tile(world, x, y, z);
        if (t == null) return false;
        if (player.getHeldItem() != null) {
            if (world.isRemote) return net.minecraftforge.fluids.FluidContainerRegistry.isContainer(player.getHeldItem());
            return FluidUtil.interact(player, t.tank);
        }
        if (!world.isRemote) {
            player.addChatComponentMessage(new ChatComponentTranslation("message.redcircuit.pipe",
                    FluidUtil.name(t.tank.getFluid()), t.tank.getFluidAmount(), TileFluidTank.CAPACITY));
        }
        return true;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileFluidTank t = tile(world, x, y, z);
        return t == null ? 0 : t.lightLevel();
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileFluidTank t = tile(world, x, y, z);
        if (t == null || t.tank.getFluidAmount() <= 0) return 0;
        return 1 + Math.round(t.fill() * 14);
    }

    // ------------------------------------------------------------------ przedmiot z zawartością

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        TileFluidTank t = tile(world, x, y, z);
        if (t != null && stack.hasTagCompound() && stack.getTagCompound().hasKey("Fluid")) {
            t.tank.setFluid(FluidStack.loadFluidStackFromNBT(stack.getTagCompound().getCompoundTag("Fluid")));
        }
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileFluidTank t = tile(world, x, y, z);
        if (t != null && player.capabilities.isCreativeMode) t.noDrop = true;
    }

    /** Przedmiot z zawartością wypada w breakBlock (blok-encja jeszcze istnieje); zwykłe dropy są puste. */
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileFluidTank t = tile(world, x, y, z);
        if (t != null && !t.noDrop && !world.isRemote) {
            ItemStack drop = new ItemStack(this);
            FluidStack fs = t.tank.getFluid();
            if (fs != null && fs.amount > 0) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("Fluid", fs.writeToNBT(new NBTTagCompound()));
                drop.setTagCompound(tag);
            }
            InvUtil.spawnRandom(world, x, y, z, drop);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        return new ArrayList<>();
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(RedCircuit.MODID + ":fluid_tank_side");
        top = reg.registerIcon(RedCircuit.MODID + ":fluid_tank_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side < 2 ? top : blockIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }
}
