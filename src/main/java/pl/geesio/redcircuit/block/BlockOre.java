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
import net.minecraft.world.World;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.item.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Rudy: 0 miedź, 1 cyna, 2 srebro, 3 azuryt (azuryt wypada jako pył, jak redstone). */
public class BlockOre extends Block implements ItemBlockMeta.IMetaNames {
    public static final String[] NAMES = {"copper", "tin", "silver", "azurite"};
    public static final int COPPER = 0, TIN = 1, SILVER = 2, AZURITE = 3;
    private static final int[] HARVEST = {1, 1, 2, 2};

    private final IIcon[] icons = new IIcon[NAMES.length];
    private final Random rand = new Random();

    public BlockOre() {
        super(Material.rock);
        setHardness(3.0F);
        setResistance(5.0F);
        setStepSound(soundTypeStone);
        setBlockName(RedCircuit.MODID + ".ore");
        setCreativeTab(RCTab.TAB);
        for (int i = 0; i < NAMES.length; i++) setHarvestLevel("pickaxe", HARVEST[i], i);
    }

    @Override
    public String nameFor(int meta) {
        return NAMES[MathHelper.clamp_int(meta, 0, NAMES.length - 1)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        for (int i = 0; i < NAMES.length; i++) icons[i] = register.registerIcon(RedCircuit.MODID + ":" + NAMES[i] + "_ore");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons[MathHelper.clamp_int(meta, 0, NAMES.length - 1)];
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
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        if (meta != AZURITE) return super.getDrops(world, x, y, z, meta, fortune);
        ArrayList<ItemStack> drops = new ArrayList<>();
        int count = 4 + world.rand.nextInt(2) + (fortune > 0 ? world.rand.nextInt(fortune + 1) : 0);
        drops.add(new ItemStack(ModItems.azurite, count));
        return drops;
    }

    @Override
    public int getExpDrop(IBlockAccess world, int meta, int fortune) {
        return meta == AZURITE ? MathHelper.getRandomIntegerInRange(rand, 1, 5) : 0;
    }
}
