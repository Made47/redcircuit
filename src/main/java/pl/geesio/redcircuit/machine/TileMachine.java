package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import pl.geesio.redcircuit.util.ConfigInventory;
import pl.geesio.redcircuit.util.IGuiInventory;

/**
 * Wspólny blok-encja maszyn z inwentarzem (Relay, Ejector, Deployer, Assembler, Buffer) oraz filtrów i maszyny
 * sortującej (wtedy sloty to wzorce, niedostępne dla rur). Logika jest w klasach bloków.
 */
public class TileMachine extends TileEntity implements ISidedInventory, IGuiInventory {
    public final ConfigInventory items = new ConfigInventory(this, 9, "container.redcircuit.machine");
    private String title = "container.redcircuit.machine";
    /** Czy rury i lejki mają dostęp do slotów (false = sloty konfiguracyjne). */
    private boolean automation = true;
    /** Licznik ticków (np. co ile Relay wypycha przedmioty). */
    public int timer;
    /** Kolejny slot do użycia (kolejka po slotach). */
    public int next;
    /** Ostatni stan 16 kanałów kabla wiązkowego (Assembler). */
    public int lastChannels;

    public TileMachine() {
    }

    public TileMachine(int size, String title, boolean automation) {
        items.resize(size);
        this.title = title;
        this.automation = automation;
    }

    @Override
    public IInventory guiInventory() {
        return this;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        Block b = getBlockType();
        if (b instanceof BlockContainerMachine m && m.ticks()) m.serverTick(worldObj, xCoord, yCoord, zCoord, this);
        else if (b instanceof IMachineTicker t) t.serverTick(worldObj, xCoord, yCoord, zCoord, this);
    }

    /** Bloki, które tykają przez TileMachine (Regulator). */
    public interface IMachineTicker {
        void serverTick(net.minecraft.world.World world, int x, int y, int z, TileMachine te);
    }

    // ------------------------------------------------------------------ zapis

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Size", items.getSizeInventory());
        tag.setString("Title", title);
        tag.setBoolean("Automation", automation);
        items.save(tag, "Items");
        tag.setInteger("Timer", timer);
        tag.setInteger("Next", next);
        tag.setInteger("Channels", lastChannels);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Size")) items.resize(tag.getInteger("Size"));
        if (tag.hasKey("Title")) title = tag.getString("Title");
        if (tag.hasKey("Automation")) automation = tag.getBoolean("Automation");
        items.load(tag, "Items");
        timer = tag.getInteger("Timer");
        next = tag.getInteger("Next");
        lastChannels = tag.getInteger("Channels");
    }

    // ------------------------------------------------------------------ inwentarz (ze wszystkich stron poza przodem)

    private static final int[] NONE = new int[0];

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (!automation) return NONE;
        Block b = getBlockType();
        if (b instanceof BlockFacing && side == (getBlockMetadata() & 7)) return NONE;
        int[] r = new int[items.getSizeInventory()];
        for (int i = 0; i < r.length; i++) r[i] = i;
        return r;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return automation;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return automation;
    }

    @Override
    public int getSizeInventory() {
        return items.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        return items.decrStackSize(slot, count);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return items.getStackInSlotOnClosing(slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        items.setInventorySlotContents(slot, stack);
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
        return items.isUseableByPlayer(player);
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
}
