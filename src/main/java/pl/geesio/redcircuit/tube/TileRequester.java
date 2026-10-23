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
 * Requester: na impuls redstone przeszukuje sieć rur podłączoną z tyłu, znajduje w inwentarzach przedmiot pasujący
 * do filtra i ściąga go rurami do siebie, a potem wypycha go do przodu. Filtr (9 slotów) obsługiwany po kolei;
 * pusty filtr = dowolny przedmiot. Zużywa energię.
 */
public class TileRequester extends TileEntity implements IItemAcceptor, IEnergyReceiver, IGuiInventory {
    public static final int CAPACITY = 10_000;

    public final ConfigInventory filter = new ConfigInventory(this, 9, "container.redcircuit.requester");
    public final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 500, 0, this::markDirty);
    private int nextSlot;

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public IInventory guiInventory() {
        return filter;
    }

    private ForgeDirection facing() {
        return BlockFacing.facing(getBlockMetadata());
    }

    /** Wejście od strony rury (tył): przyjmuje zamówione przedmioty i od razu wypycha je do przodu. */
    @Override
    public ItemStack accept(ItemStack stack, ForgeDirection side, boolean simulate) {
        ForgeDirection facing = facing();
        if (side != facing.getOpposite() || stack == null) return stack;
        int n = BlockFacing.deliverableCount(worldObj, xCoord, yCoord, zCoord, facing, stack, -1);
        if (n <= 0) return stack;
        ItemStack rest = stack.copy();
        ItemStack out = rest.splitStack(Math.min(n, rest.stackSize));
        if (!simulate) BlockFacing.deliver(worldObj, xCoord, yCoord, zCoord, facing, out, -1);
        return rest.stackSize > 0 ? rest : null;
    }

    /** Wykonuje jedno zamówienie. */
    public void retrieve() {
        if (energy.get() < RCConfig.requesterRf) return;
        ForgeDirection back = facing().getOpposite();
        Pos self = new Pos(xCoord, yCoord, zCoord);
        Pos startTube = self.offset(back);
        if (!(worldObj.getTileEntity(startTube.x, startTube.y, startTube.z) instanceof TileTube)) return;

        boolean anyPattern = false;
        for (int i = 0; i < 9; i++) {
            int slot = (nextSlot + i) % 9;
            ItemStack pattern = filter.getStackInSlot(slot);
            if (pattern == null) continue;
            anyPattern = true;
            if (fetch(self, startTube, pattern)) {
                nextSlot = (slot + 1) % 9;
                markDirty();
                return;
            }
        }
        if (!anyPattern) fetch(self, startTube, null);
    }

    private boolean fetch(Pos self, Pos startTube, ItemStack pattern) {
        int max = pattern == null ? 64 : pattern.stackSize;
        int sent = TubeNetwork.fetch(worldObj, self, startTube, Collections.emptySet(),
                s -> pattern == null || InvUtil.stackable(pattern, s), max);
        if (sent <= 0) return false;
        energy.use(RCConfig.requesterRf);
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        filter.save(tag, "Filter");
        energy.save(tag);
        tag.setInteger("Next", nextSlot);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        filter.load(tag, "Filter");
        energy.load(tag);
        nextSlot = tag.getInteger("Next");
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
