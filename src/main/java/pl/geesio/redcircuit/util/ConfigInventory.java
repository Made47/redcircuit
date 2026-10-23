package pl.geesio.redcircuit.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/** Sloty powiązane z blokiem-encją (filtry, konfiguracja maszyn, zawartość maszyn) - odpowiednik MachineContainer. */
public class ConfigInventory implements IInventory {
    private final TileEntity owner;
    private final String name;
    private ItemStack[] items;

    public ConfigInventory(TileEntity owner, int size, String name) {
        this.owner = owner;
        this.items = new ItemStack[size];
        this.name = name;
    }

    /** Zmiana rozmiaru (np. po wczytaniu maszyny z zapisu). */
    public void resize(int size) {
        if (size == items.length) return;
        ItemStack[] n = new ItemStack[size];
        System.arraycopy(items, 0, n, 0, Math.min(size, items.length));
        items = n;
    }

    public boolean isEmpty() {
        for (ItemStack s : items) if (s != null) return false;
        return true;
    }

    @Override
    public int getSizeInventory() {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < items.length ? items[slot] : null;
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
        if (stack != null && stack.stackSize > getInventoryStackLimit()) stack.stackSize = getInventoryStackLimit();
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return name;
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
    public void markDirty() {
        owner.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return owner.getWorldObj() != null && owner.getWorldObj().getTileEntity(owner.xCoord, owner.yCoord, owner.zCoord) == owner
                && player.getDistanceSq(owner.xCoord + 0.5, owner.yCoord + 0.5, owner.zCoord + 0.5) <= 64.0;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    public void save(NBTTagCompound tag, String key) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) continue;
            NBTTagCompound e = new NBTTagCompound();
            e.setByte("Slot", (byte) i);
            items[i].writeToNBT(e);
            list.appendTag(e);
        }
        tag.setTag(key, list);
    }

    public void load(NBTTagCompound tag, String key) {
        java.util.Arrays.fill(items, null);
        NBTTagList list = tag.getTagList(key, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            int slot = e.getByte("Slot") & 255;
            if (slot < items.length) items[slot] = ItemStack.loadItemStackFromNBT(e);
        }
    }
}
