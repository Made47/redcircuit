package pl.geesio.redcircuit.light;

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
import net.minecraft.world.World;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.block.ItemBlockMeta;
import pl.geesio.redcircuit.wire.BlockRedWire;

import java.util.List;
import java.util.Random;

/**
 * Lampa w 16 kolorach (metadane = kolor wełny). Zwykła świeci przy sygnale redstone, odwrócona - bez sygnału.
 * Jak lampa z gry: osobne bloki "świeci" / "zgaszona" (światło bloku w 1.7.10 nie zależy od stanu), zamieniane
 * przy zmianie sygnału. Gaśnie z opóźnieniem 4 ticków, żeby nie migotała przy krótkich impulsach.
 */
public class BlockLamp extends Block implements ItemBlockMeta.IMetaNames, BlockRedWire.IRedstoneDevice {
    public static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private final boolean inverted, lit;
    /** Bliźniaczy blok o przeciwnym stanie świecenia (ustawiany w ModBlocks). */
    private BlockLamp twin;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    /**
     * @param inverted lampa odwrócona
     * @param lit      wariant świecący
     */
    public BlockLamp(boolean inverted, boolean lit) {
        super(Material.redstoneLight);
        this.inverted = inverted;
        this.lit = lit;
        setHardness(0.3F);
        setStepSound(soundTypeGlass);
        setBlockName(RedCircuit.MODID + "." + (inverted ? "inverted_lamp" : "lamp"));
        if (lit) setLightLevel(1.0F);
        // W menu tylko stan domyślny: zwykła zgaszona, odwrócona świecąca
        if (lit == inverted) setCreativeTab(RCTab.TAB);
    }

    public static void pair(BlockLamp a, BlockLamp b) {
        a.twin = b;
        b.twin = a;
    }

    public boolean isInverted() {
        return inverted;
    }

    public boolean isLit() {
        return lit;
    }

    /** Blok, który trafia do ekwipunku (stan domyślny). */
    private BlockLamp itemBlock() {
        return lit == inverted ? this : twin;
    }

    @Override
    public String nameFor(int meta) {
        return COLORS[MathHelper.clamp_int(meta, 0, 15)];
    }

    // ------------------------------------------------------------------ logika

    private boolean shouldBeLit(World world, int x, int y, int z) {
        return world.isBlockIndirectlyGettingPowered(x, y, z) != inverted;
    }

    private void swap(World world, int x, int y, int z) {
        world.setBlock(x, y, z, twin, world.getBlockMetadata(x, y, z), 2);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (world.isRemote) return;
        if (shouldBeLit(world, x, y, z) != lit) swap(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;
        boolean want = shouldBeLit(world, x, y, z);
        if (want == lit) return;
        if (want) swap(world, x, y, z);
        else world.scheduleBlockUpdate(x, y, z, this, 4);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!world.isRemote && lit && !shouldBeLit(world, x, y, z)) swap(world, x, y, z);
    }

    // ------------------------------------------------------------------ przedmiot

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(itemBlock());
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(itemBlock(), 1, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(itemBlock());
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < 16; i++) list.add(new ItemStack(item, 1, i));
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        icons = new IIcon[16];
        for (int i = 0; i < 16; i++) {
            icons[i] = reg.registerIcon(RedCircuit.MODID + ":lamp_" + COLORS[i] + (lit ? "_on" : "_off"));
        }
        blockIcon = icons[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons[meta & 15];
    }
}
