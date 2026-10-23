package pl.geesio.redcircuit.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.item.ModItems;

import java.util.List;

/**
 * Bloki materiałów: 0 miedź, 1 cyna, 2 srebro, 3 azuryt, 4 mosiądz, 5 stal.
 * Metale używają tekstury bloku żelaza z gry przebarwionej na kolor metalu.
 */
public class BlockStorage extends Block implements ItemBlockMeta.IMetaNames {
    public static final String[] NAMES = {"copper", "tin", "silver", "azurite", "brass", "steel"};
    public static final int COPPER = 0, TIN = 1, SILVER = 2, AZURITE = 3, BRASS = 4, STEEL = 5;
    private static final int[] COLORS = {ModItems.COPPER, ModItems.TIN, ModItems.SILVER, 0xFFFFFF, ModItems.BRASS, ModItems.STEEL};

    private IIcon metal, azurite;

    public BlockStorage() {
        super(Material.iron);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setBlockName(RedCircuit.MODID + ".storage");
        setCreativeTab(RCTab.TAB);
        for (int i = 0; i < NAMES.length; i++) setHarvestLevel("pickaxe", 1, i);
    }

    @Override
    public String nameFor(int meta) {
        return NAMES[MathHelper.clamp_int(meta, 0, NAMES.length - 1)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        metal = register.registerIcon("iron_block");
        azurite = register.registerIcon(RedCircuit.MODID + ":azurite_block");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return meta == AZURITE ? azurite : metal;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return COLORS[MathHelper.clamp_int(meta, 0, COLORS.length - 1)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return getRenderColor(world.getBlockMetadata(x, y, z));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < NAMES.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    public boolean isBeaconBase(IBlockAccess world, int x, int y, int z, int beaconX, int beaconY, int beaconZ) {
        return true;
    }
}
