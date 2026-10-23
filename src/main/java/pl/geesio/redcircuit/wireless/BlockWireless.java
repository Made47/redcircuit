package pl.geesio.redcircuit.wireless;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.RenderIds;
import pl.geesio.redcircuit.wire.BlockRedWire;

/**
 * Wspólna baza nadajnika i odbiornika: płytka z anteną, stawiana na podłożu. PPM ustawia częstotliwość.
 * Metadane: 1 = zasilony / nadaje (świecąca końcówka anteny).
 */
public abstract class BlockWireless extends Block implements BlockRedWire.IRedstoneDevice {
    private final String name;

    @SideOnly(Side.CLIENT)
    private IIcon top, side, rod, tipOff, tipOn;

    protected BlockWireless(String name) {
        super(Material.circuits);
        this.name = name;
        setBlockName(RedCircuit.MODID + "." + name);
        setHardness(0.0F);
        setStepSound(soundTypeStone);
        setCreativeTab(RCTab.TAB);
        // Ramka zaznaczenia obejmuje antenę; kolizja tylko z płytką (getCollisionBoundingBoxFromPool)
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 13.0F / 16.0F, 1.0F);
    }

    public static TileWireless tile(IBlockAccess world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileWireless w ? w : null;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileWireless();
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
        return RenderIds.wireless;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 0.125, z + 1);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    /** Usuwa blok bez podłoża; zwraca true, gdy blok zniknął. */
    protected boolean dropIfUnsupported(World world, int x, int y, int z) {
        if (canBlockStay(world, x, y, z)) return false;
        dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlockToAir(x, y, z);
        return true;
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return side >= 0;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int s, float hx, float hy, float hz) {
        TileWireless t = tile(world, x, y, z);
        net.minecraft.item.ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() instanceof pl.geesio.redcircuit.tool.ItemMultimeter) return true;   // miernik działa w onItemUseFirst
        if (world.isRemote && t != null) {
            RedCircuit.proxy.openFrequencyGui(0, x, y, z, t.getFrequency(), getUnlocalizedName() + ".name");
        }
        return true;
    }

    /** Zmiana częstotliwości z okna (serwer). */
    public void applyFrequency(World world, int x, int y, int z, int freq) {
        TileWireless t = tile(world, x, y, z);
        if (t != null) t.setFrequency(freq);
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        String p = RedCircuit.MODID + ":";
        top = reg.registerIcon(p + name + "_top");
        side = reg.registerIcon(p + "wireless_side");
        rod = reg.registerIcon(p + "wireless_rod");
        tipOff = reg.registerIcon(p + "wireless_tip_off");
        tipOn = reg.registerIcon(p + "wireless_tip_on");
        blockIcon = top;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int s, int meta) {
        return s == 1 ? top : side;
    }

    @SideOnly(Side.CLIENT)
    public IIcon rodIcon() {
        return rod;
    }

    @SideOnly(Side.CLIENT)
    public IIcon tipIcon(boolean on) {
        return on ? tipOn : tipOff;
    }
}
