package pl.geesio.redcircuit.alloy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;

import java.util.Arrays;

/**
 * Piec do stopów: 9 slotów składników, wynik, paliwo. Pali dowolne paliwo pieca.
 * Lejki: z góry składniki, z boków paliwo, z dołu wynik (i puste wiadra po lawie).
 */
public class TileAlloyFurnace extends TileEntity implements ISidedInventory, cofh.api.energy.IEnergyReceiver {
    public static final int GRID = 9, OUTPUT = 9, FUEL = 10, SIZE = 11;
    private static final int[] SLOTS_TOP = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] SLOTS_BOTTOM = {OUTPUT, FUEL};
    private static final int[] SLOTS_SIDE = {FUEL};

    private final ItemStack[] items = new ItemStack[SIZE];
    public int burnTime, burnTotal, progress, cookTime = 200;

    /** Elektryczny piec do stopów: zamiast paliwa energia (slot paliwa = slot baterii), 2x szybszy. */
    public boolean electric;
    public static final int ENERGY_CAPACITY = 20_000;
    public final pl.geesio.redcircuit.energy.EnergyBuffer energy =
            new pl.geesio.redcircuit.energy.EnergyBuffer(ENERGY_CAPACITY, 500, 0, this::markDirty);
    /** Energia widziana w oknie (klient). */
    public int clientEnergy;

    public TileAlloyFurnace() {
    }

    public TileAlloyFurnace(boolean electric) {
        this.electric = electric;
    }

    private void electricTick() {
        pl.geesio.redcircuit.energy.BatteryHelper.discharge(items[FUEL], energy, 500);
        ItemStack[] grid = Arrays.copyOf(items, GRID);
        AlloyRecipe recipe = AlloyRecipes.find(grid);
        boolean canWork = recipe != null && fits(recipe.result());
        boolean working = false;
        if (canWork) {
            cookTime = recipe.cookTime;
            if (energy.use(pl.geesio.redcircuit.config.RCConfig.powerAlloyFurnaceRf)) {
                working = true;
                progress += 2;
                if (progress >= cookTime) {
                    progress = 0;
                    craft(recipe, grid);
                }
                markDirty();
            }
        } else if (progress != 0) {
            progress = 0;
            markDirty();
        }
        BlockAlloyFurnace.setLit(worldObj, xCoord, yCoord, zCoord, working);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        if (electric) {
            electricTick();
            return;
        }
        boolean wasBurning = burnTime > 0;
        boolean dirty = false;
        if (burnTime > 0) burnTime--;

        ItemStack[] grid = Arrays.copyOf(items, GRID);
        AlloyRecipe recipe = AlloyRecipes.find(grid);
        boolean canWork = recipe != null && fits(recipe.result());

        if (burnTime == 0 && canWork) {
            ItemStack fuel = items[FUEL];
            int t = TileEntityFurnace.getItemBurnTime(fuel);
            if (t > 0) {
                burnTime = burnTotal = t;
                ItemStack container = fuel.getItem().getContainerItem(fuel);
                fuel.stackSize--;
                if (fuel.stackSize <= 0) items[FUEL] = container;
                dirty = true;
            }
        }

        if (burnTime > 0 && canWork) {
            cookTime = recipe.cookTime;
            if (++progress >= cookTime) {
                progress = 0;
                craft(recipe, grid);
                dirty = true;
            }
        } else if (progress > 0) {
            progress = canWork ? Math.max(0, progress - 2) : 0;
        }

        boolean burning = burnTime > 0;
        if (wasBurning != burning) {
            dirty = true;
            BlockAlloyFurnace.setLit(worldObj, xCoord, yCoord, zCoord, burning);
        }
        if (dirty) markDirty();
    }

    private boolean fits(ItemStack result) {
        ItemStack out = items[OUTPUT];
        if (out == null) return true;
        if (!out.isItemEqual(result) || !ItemStack.areItemStackTagsEqual(out, result)) return false;
        return out.stackSize + result.stackSize <= Math.min(out.getMaxStackSize(), getInventoryStackLimit());
    }

    private void craft(AlloyRecipe recipe, ItemStack[] grid) {
        int[] take = recipe.allocate(grid);
        if (take == null) return;
        for (int i = 0; i < GRID; i++) {
            if (take[i] > 0) decrStackSize(i, take[i]);
        }
        ItemStack result = recipe.result();
        if (items[OUTPUT] == null) items[OUTPUT] = result;
        else items[OUTPUT].stackSize += result.stackSize;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    // ------------------------------------------------------------------ zapis

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        Arrays.fill(items, null);
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            int slot = t.getByte("Slot") & 255;
            if (slot < SIZE) items[slot] = ItemStack.loadItemStackFromNBT(t);
        }
        burnTime = tag.getInteger("BurnTime");
        burnTotal = tag.getInteger("BurnTotal");
        progress = tag.getInteger("Progress");
        cookTime = Math.max(1, tag.getInteger("CookTime"));
        electric = tag.getBoolean("Electric");
        energy.load(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SIZE; i++) {
            if (items[i] == null) continue;
            NBTTagCompound t = new NBTTagCompound();
            t.setByte("Slot", (byte) i);
            items[i].writeToNBT(t);
            list.appendTag(t);
        }
        tag.setTag("Items", list);
        tag.setInteger("BurnTime", burnTime);
        tag.setInteger("BurnTotal", burnTotal);
        tag.setInteger("Progress", progress);
        tag.setInteger("CookTime", cookTime);
        tag.setBoolean("Electric", electric);
        energy.save(tag);
    }

    // ------------------------------------------------------------------ IInventory

    @Override
    public int getSizeInventory() {
        return SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack s = items[slot];
        if (s == null) return null;
        if (s.stackSize <= amount) {
            items[slot] = null;
            markDirty();
            return s;
        }
        ItemStack split = s.splitStack(amount);
        markDirty();
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        items[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) stack.stackSize = getInventoryStackLimit();
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return electric ? "tile.redcircuit.power_alloy_furnace.name" : "container.redcircuit.alloy_furnace";
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

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == OUTPUT) return false;
        if (slot == FUEL) return electric ? pl.geesio.redcircuit.energy.BatteryHelper.isBattery(stack) : TileEntityFurnace.isItemFuel(stack);
        return true;
    }

    // ------------------------------------------------------------------ ISidedInventory (lejki)

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? SLOTS_BOTTOM : side == 1 ? SLOTS_TOP : SLOTS_SIDE;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        if (slot == FUEL && electric) return pl.geesio.redcircuit.energy.BatteryHelper.isEmpty(stack);
        return slot == OUTPUT || (slot == FUEL && stack.getItem() == Items.bucket);
    }

    // ------------------------------------------------------------------ RF (tylko wersja elektryczna)

    @Override
    public boolean canConnectEnergy(net.minecraftforge.common.util.ForgeDirection from) {
        return electric;
    }

    @Override
    public int receiveEnergy(net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate) {
        return electric ? energy.receive(maxReceive, simulate) : 0;
    }

    @Override
    public int getEnergyStored(net.minecraftforge.common.util.ForgeDirection from) {
        return energy.get();
    }

    @Override
    public int getMaxEnergyStored(net.minecraftforge.common.util.ForgeDirection from) {
        return ENERGY_CAPACITY;
    }
}
