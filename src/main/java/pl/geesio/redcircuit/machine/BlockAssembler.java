package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Montażownica: 16 slotów sterowanych kablem wiązkowym. Włączenie kanału N stawia przed maszyną
 * blok ze slotu N, wyłączenie - zbiera ten blok z powrotem do slotu N.
 * Zwykły impuls redstone: stawia blok z kolejnego slotu albo zbiera blok stojący przed maszyną.
 */
public class BlockAssembler extends BlockContainerMachine {
    public BlockAssembler() {
        super("assembler", Material.iron, 2);
    }

    @Override
    protected boolean ticks() {
        return true;
    }

    @Override
    protected void serverTick(World world, int x, int y, int z, TileMachine te) {
        int mask = MachineUtil.bundledInput(world, x, y, z);
        int changed = mask ^ te.lastChannels;
        if (changed == 0) return;
        te.lastChannels = mask;
        te.markDirty();
        for (int ch = 0; ch < 16; ch++) {
            if ((changed & (1 << ch)) == 0) continue;
            if ((mask & (1 << ch)) != 0) place(world, x, y, z, te, ch);
            else collect(world, x, y, z, te, ch);
        }
    }

    @Override
    protected void pulse(World world, int x, int y, int z, TileMachine te) {
        ForgeDirection f = facing(world, x, y, z);
        int fx = x + f.offsetX, fy = y + f.offsetY, fz = z + f.offsetZ;
        if (world.getBlock(fx, fy, fz).isReplaceable(world, fx, fy, fz)) {
            for (int k = 0; k < 16; k++) {
                int slot = (te.next + k) % 16;
                if (place(world, x, y, z, te, slot)) {
                    te.next = (slot + 1) % 16;
                    return;
                }
            }
        } else {
            collect(world, x, y, z, te, -1);
        }
    }

    private static boolean place(World world, int x, int y, int z, TileMachine te, int slot) {
        if (!(world instanceof WorldServer ws)) return false;
        ForgeDirection f = facing(world, x, y, z);
        int fx = x + f.offsetX, fy = y + f.offsetY, fz = z + f.offsetZ;
        ItemStack stack = te.items.getStackInSlot(slot);
        if (stack == null || !(stack.getItem() instanceof ItemBlock ib)) return false;
        Block target = Block.getBlockFromItem(ib);
        if (!world.getBlock(fx, fy, fz).isReplaceable(world, fx, fy, fz) || !target.canPlaceBlockAt(world, fx, fy, fz)) return false;
        int side = f.getOpposite().ordinal();
        int meta = ib.getMetadata(stack.getItemDamage());
        meta = target.onBlockPlaced(world, fx, fy, fz, side, 0.5F, 0.5F, 0.5F, meta);
        if (!ib.placeBlockAt(stack, FakePlayerFactory.getMinecraft(ws), world, fx, fy, fz, side, 0.5F, 0.5F, 0.5F, meta)) return false;
        world.playSoundEffect(fx + 0.5, fy + 0.5, fz + 0.5, target.stepSound.getBreakSound(),
                (target.stepSound.getVolume() + 1.0F) / 2.0F, target.stepSound.getPitch() * 0.8F);
        if (--stack.stackSize <= 0) te.items.setInventorySlotContents(slot, null);
        te.markDirty();
        return true;
    }

    /** Zbiera blok sprzed maszyny; najpierw do slotu {@code slot} (jeśli >= 0), potem do pozostałych. */
    private static void collect(World world, int x, int y, int z, TileMachine te, int slot) {
        ForgeDirection f = facing(world, x, y, z);
        for (ItemStack drop : MachineUtil.breakBlock(world, x + f.offsetX, y + f.offsetY, z + f.offsetZ)) {
            if (slot >= 0) {
                ItemStack cur = te.items.getStackInSlot(slot);
                if (cur == null) {
                    te.items.setInventorySlotContents(slot, drop);
                    continue;
                }
                if (InvUtil.stackable(cur, drop) && cur.stackSize + drop.stackSize <= cur.getMaxStackSize()) {
                    cur.stackSize += drop.stackSize;
                    te.markDirty();
                    continue;
                }
            }
            ItemStack rest = MachineUtil.insert(te.items, drop);
            if (rest != null) {
                ForgeDirection b = f.getOpposite();
                InvUtil.spawn(world, x + b.offsetX, y + b.offsetY, z + b.offsetZ, rest, null);
            }
        }
    }
}
