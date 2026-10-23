package pl.geesio.redcircuit.machine;

import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Rozmieszczacz: 9 slotów; na impuls redstone używa przedmiotu na bloku przed sobą jak gracz PPM
 * (stawia bloki, sypie mączkę kostną, napełnia wiadra, strzyże...). Sloty po kolei.
 */
public class BlockDeployer extends BlockContainerMachine {
    public BlockDeployer() {
        super("deployer", Material.rock, 1);
    }

    @Override
    protected void pulse(World world, int x, int y, int z, TileMachine te) {
        if (!(world instanceof WorldServer ws)) return;
        ForgeDirection facing = facing(world, x, y, z);
        int fx = x + facing.offsetX, fy = y + facing.offsetY, fz = z + facing.offsetZ;
        FakePlayer player = FakePlayerFactory.getMinecraft(ws);
        float yaw = switch (facing) {
            case SOUTH -> 0F;
            case WEST -> 90F;
            case NORTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        float pitch = facing == ForgeDirection.UP ? -90F : facing == ForgeDirection.DOWN ? 90F : 0F;
        player.setLocationAndAngles(x + 0.5, y + 0.5 - player.getEyeHeight(), z + 0.5, yaw, pitch);
        player.rotationYawHead = yaw;

        int n = te.items.getSizeInventory();
        for (int k = 0; k < n; k++) {
            int slot = (te.next + k) % n;
            ItemStack stack = te.items.getStackInSlot(slot);
            if (stack == null) continue;
            player.inventory.clearInventory(null, -1);
            player.inventory.currentItem = 0;
            player.inventory.setInventorySlotContents(0, stack);
            boolean done = use(ws, player, fx, fy, fz, facing, stack);
            ItemStack after = player.inventory.getStackInSlot(0);
            te.items.setInventorySlotContents(slot, after != null && after.stackSize > 0 ? after : null);
            player.inventory.setInventorySlotContents(0, null);
            // To, co trafiło do ekwipunku sztucznego gracza (np. pełne wiadro), wraca do maszyny
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack extra = player.inventory.getStackInSlotOnClosing(i);
                if (extra == null) continue;
                ItemStack rest = MachineUtil.insert(te.items, extra);
                if (rest != null) {
                    ForgeDirection b = facing.getOpposite();
                    InvUtil.spawn(world, x + b.offsetX, y + b.offsetY, z + b.offsetZ, rest, null);
                }
            }
            if (done) {
                te.next = (slot + 1) % n;
                te.markDirty();
                return;
            }
        }
    }

    private static boolean use(WorldServer world, FakePlayer player, int x, int y, int z, ForgeDirection facing, ItemStack stack) {
        int side = facing.getOpposite().ordinal();
        float hx = 0.5F - facing.offsetX * 0.5F, hy = 0.5F - facing.offsetY * 0.5F, hz = 0.5F - facing.offsetZ * 0.5F;
        if (player.theItemInWorldManager.activateBlockOrUseItem(player, world, stack, x, y, z, side, hx, hy, hz)) return true;
        ItemStack held = player.inventory.getStackInSlot(0);
        if (held == null) return false;
        int before = held.stackSize;
        ItemStack result = held.useItemRightClick(world, player);
        if (result != held || result.stackSize != before) {
            player.inventory.setInventorySlotContents(0, result);
            return true;
        }
        return false;
    }
}
