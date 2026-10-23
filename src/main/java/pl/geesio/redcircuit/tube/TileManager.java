package pl.geesio.redcircuit.tube;

import cofh.api.energy.IEnergyReceiver;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.config.RCConfig;
import pl.geesio.redcircuit.energy.EnergyBuffer;
import pl.geesio.redcircuit.machine.BlockFacing;
import pl.geesio.redcircuit.util.ConfigInventory;
import pl.geesio.redcircuit.util.IGuiInventory;
import pl.geesio.redcircuit.util.IItemAcceptor;
import pl.geesio.redcircuit.util.InvUtil;
import pl.geesio.redcircuit.util.Pos;

import java.util.Collections;

/**
 * Manager: pilnuje zapasów w inwentarzu za sobą. Filtr (9 slotów) = lista przedmiotów i ich ilości.
 * Brakujące przedmioty zamawia z sieci rur (z przodu), a przedmioty spoza listy odsyła do sieci.
 */
public class TileManager extends TileEntity implements IItemAcceptor, IEnergyReceiver, IGuiInventory {
    public static final int CAPACITY = 10_000, INTERVAL = 20, ORDER_TIMEOUT = 5;

    public final ConfigInventory filter = new ConfigInventory(this, 9, "container.redcircuit.manager");
    public final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 500, 0, this::markDirty);
    private int timer, waiting, next;

    @Override
    public IInventory guiInventory() {
        return filter;
    }

    private ForgeDirection facing() {
        return BlockFacing.facing(getBlockMetadata());
    }

    private IInventory managed() {
        ForgeDirection b = facing().getOpposite();
        return InvUtil.inventory(worldObj, xCoord + b.offsetX, yCoord + b.offsetY, zCoord + b.offsetZ);
    }

    private boolean listed(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            ItemStack p = filter.getStackInSlot(i);
            if (p != null && InvUtil.stackable(p, stack)) return true;
        }
        return false;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || ++timer < INTERVAL) return;
        timer = 0;
        IInventory inv = managed();
        if (inv == null || filter.isEmpty()) return;
        ForgeDirection facing = facing();
        int side = facing.ordinal();   // ściana pilnowanego inwentarza zwrócona do managera
        Pos self = new Pos(xCoord, yCoord, zCoord);
        Pos front = self.offset(facing);
        if (!(worldObj.getTileEntity(front.x, front.y, front.z) instanceof TileTube)) return;

        // 1. Przedmioty spoza listy wracają do sieci
        for (int slot : InvUtil.slots(inv, side)) {
            ItemStack s = InvUtil.peek(inv, slot, side);
            if (s == null || listed(s)) continue;
            int n = BlockFacing.deliverableCount(worldObj, xCoord, yCoord, zCoord, facing, s, -1);
            if (n <= 0) break;
            ItemStack got = InvUtil.extract(inv, slot, n, side);
            if (got != null) BlockFacing.deliver(worldObj, xCoord, yCoord, zCoord, facing, got, -1);
            break;
        }

        // 2. Zamawianie brakujących (jedno zamówienie naraz)
        if (waiting > 0) {
            waiting--;
            return;
        }
        if (energy.get() < RCConfig.managerRf) return;
        for (int k = 0; k < 9; k++) {
            int slot = (next + k) % 9;
            ItemStack p = filter.getStackInSlot(slot);
            if (p == null) continue;
            int need = p.stackSize - InvUtil.count(inv, side, p);
            if (need <= 0) continue;
            int sent = TubeNetwork.fetch(worldObj, self, front, Collections.singleton(self.offset(facing.getOpposite())),
                    s -> InvUtil.stackable(p, s), Math.min(need, p.getMaxStackSize()));
            if (sent > 0) {
                energy.use(RCConfig.managerRf);
                waiting = ORDER_TIMEOUT;
                next = (slot + 1) % 9;
                markDirty();
                return;
            }
        }
    }

    /** Wejście od przodu: zamówione przedmioty trafiają do pilnowanego inwentarza. */
    @Override
    public ItemStack accept(ItemStack stack, ForgeDirection side, boolean simulate) {
        ForgeDirection facing = facing();
        if (side != facing || stack == null) return stack;
        IInventory inv = managed();
        if (inv == null) return stack;
        ItemStack rest = InvUtil.insert(inv, stack, facing.ordinal(), simulate);
        if (!simulate && (rest == null || rest.stackSize < stack.stackSize)) waiting = 0;
        return rest;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        filter.save(tag, "Filter");
        energy.save(tag);
        tag.setInteger("Next", next);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        filter.load(tag, "Filter");
        energy.load(tag);
        next = tag.getInteger("Next");
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        return energy.receive(maxReceive, simulate);
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return energy.get();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return CAPACITY;
    }
}
