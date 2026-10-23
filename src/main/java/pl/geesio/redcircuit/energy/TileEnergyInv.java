package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Arrays;

/**
 * Wspólna baza maszyn energii z ekwipunkiem i oknem: sloty, magazyn RF, pola synchronizowane z oknem
 * (0 = energia, 1 = pojemność, 2 = postęp, 3 = czas, 4 = spalanie, 5 = czas spalania).
 */
public abstract class TileEnergyInv extends TileEntity implements ISidedInventory, IEnergyHandler {
    public static final int F_ENERGY = 0, F_CAPACITY = 1, F_PROGRESS = 2, F_MAX_PROGRESS = 3, F_BURN = 4, F_BURN_TOTAL = 5,
            FIELDS = 6;

    protected final ItemStack[] items;
    protected final EnergyBuffer energy;
    private final String title;
    /** Wartości pól po stronie klienta (z okna). */
    public final int[] clientFields = new int[FIELDS];

    protected TileEnergyInv(int slots, int capacity, int maxReceive, int maxExtract, String title) {
        items = new ItemStack[slots];
        energy = new EnergyBuffer(capacity, maxReceive, maxExtract, this::markDirty);
        this.title = title;
    }

    public EnergyBuffer energy() {
        return energy;
    }

    /** Układ okna. */
    public abstract GuiLayout layout();

    /** Wartość pola dla okna (serwer). */
    public int field(int i) {
        return switch (i) {
            case F_ENERGY -> energy.get();
            case F_CAPACITY -> energy.capacity;
            default -> 0;
        };
    }

    /** Pole widziane w oknie (klient: z synchronizacji). */
    public int shown(int i) {
        return worldObj != null && worldObj.isRemote ? clientFields[i] : field(i);
    }

    /** Wyrzucenie zawartości przy niszczeniu. */
    public ItemStack[] contents() {
        return items;
    }

    // ------------------------------------------------------------------ zapis

    protected void writeExtra(NBTTagCompound tag) {
    }

    protected void readExtra(NBTTagCompound tag) {
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) continue;
            NBTTagCompound t = new NBTTagCompound();
            t.setByte("Slot", (byte) i);
            items[i].writeToNBT(t);
            list.appendTag(t);
        }
        tag.setTag("Items", list);
        energy.save(tag);
        writeExtra(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        Arrays.fill(items, null);
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            int slot = t.getByte("Slot") & 255;
            if (slot < items.length) items[slot] = ItemStack.loadItemStackFromNBT(t);
        }
        energy.load(tag);
        readExtra(tag);
    }

    // ------------------------------------------------------------------ RF

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        return energy.receive(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        return energy.extract(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return energy.get();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return energy.capacity;
    }

    // ------------------------------------------------------------------ IInventory

    @Override
    public int getSizeInventory() {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        ItemStack s = items[slot];
        if (s == null) return null;
        if (s.stackSize <= count) {
            items[slot] = null;
            markDirty();
            return s;
        }
        ItemStack out = s.splitStack(count);
        markDirty();
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack s = items[slot];
        items[slot] = null;
        return s;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        items[slot] = stack != null && stack.stackSize <= 0 ? null : stack;
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return title;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    /** Domyślnie: sloty baterii przyjmują tylko baterie, wyjścia nic. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        GuiLayout.SlotDef d = layout().def(slot);
        if (d == null) return true;
        return switch (d.kind) {
            case OUTPUT -> false;
            case BATTERY -> BatteryHelper.isBattery(stack);
            default -> true;
        };
    }

    // ------------------------------------------------------------------ ISidedInventory (lejki, rury): domyślnie wejścia
    // z góry i boków, wyjścia od dołu

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int[] r = new int[items.length];
        for (int i = 0; i < r.length; i++) r[i] = i;
        return r;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        GuiLayout.SlotDef d = layout().def(slot);
        return side != 0 && (d == null || d.kind == GuiLayout.Kind.NORMAL) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        GuiLayout.SlotDef d = layout().def(slot);
        return d != null && d.kind == GuiLayout.Kind.OUTPUT;
    }

    // ------------------------------------------------------------------ pomocnicze

    /** Czy stos zmieści się w slocie (bez zmian). */
    protected boolean fits(int slot, ItemStack result) {
        if (result == null) return true;
        ItemStack out = items[slot];
        if (out == null) return true;
        if (!out.isItemEqual(result) || !ItemStack.areItemStackTagsEqual(out, result)) return false;
        return out.stackSize + result.stackSize <= Math.min(out.getMaxStackSize(), getInventoryStackLimit());
    }

    protected void addTo(int slot, ItemStack result) {
        if (result == null) return;
        if (items[slot] == null) items[slot] = result.copy();
        else items[slot].stackSize += result.stackSize;
        markDirty();
    }

    /** Zmiana bitu "pracuje" w metadanych (bit 3) - światło i tekstura przodu. */
    protected void setLit(boolean lit) {
        BlockEnergyMachine.setLit(worldObj, xCoord, yCoord, zCoord, lit);
    }
}
