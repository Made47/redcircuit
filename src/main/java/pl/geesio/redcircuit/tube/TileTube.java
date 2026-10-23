package pl.geesio.redcircuit.tube;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.IItemAcceptor;
import pl.geesio.redcircuit.util.InvUtil;
import pl.geesio.redcircuit.util.Pos;

import java.util.ArrayList;
import java.util.List;

/**
 * Rura: przedmioty w drodze i kolor farby. Przyjmuje przedmioty od maszyn RedCircuit (IItemAcceptor)
 * oraz od lejków i rur innych modów (ISidedInventory z jednym "wirtualnym" slotem).
 */
public class TileTube extends TileEntity implements IItemAcceptor, ISidedInventory {
    /** Prędkość: 1/8 bloku na tick. */
    public static final float SPEED = 0.125F;
    public static final int MAX_ITEMS = 32;
    /** Spadek prędkości przyspieszonego przedmiotu na każdym bloku rury. */
    public static final float SPEED_DECAY = 0.02F;
    public static final int NO_PAINT = 16;

    private final List<TubeItem> items = new ArrayList<>();
    public int paint = NO_PAINT;
    /** Ściana, przez którą wkłada lejek (z canInsertItem, przed setInventorySlotContents). */
    private int hopperSide = 1;

    /** Wywoływane, gdy przedmiot wjeżdża do tej rury (Akcelerator przyspiesza). */
    protected void onItemEnter(TubeItem item) {
    }

    public List<TubeItem> getItems() {
        return items;
    }

    public static boolean passable(int paint, int itemColor) {
        return paint == NO_PAINT || paint == itemColor;
    }

    public boolean canAccept(int color) {
        return items.size() < MAX_ITEMS && passable(paint, color);
    }

    /** Wkłada przedmiot do rury od strony {@code from}. */
    public void insert(ItemStack stack, ForgeDirection from, int color) {
        if (stack == null || stack.stackSize <= 0) return;
        TubeItem item = new TubeItem(stack, from, color);
        onItemEnter(item);
        items.add(item);
        sync();
    }

    /** Wkłada przedmiot zamówiony przez Requester - pojedzie do {@code target}. */
    public void insertTargeted(ItemStack stack, ForgeDirection from, Pos target) {
        if (stack == null || stack.stackSize <= 0) return;
        TubeItem item = new TubeItem(stack, from, -1);
        item.target = target;
        onItemEnter(item);
        items.add(item);
        sync();
    }

    @Override
    public ItemStack accept(ItemStack stack, ForgeDirection side, boolean simulate) {
        if (!canAccept(-1)) return stack;
        if (!simulate) insert(stack.copy(), side, -1);
        return null;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) clientTick();
        else serverTick();
    }

    private void clientTick() {
        for (TubeItem item : items) {
            float limit = item.to == null ? 0.5F : 1.0F;
            item.progress = Math.min(limit, item.progress + item.speed);
        }
    }

    private void serverTick() {
        if (items.isEmpty()) return;
        boolean dirty = false;
        for (TubeItem item : new ArrayList<>(items)) {
            item.progress += item.speed;

            if (item.to == null && item.progress >= 0.5F) {
                item.to = TubeRouter.route(worldObj, new Pos(xCoord, yCoord, zCoord), item);
                dirty = true;
                if (item.to == null) {
                    // Nie ma dokąd iść - wyrzuć przedmiot
                    items.remove(item);
                    InvUtil.spawn(worldObj, xCoord, yCoord, zCoord, item.stack, null);
                    continue;
                }
            }

            if (item.progress >= 1.0F && item.to != null) {
                items.remove(item);
                dirty = true;
                if (!pass(item)) {
                    // Odbij i wyznacz trasę ponownie
                    item.from = item.to;
                    item.to = null;
                    item.progress = 0F;
                    items.add(item);
                }
            }
        }
        if (dirty) sync();
    }

    /** Próbuje przekazać przedmiot dalej. Zwraca true, jeśli przedmiot opuścił rurę. */
    private boolean pass(TubeItem item) {
        ForgeDirection out = item.to;
        int nx = xCoord + out.offsetX, ny = yCoord + out.offsetY, nz = zCoord + out.offsetZ;
        World w = worldObj;
        if (!w.blockExists(nx, ny, nz)) return false;

        TileEntity te = w.getTileEntity(nx, ny, nz);
        if (te instanceof TileTube next) {
            if (!next.canAccept(item.color)) return false;
            item.from = out.getOpposite();
            item.to = null;
            item.progress = 0F;
            item.speed = Math.max(SPEED, item.speed - SPEED_DECAY);
            next.onItemEnter(item);
            next.items.add(item);
            next.sync();
            return true;
        }

        if (InvUtil.isItemBlock(w, nx, ny, nz)) {
            ItemStack rest = InvUtil.insert(w, nx, ny, nz, item.stack, out.getOpposite(), false);
            if (rest == null) return true;
            item.stack = rest;
            return false;
        }

        // Otwarty koniec rury - wyrzuć przedmiot na zewnątrz
        if (InvUtil.open(w, nx, ny, nz)) {
            InvUtil.spawn(w, nx, ny, nz, item.stack, out);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ sync / zapis

    public void sync() {
        markDirty();
        if (worldObj != null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    private void writeItems(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (TubeItem item : items) if (item.stack != null) list.appendTag(item.save());
        tag.setTag("Items", list);
        tag.setByte("Paint", (byte) paint);
    }

    private void readItems(NBTTagCompound tag) {
        items.clear();
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            TubeItem item = TubeItem.load(list.getCompoundTagAt(i));
            if (item != null) items.add(item);
        }
        paint = tag.hasKey("Paint") ? tag.getByte("Paint") : NO_PAINT;
        if (paint < 0 || paint > NO_PAINT) paint = NO_PAINT;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeItems(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readItems(tag);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeItems(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        int oldPaint = paint;
        readItems(pkt.func_148857_g());
        if (oldPaint != paint) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    }

    // ------------------------------------------------------------------ lejki i inne mody (jeden wirtualny slot)

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        hopperSide = side;
        return canAccept(-1);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return false;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (stack != null && stack.stackSize > 0 && worldObj != null && !worldObj.isRemote) {
            insert(stack.copy(), ForgeDirection.getOrientation(hopperSide), -1);
        }
    }

    @Override
    public String getInventoryName() {
        return "tile.redcircuit.tube.name";
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
        return false;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return canAccept(-1);
    }
}
