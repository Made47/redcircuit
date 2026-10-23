package pl.geesio.redcircuit.wire;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
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
import pl.geesio.redcircuit.block.ItemBlockMeta;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Pos;

import java.util.ArrayList;
import java.util.List;

/**
 * Przewód w obudowie: biegnie środkiem bloku jak rura. Łączy się z przewodami tego samego rodzaju -
 * także z przewodami przyklejonymi do ścian. Rodzaje: stop sygnałowy, izolowany (kolor = metadane), kabel wiązkowy.
 */
public class BlockEncasedWire extends Block implements WireTypes.IWire, ItemBlockMeta.IMetaNames {
    public enum Kind { SIGNAL, INSULATED, BUNDLED, POWER }

    private final Kind kind;
    private final float half;
    private final IIcon[] icons = new IIcon[16];

    public BlockEncasedWire(Kind kind) {
        super(Material.circuits);   // bez wymaganego narzędzia - wypada zawsze
        this.kind = kind;
        this.half = (kind == Kind.BUNDLED ? 8f : 6f) / 2f;
        setHardness(1.0F);
        setResistance(5.0F);
        setStepSound(soundTypeStone);
        setLightOpacity(0);
        setBlockName(RedCircuit.MODID + switch (kind) {
            case SIGNAL -> ".encased_signal_alloy_wire";
            case INSULATED -> ".encased_insulated_wire";
            case BUNDLED -> ".encased_bundled_cable";
            case POWER -> ".encased_power_alloy_wire";
        });
        setCreativeTab(RCTab.TAB);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public int wireType(int meta) {
        return switch (kind) {
            case SIGNAL -> WireTypes.RED;
            case INSULATED -> meta & 15;
            case BUNDLED -> WireTypes.BUNDLED;
            case POWER -> WireTypes.POWER;
        };
    }

    @Override
    public String nameFor(int meta) {
        return kind == Kind.INSULATED ? BlockRedWire.COLORS[meta & 15] : null;
    }

    @Override
    public int damageDropped(int meta) {
        return kind == Kind.INSULATED ? meta & 15 : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        if (kind != Kind.INSULATED) {
            list.add(new ItemStack(item));
            return;
        }
        for (int i = 0; i < 16; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return kind == Kind.POWER ? new pl.geesio.redcircuit.energy.TilePowerWire() : new TileWire();
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        switch (kind) {
            case SIGNAL -> blockIcon = register.registerIcon(RedCircuit.MODID + ":encased_signal_alloy_wire");
            case BUNDLED -> blockIcon = register.registerIcon(RedCircuit.MODID + ":encased_bundled_cable");
            case POWER -> blockIcon = register.registerIcon(RedCircuit.MODID + ":encased_power_alloy_wire");
            case INSULATED -> {
                for (int i = 0; i < 16; i++) {
                    icons[i] = register.registerIcon(RedCircuit.MODID + ":encased_insulated_wire_" + BlockRedWire.COLORS[i]);
                }
                blockIcon = icons[14];
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return kind == Kind.INSULATED ? icons[meta & 15] : blockIcon;
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
        return RenderIds.encasedWire;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return false;
    }

    // ------------------------------------------------------------------ połączenia

    /** Czy łączy się z sąsiadem w kierunku {@code dir}. */
    public boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
        int self = wireType(world.getBlockMetadata(x, y, z));
        Pos np = new Pos(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
        Block nb = world.getBlock(np.x, np.y, np.z);
        if (PartWires.isHost(world, np)) {
            // Przewody-części ForgeMultipart na ścianach prostopadłych do kierunku połączenia
            for (ForgeDirection side : Dirs.ALL) {
                if (Dirs.sameAxis(side, dir)) continue;
                Integer pt = PartWires.wireAt(world, np, side.ordinal());
                if (pt != null && WireTypes.compatible(self, pt)) return true;
            }
        }
        Integer t = WireTypes.typeAt(world, np.x, np.y, np.z);
        if (t != null) {
            if (!WireTypes.compatible(self, t)) return false;
            if (nb instanceof BlockFaceWire) {
                // Przewód na ścianie łączy się z nami, jeśli leży na ścianie prostopadłej do kierunku połączenia
                for (ForgeDirection side : Dirs.ALL) {
                    if (!Dirs.sameAxis(side, dir) && BlockFaceWire.hasFace(world, np, side)) return true;
                }
                return false;
            }
            return true;
        }
        return switch (kind) {
            case BUNDLED -> BundledIO.isDevice(world, np, nb);
            case POWER -> pl.geesio.redcircuit.energy.EnergyUtil.connects(world, x, y, z, dir);
            default -> BlockRedWire.isRedstoneDevice(world, np, nb, dir);
        };
    }

    /** Prostopadłościany (piksele): środek + ramiona do połączonych sąsiadów. */
    public List<float[]> boxes(IBlockAccess world, int x, int y, int z) {
        List<float[]> out = new ArrayList<>();
        float a = 8 - half, b = 8 + half;
        out.add(new float[]{a, a, a, b, b, b});
        for (ForgeDirection d : Dirs.ALL) {
            if (world != null && !connects(world, x, y, z, d)) continue;
            if (world == null && Dirs.axis(d) != 2) continue;   // przedmiot: ramiona wschód-zachód
            float[] box = {a, a, a, b, b, b};
            int c = Dirs.coord(d);
            if (Dirs.positive(d)) {
                box[c] = b;
                box[c + 3] = 16;
            } else {
                box[c] = 0;
                box[c + 3] = a;
            }
            out.add(box);
        }
        return out;
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
        setBlockBounds(u[0] / 16f, u[1] / 16f, u[2] / 16f, u[3] / 16f, u[4] / 16f, u[5] / 16f);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, (8 - half) / 16f, (8 - half) / 16f, 1.0F, (8 + half) / 16f, (8 + half) / 16f);
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

    // ------------------------------------------------------------------ redstone

    @Override
    public boolean canProvidePower() {
        return kind == Kind.SIGNAL || kind == Kind.INSULATED;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        if (!canProvidePower() || !WireNetwork.isEmitting()) return 0;
        return BlockRedWire.power(world, x, y, z);
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return isProvidingWeakPower(world, x, y, z, side);
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return canProvidePower();
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!world.isRemote && kind != Kind.POWER) WireNetwork.onWireChanged(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote || kind == Kind.POWER) return;
        if (kind != Kind.BUNDLED || neighbor instanceof WireTypes.IWire || BundledIO.active()) WireNetwork.update(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        if (!world.isRemote && kind != Kind.POWER) WireNetwork.onWireRemoved(world, x, y, z, this);
    }

    /** Przewód energetyczny: PPM pustą ręką pokazuje zgromadzoną energię. */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, net.minecraft.entity.player.EntityPlayer player, int side,
                                    float hx, float hy, float hz) {
        if (kind != Kind.POWER || player.getHeldItem() != null) return false;
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof pl.geesio.redcircuit.energy.TilePowerWire t) {
            pl.geesio.redcircuit.energy.EnergyUtil.showEnergy(player, t.buffer().get(), t.buffer().capacity);
        }
        return true;
    }
}
