package pl.geesio.redcircuit.wire;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneLight;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.block.ItemBlockMeta;
import pl.geesio.redcircuit.util.Pos;

import java.util.List;

/**
 * Przewód ze stopu sygnałowego (insulated = false) albo izolowany (insulated = true, kolor = metadane 0..15).
 * Przewody nie tracą mocy na odległości - cała sieć ma jeden poziom sygnału równy najsilniejszemu wejściu.
 */
public class BlockRedWire extends BlockFaceWire implements ItemBlockMeta.IMetaNames {
    public static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private final boolean insulated;
    private IIcon off, on;
    private final IIcon[] colored = new IIcon[16];

    public BlockRedWire(boolean insulated) {
        super(Material.circuits, insulated ? 4f : 2f, insulated ? 3f : 2f);
        this.insulated = insulated;
        setStepSound(insulated ? soundTypeCloth : soundTypeMetal);
        setBlockName(RedCircuit.MODID + (insulated ? ".insulated_wire" : ".signal_alloy_wire"));
        setCreativeTab(RCTab.TAB);
    }

    public boolean isInsulated() {
        return insulated;
    }

    @Override
    public int wireType(int meta) {
        return insulated ? (meta & 15) : WireTypes.RED;
    }

    @Override
    public String nameFor(int meta) {
        return insulated ? COLORS[meta & 15] : null;
    }

    @Override
    public int damageDropped(int meta) {
        return insulated ? meta & 15 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        if (!insulated) {
            list.add(new ItemStack(item));
            return;
        }
        for (int i = 0; i < 16; i++) list.add(new ItemStack(item, 1, i));
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        if (insulated) {
            for (int i = 0; i < 16; i++) colored[i] = register.registerIcon(RedCircuit.MODID + ":insulated_" + COLORS[i]);
            blockIcon = colored[14];
        } else {
            off = register.registerIcon(RedCircuit.MODID + ":signal_alloy_off");
            on = register.registerIcon(RedCircuit.MODID + ":signal_alloy_on");
            blockIcon = off;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return insulated ? colored[meta & 15] : off;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int meta, int power) {
        return insulated ? colored[meta & 15] : power > 0 ? on : off;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (insulated) return colored[world.getBlockMetadata(x, y, z) & 15];
        TileWire t = tile(world, x, y, z);
        return t != null && t.power > 0 ? on : off;
    }

    // ------------------------------------------------------------------ połączenia

    @Override
    public boolean connectsToDevice(IBlockAccess world, Pos pos, ForgeDirection side, ForgeDirection dir, Pos np, Block nb) {
        if (nb instanceof pl.geesio.redcircuit.gate.BlockGate) {
            return side == ForgeDirection.DOWN && pl.geesio.redcircuit.gate.BlockGate.connectsOn(world, np.x, np.y, np.z, dir.getOpposite());
        }
        return isRedstoneDevice(world, np, nb);
    }

    /** Jak niżej, ale bramki tylko od strony wyjść i włączonych wejść ({@code dir} = od przewodu do bloku). */
    public static boolean isRedstoneDevice(IBlockAccess world, Pos np, Block b, ForgeDirection dir) {
        if (b instanceof pl.geesio.redcircuit.gate.BlockGate) {
            return pl.geesio.redcircuit.gate.BlockGate.connectsOn(world, np.x, np.y, np.z, dir.getOpposite());
        }
        return isRedstoneDevice(world, np, b);
    }

    /** Bloki redstone, z którymi łączą się przewody sygnałowe (bez innych przewodów). */
    public static boolean isRedstoneDevice(IBlockAccess world, Pos np, Block b) {
        if (b instanceof WireTypes.IWire) return false;
        if (b instanceof IRedstoneDevice) return true;
        if (b.canProvidePower()) return true;
        return b instanceof BlockRedstoneLight || b instanceof BlockPistonBase || b instanceof BlockDoor
                || b instanceof BlockTrapDoor || b instanceof BlockFenceGate || b instanceof BlockNote
                || b instanceof BlockDispenser || b instanceof BlockTNT || b instanceof BlockHopper
                || b instanceof BlockRailPowered || b instanceof BlockCommandBlock;
    }

    /** Bloki RedCircuit sterowane sygnałem (bramki, lampy, maszyny) - przewody się z nimi łączą. */
    public interface IRedstoneDevice {
    }

    // ------------------------------------------------------------------ redstone

    @Override
    public boolean canProvidePower() {
        return true;
    }

    public static int power(IBlockAccess world, int x, int y, int z) {
        TileWire t = tile(world, x, y, z);
        return t == null ? 0 : t.power;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return WireNetwork.isEmitting() ? power(world, x, y, z) : 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return WireNetwork.isEmitting() ? power(world, x, y, z) : 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    protected void onSignalNeighborChanged(World world, int x, int y, int z, Block neighbor) {
        WireNetwork.update(world, x, y, z);
    }

    @Override
    public void onWireChanged(World world, int x, int y, int z) {
        WireNetwork.onWireChanged(world, x, y, z);
    }

    @Override
    protected void onWireRemoved(World world, int x, int y, int z) {
        WireNetwork.onWireRemoved(world, x, y, z, this);
    }
}
