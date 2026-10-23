package pl.geesio.redcircuit.alloy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

/** Okno pieca do stopów: siatka 3x3, paliwo, wynik + ekwipunek gracza. */
public class ContainerAlloyFurnace extends Container {
    public static final int GRID_X = 30, GRID_Y = 17, FUEL_X = 8, FUEL_Y = 53, OUT_X = 124, OUT_Y = 35;
    private static final int TE_SLOTS = TileAlloyFurnace.SIZE;

    private final TileAlloyFurnace furnace;
    private int lastBurn, lastBurnTotal, lastProgress, lastCook;

    public ContainerAlloyFurnace(InventoryPlayer inv, TileAlloyFurnace furnace) {
        this.furnace = furnace;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlotToContainer(new Slot(furnace, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addSlotToContainer(new Slot(furnace, TileAlloyFurnace.OUTPUT, OUT_X, OUT_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
        addSlotToContainer(new Slot(furnace, TileAlloyFurnace.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return furnace.isItemValidForSlot(TileAlloyFurnace.FUEL, stack);
            }

            @Override
            public int getSlotStackLimit() {
                return furnace.electric ? 1 : 64;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inv, col, 8 + col * 18, 142));
    }

    public TileAlloyFurnace furnace() {
        return furnace;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return furnace.isUseableByPlayer(player);
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        crafter.sendProgressBarUpdate(this, 0, v0());
        crafter.sendProgressBarUpdate(this, 1, v1());
        crafter.sendProgressBarUpdate(this, 2, furnace.progress);
        crafter.sendProgressBarUpdate(this, 3, furnace.cookTime);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (Object o : crafters) {
            ICrafting c = (ICrafting) o;
            if (lastBurn != v0()) c.sendProgressBarUpdate(this, 0, v0());
            if (lastBurnTotal != v1()) c.sendProgressBarUpdate(this, 1, v1());
            if (lastProgress != furnace.progress) c.sendProgressBarUpdate(this, 2, furnace.progress);
            if (lastCook != furnace.cookTime) c.sendProgressBarUpdate(this, 3, furnace.cookTime);
        }
        lastBurn = v0();
        lastBurnTotal = v1();
        lastProgress = furnace.progress;
        lastCook = furnace.cookTime;
    }

    /** Pola 0-1: paliwo (zwykły piec) albo energia w dwóch 15-bitowych częściach (elektryczny). */
    private int v0() {
        return furnace.electric ? furnace.energy.get() & 0x7FFF : furnace.burnTime;
    }

    private int v1() {
        return furnace.electric ? (furnace.energy.get() >> 15) & 0x7FFF : furnace.burnTotal;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        if (furnace.electric && id < 2) {
            int e = furnace.clientEnergy;
            furnace.clientEnergy = id == 0 ? (e & ~0x7FFF) | (value & 0x7FFF) : (e & 0x7FFF) | ((value & 0x7FFF) << 15);
            return;
        }
        switch (id) {
            case 0 -> furnace.burnTime = value;
            case 1 -> furnace.burnTotal = value;
            case 2 -> furnace.progress = value;
            case 3 -> furnace.cookTime = value;
            default -> { }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        int playerStart = TE_SLOTS, playerEnd = TE_SLOTS + 36;
        if (index < TE_SLOTS) {
            if (!mergeItemStack(stack, playerStart, playerEnd, true)) return null;
            slot.onSlotChange(stack, copy);
        } else if (furnace.isItemValidForSlot(TileAlloyFurnace.FUEL, stack) && !furnace.electric
                && mergeItemStack(stack, TileAlloyFurnace.FUEL, TileAlloyFurnace.FUEL + 1, false)) {
            // paliwo trafiło do slotu paliwa
        } else if (furnace.electric && furnace.isItemValidForSlot(TileAlloyFurnace.FUEL, stack)
                && !((Slot) inventorySlots.get(TileAlloyFurnace.FUEL)).getHasStack()) {
            ((Slot) inventorySlots.get(TileAlloyFurnace.FUEL)).putStack(stack.splitStack(1));
            // paliwo trafiło do slotu paliwa
        } else if (!mergeItemStack(stack, 0, TileAlloyFurnace.GRID, false)) {
            if (index < playerStart + 27) {
                if (!mergeItemStack(stack, playerStart + 27, playerEnd, false)) return null;
            } else if (!mergeItemStack(stack, playerStart, playerStart + 27, false)) {
                return null;
            }
        }
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        if (stack.stackSize == copy.stackSize) return null;
        slot.onPickupFromSlot(player, stack);
        return copy;
    }
}
