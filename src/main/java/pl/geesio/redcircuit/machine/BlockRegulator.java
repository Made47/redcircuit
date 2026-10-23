package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.tube.TileTube;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Regulator - tył: źródło, przód: uzupełniany inwentarz. PPM: filtr z ilościami (9 slotów).
 * Co 10 ticków przenosi przedmioty, aż przód zawiera ilości z filtra; wtedy wysyła sygnał redstone.
 * Bit 8 metadanych = uzupełniony.
 */
public class BlockRegulator extends BlockFacing implements TileMachine.IMachineTicker {
    public static final int INTERVAL = 10;

    public BlockRegulator() {
        super("regulator");
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileMachine(9, "container.redcircuit.regulator", false);
    }

    @Override
    public boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube) {
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        // Pracuje stale - nie reaguje na impulsy
    }

    @Override
    protected void transfer(World world, int x, int y, int z) {
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return (world.getBlockMetadata(x, y, z) & POWERED) != 0 ? 15 : 0;
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (!world.isRemote) player.openGui(RedCircuit.instance, GuiHandler.CHEST, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileMachine te = BlockContainerMachine.tile(world, x, y, z);
        if (te != null) InvUtil.dropAll(world, x, y, z, te.items);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void serverTick(World world, int x, int y, int z, TileMachine be) {
        if (++be.timer < INTERVAL) return;
        be.timer = 0;
        int meta = world.getBlockMetadata(x, y, z);
        ForgeDirection facing = facing(meta);
        ForgeDirection back = facing.getOpposite();
        int fx = x + facing.offsetX, fy = y + facing.offsetY, fz = z + facing.offsetZ;
        IInventory src = InvUtil.inventory(world, x + back.offsetX, y + back.offsetY, z + back.offsetZ);
        IInventory dst = world.getTileEntity(fx, fy, fz) instanceof TileTube ? null : InvUtil.inventory(world, fx, fy, fz);
        int srcSide = facing.ordinal(), dstSide = back.ordinal();

        boolean satisfied = dst != null && !be.items.isEmpty();
        if (dst != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack p = be.items.getStackInSlot(i);
                if (p == null) continue;
                int need = p.stackSize - InvUtil.count(dst, dstSide, p);
                if (need > 0 && src != null) {
                    for (int s : InvUtil.slots(src, srcSide)) {
                        if (need <= 0) break;
                        ItemStack peek = InvUtil.peek(src, s, srcSide);
                        if (peek == null || !InvUtil.stackable(p, peek)) continue;
                        ItemStack want = InvUtil.copy(peek, Math.min(need, peek.stackSize));
                        ItemStack rest = InvUtil.insert(dst, want, dstSide, true);
                        int fits = want.stackSize - (rest == null ? 0 : rest.stackSize);
                        if (fits <= 0) continue;
                        ItemStack moved = InvUtil.extract(src, s, fits, srcSide);
                        if (moved == null) continue;
                        ItemStack left = InvUtil.insert(dst, moved, dstSide, false);
                        if (left != null) InvUtil.insert(src, left, srcSide, false);
                        need -= moved.stackSize - (left == null ? 0 : left.stackSize);
                    }
                }
                if (need > 0) satisfied = false;
            }
        }
        if (((meta & POWERED) != 0) != satisfied) {
            world.setBlockMetadataWithNotify(x, y, z, satisfied ? meta | POWERED : meta & ~POWERED, 3);
        }
    }
}
