package pl.geesio.redcircuit.wire;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.block.ItemBlockMeta;

/**
 * Przedmiot przewodu przyklejanego do ścian: stawia przewód na klikniętej ścianie. Gdy w tym miejscu leży już
 * przewód tego samego rodzaju, dokłada mu kolejną ścianę. W ekwipunku płaska ikona (tekstura przedmiotu).
 */
public class ItemFaceWire extends ItemBlockMeta {
    private IIcon[] icons;

    public ItemFaceWire(Block block) {
        super(block);
    }

    private BlockFaceWire wire() {
        return (BlockFaceWire) Block.getBlockFromItem(this);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ) {
        if (stack.stackSize <= 0) return false;
        BlockFaceWire block = wire();
        int meta = stack.getItemDamage() & 15;
        ForgeDirection clicked = ForgeDirection.getOrientation(side);

        // Kliknięcie w istniejący przewód tego samego rodzaju: dołóż ścianę po przeciwnej stronie klikniętej
        if (world.getBlock(x, y, z) == block && (world.getBlockMetadata(x, y, z) & 15) == meta
                && tryAdd(world, x, y, z, clicked.getOpposite(), player, stack, block)) {
            return true;
        }

        PartWires.Placer placer = PartWires.placer;
        // ForgeMultipart: kliknięcie wewnątrz bloku wieloczęściowego (np. w osłonę) - przewód w tym samym bloku
        if (placer != null && depth(side, hitX, hitY, hitZ) < 1 && placer.accepts(world, x, y, z)
                && player.canPlayerEdit(x, y, z, side, stack)
                && placer.place(world, x, y, z, clicked.getOpposite(), block, meta)) {
            if (!world.isRemote) placed(world, x, y, z, player, stack, block);
            return true;
        }

        int px = x + clicked.offsetX, py = y + clicked.offsetY, pz = z + clicked.offsetZ;
        ForgeDirection attach = clicked.getOpposite();   // ściana podłoża, do której przyklejamy przewód
        if (!player.canPlayerEdit(px, py, pz, side, stack)) return false;
        Block existing = world.getBlock(px, py, pz);
        if (existing == block && (world.getBlockMetadata(px, py, pz) & 15) == meta) {
            return tryAdd(world, px, py, pz, attach, player, stack, block);
        }
        if (placer != null && !existing.isReplaceable(world, px, py, pz) && placer.accepts(world, px, py, pz)) {
            if (!placer.place(world, px, py, pz, attach, block, meta)) return false;
            if (!world.isRemote) placed(world, px, py, pz, player, stack, block);
            return true;
        }
        if (!existing.isReplaceable(world, px, py, pz) || !BlockFaceWire.canAttach(world, px, py, pz, attach)) return false;
        if (world.isRemote) return true;
        if (!world.setBlock(px, py, pz, block, meta, 3)) return false;
        TileWire t = BlockFaceWire.tile(world, px, py, pz);
        if (t != null) {
            t.faces = 1 << attach.ordinal();
            t.markDirty();
        }
        world.markBlockForUpdate(px, py, pz);
        block.onWireChanged(world, px, py, pz);
        placed(world, px, py, pz, player, stack, block);
        return true;
    }

    /**
     * Klient: ItemBlock domyślnie nie wysyła kliknięcia, gdy blok obok klikniętej ściany nie jest zastępowalny
     * (np. leży tam już przewód albo blok ForgeMultipart). Wtedy nie dało się dołożyć drugiej ściany ani drugiego
     * rodzaju przewodu do tego samego bloku - o wszystkim decyduje {@link #onItemUse}.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean func_150936_a(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
        return true;
    }

    /** Głębokość trafienia liczona od przeciwnej ściany bloku: 1 = zewnętrzna ściana, mniej = wnętrze bloku. */
    private static double depth(int side, float hx, float hy, float hz) {
        return switch (side) {
            case 0 -> 1 - hy;
            case 1 -> hy;
            case 2 -> 1 - hz;
            case 3 -> hz;
            case 4 -> 1 - hx;
            default -> hx;
        };
    }

    private boolean tryAdd(World world, int x, int y, int z, ForgeDirection face, EntityPlayer player, ItemStack stack, BlockFaceWire block) {
        TileWire t = BlockFaceWire.tile(world, x, y, z);
        if (t == null || t.hasFace(face.ordinal()) || !BlockFaceWire.canAttach(world, x, y, z, face)) return false;
        if (world.isRemote) return true;
        t.faces |= 1 << face.ordinal();
        t.markDirty();
        world.markBlockForUpdate(x, y, z);
        world.notifyBlocksOfNeighborChange(x, y, z, block);
        block.onWireChanged(world, x, y, z);
        placed(world, x, y, z, player, stack, block);
        return true;
    }

    private static void placed(World world, int x, int y, int z, EntityPlayer player, ItemStack stack, Block block) {
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound(),
                (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
    }

    // ------------------------------------------------------------------ ikona

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 1;   // atlas przedmiotów
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        Block b = Block.getBlockFromItem(this);
        if (b instanceof BlockRedWire red && red.isInsulated()) {
            icons = new IIcon[16];
            for (int i = 0; i < 16; i++) icons[i] = register.registerIcon(RedCircuit.MODID + ":insulated_wire_" + BlockRedWire.COLORS[i]);
        } else if (b instanceof pl.geesio.redcircuit.energy.BlockPowerWire) {
            icons = new IIcon[]{register.registerIcon(RedCircuit.MODID + ":power_alloy_wire")};
        } else if (b instanceof BlockRedWire) {
            icons = new IIcon[]{register.registerIcon(RedCircuit.MODID + ":signal_alloy_wire")};
        } else {
            icons = new IIcon[]{register.registerIcon(RedCircuit.MODID + ":bundled_cable")};
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icons[icons.length == 1 ? 0 : damage & 15];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 0xFFFFFF;
    }
}
