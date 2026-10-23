package pl.geesio.redcircuit.energy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Okno maszyny energii: sloty z {@link GuiLayout} + ekwipunek gracza; pola wysyłane w dwóch 15-bitowych częściach. */
public class ContainerEnergyMachine extends Container {
    private final TileEnergyInv te;
    private final int machineSlots;
    private final int[] last = new int[TileEnergyInv.FIELDS];

    public ContainerEnergyMachine(InventoryPlayer inv, TileEnergyInv te) {
        this.te = te;
        GuiLayout layout = te.layout();
        for (GuiLayout.SlotDef d : layout.slots) {
            addSlotToContainer(new Slot(te, d.index, d.x, d.y) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return d.kind != GuiLayout.Kind.OUTPUT && te.isItemValidForSlot(d.index, stack);
                }

                @Override
                public int getSlotStackLimit() {
                    return d.kind == GuiLayout.Kind.BATTERY ? 1 : super.getSlotStackLimit();
                }
            });
        }
        machineSlots = layout.slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inv, col, 8 + col * 18, 142));
        java.util.Arrays.fill(last, -1);
    }

    public TileEnergyInv tile() {
        return te;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return te.isUseableByPlayer(player);
    }

    private void send(ICrafting c, int i, int v) {
        c.sendProgressBarUpdate(this, i * 2, v & 0x7FFF);
        c.sendProgressBarUpdate(this, i * 2 + 1, (v >> 15) & 0x7FFF);
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        for (int i = 0; i < TileEnergyInv.FIELDS; i++) send(crafter, i, te.field(i));
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int i = 0; i < TileEnergyInv.FIELDS; i++) {
            int v = te.field(i);
            if (v == last[i]) continue;
            for (Object o : crafters) send((ICrafting) o, i, v);
            last[i] = v;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        int i = id / 2;
        if (i >= TileEnergyInv.FIELDS) return;
        int cur = te.clientFields[i];
        te.clientFields[i] = (id & 1) == 0 ? (cur & ~0x7FFF) | (value & 0x7FFF) : (cur & 0x7FFF) | ((value & 0x7FFF) << 15);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        int playerStart = machineSlots, playerEnd = machineSlots + 36;
        if (index < machineSlots) {
            if (!mergeItemStack(stack, playerStart, playerEnd, true)) return null;
            slot.onSlotChange(stack, copy);
        } else {
            boolean moved = false;
            // Najpierw sloty baterii (po jednej sztuce), potem zwykłe wejścia
            for (int pass = 0; pass < 2 && !moved; pass++) {
                for (int i = 0; i < machineSlots && stack.stackSize > 0; i++) {
                    Slot s = (Slot) inventorySlots.get(i);
                    GuiLayout.SlotDef d = te.layout().slots.get(i);
                    if ((pass == 0) != (d.kind == GuiLayout.Kind.BATTERY) || !s.isItemValid(stack)) continue;
                    if (d.kind == GuiLayout.Kind.BATTERY) {
                        if (s.getHasStack()) continue;
                        s.putStack(stack.splitStack(1));
                        moved = true;
                        break;
                    }
                    if (mergeItemStack(stack, i, i + 1, false)) moved = true;
                }
            }
            if (!moved) {
                if (index < playerStart + 27) {
                    if (!mergeItemStack(stack, playerStart + 27, playerEnd, false)) return null;
                } else if (!mergeItemStack(stack, playerStart, playerStart + 27, false)) {
                    return null;
                }
            }
        }
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        if (stack.stackSize == copy.stackSize) return null;
        slot.onPickupFromSlot(player, stack);
        return copy;
    }
}
