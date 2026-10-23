package pl.geesio.redcircuit.craft;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;
import pl.geesio.redcircuit.item.ModItems;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Okno stołu kreślarskiego: siatka 3x3 (trwała), wynik, slot planu, 18 slotów magazynu, ekwipunek gracza.
 * Składniki są zużywane najpierw z magazynu. Przycisk (enchantItem, id 0) zapisuje plan albo wczytuje go do siatki.
 */
public class ContainerBlueprintTable extends Container {
    public static final int GRID_START = 0, RESULT = 9, PLAN = 10, STORAGE_START = 11, STORAGE_END = 29;
    public static final int INV_START = 29, INV_END = 65;
    public static final int BUTTON_PLAN = 0;

    private final TileBlueprintTable te;
    private final EntityPlayer player;
    private final InventoryCraftResult result = new InventoryCraftResult();
    private ItemStack[] lastGrid = new ItemStack[9];

    public ContainerBlueprintTable(InventoryPlayer inv, TileBlueprintTable te) {
        this.te = te;
        this.player = inv.player;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlotToContainer(new Slot(te.grid, col + row * 3, 48 + col * 18, 18 + row * 18));
            }
        }
        addSlotToContainer(new ResultSlot(result, 0, 142, 36));
        addSlotToContainer(new Slot(te.plan, 0, 17, 36) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack != null && stack.getItem() instanceof ItemPlan;
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(te.storage, col + row * 9, 8 + col * 18, 90 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inv, col, 8 + col * 18, 198));
        updateResult(true);
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return te.grid.isUseableByPlayer(p);
    }

    // ------------------------------------------------------------------ receptura

    private ItemStack[] gridItems() {
        ItemStack[] items = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack s = te.grid.getStackInSlot(i);
            items[i] = s == null ? null : s.copy();
        }
        return items;
    }

    private InventoryCrafting craftingOf(ItemStack[] items) {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer p) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < 9; i++) inv.setInventorySlotContents(i, items[i]);
        return inv;
    }

    private ItemStack findResult(World world, ItemStack[] items) {
        return CraftingManager.getInstance().findMatchingRecipe(craftingOf(items), world);
    }

    /** Przelicza wynik (tylko serwer; slot wyniku synchronizuje się sam). */
    private void updateResult(boolean force) {
        World world = player.worldObj;
        if (world.isRemote) return;
        ItemStack[] now = gridItems();
        if (!force && same(now, lastGrid)) return;
        lastGrid = now;
        result.setInventorySlotContents(0, findResult(world, now));
    }

    private static boolean same(ItemStack[] a, ItemStack[] b) {
        for (int i = 0; i < 9; i++) if (!ItemStack.areItemStacksEqual(a[i], b[i])) return false;
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        updateResult(false);
        super.detectAndSendChanges();
    }

    /** Zużywa składniki po wytworzeniu: najpierw z magazynu, potem z siatki; pojemniki (wiadra, piły) wracają. */
    private void consumeIngredients() {
        for (int i = 0; i < 9; i++) {
            ItemStack wanted = te.grid.getStackInSlot(i);
            if (wanted == null) continue;
            ItemStack used = null;
            boolean fromGrid = true;
            for (int s = 0; s < te.storage.getSizeInventory(); s++) {
                ItemStack st = te.storage.getStackInSlot(s);
                if (st != null && InvUtil.stackable(st, wanted)) {
                    used = te.storage.decrStackSize(s, 1);
                    fromGrid = false;
                    break;
                }
            }
            if (used == null) used = te.grid.decrStackSize(i, 1);
            if (used == null || !used.getItem().hasContainerItem(used)) continue;
            ItemStack container = used.getItem().getContainerItem(used);
            if (container == null) continue;
            if (container.isItemStackDamageable() && container.getItemDamage() > container.getMaxDamage()) continue;
            if (fromGrid && te.grid.getStackInSlot(i) == null) {
                te.grid.setInventorySlotContents(i, container);   // narzędzie / pojemnik zostaje w siatce
                continue;
            }
            ItemStack left = InvUtil.insert(te.storage, container, -1, false);
            if (left != null && !player.inventory.addItemStackToInventory(left)) player.dropPlayerItemWithRandomChoice(left, false);
        }
    }

    private class ResultSlot extends Slot {
        ResultSlot(InventoryCraftResult inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public void onPickupFromSlot(EntityPlayer p, ItemStack stack) {
            stack.onCrafting(p.worldObj, p, stack.stackSize);
            FMLCommonHandler.instance().firePlayerCraftingEvent(p, stack, craftingOf(gridItems()));
            if (!p.worldObj.isRemote) {
                consumeIngredients();
                updateResult(true);
            }
            super.onPickupFromSlot(p, stack);
        }
    }

    // ------------------------------------------------------------------ plan

    @Override
    public boolean enchantItem(EntityPlayer p, int id) {
        if (id != BUTTON_PLAN || p.worldObj.isRemote) return id == BUTTON_PLAN;
        ItemStack planStack = te.plan.getStackInSlot(0);
        if (planStack == null || !(planStack.getItem() instanceof ItemPlan item)) return false;
        if (item.isBlank()) {
            ItemStack out = result.getStackInSlot(0);
            if (out == null) return false;
            te.plan.setInventorySlotContents(0, ItemPlan.create(ModItems.plan, gridItems(), out));
            return true;
        }
        // Wczytaj plan: uzupełnij puste pola siatki przedmiotami z magazynu
        ItemStack[] data = ItemPlan.read(planStack);
        for (int i = 0; i < 9; i++) {
            ItemStack wanted = data[i];
            if (wanted == null || te.grid.getStackInSlot(i) != null) continue;
            for (int s = 0; s < te.storage.getSizeInventory(); s++) {
                ItemStack st = te.storage.getStackInSlot(s);
                if (st != null && InvUtil.stackable(st, wanted)) {
                    te.grid.setInventorySlotContents(i, te.storage.decrStackSize(s, 1));
                    break;
                }
            }
        }
        updateResult(true);
        return true;
    }

    // ------------------------------------------------------------------ shift-klik

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;

        if (index == RESULT) {
            // Wytwarzaj wielokrotnie, dopóki są składniki (max 64 razy)
            ItemStack first = slot.getStack().copy();
            for (int n = 0; n < 64; n++) {
                ItemStack out = slot.getStack();
                if (out == null || !InvUtil.stackable(out, first)) break;
                ItemStack moving = out.copy();
                if (!mergeItemStack(moving, INV_START, INV_END, true)) break;
                slot.onPickupFromSlot(p, out.copy());
                if (moving.stackSize > 0) p.dropPlayerItemWithRandomChoice(moving, false);
                result.setInventorySlotContents(0, null);
                updateResult(true);
            }
            return null;
        }

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!mergeItemStack(stack, INV_START, INV_END, true)) return null;
        } else if (stack.getItem() instanceof ItemPlan) {
            Slot plan = (Slot) inventorySlots.get(PLAN);
            if (!plan.getHasStack()) plan.putStack(stack.splitStack(1));
            else if (!mergeItemStack(stack, STORAGE_START, STORAGE_END, false)) return null;
        } else if (!mergeItemStack(stack, STORAGE_START, STORAGE_END, false)) {
            return null;
        }
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        if (stack.stackSize == copy.stackSize) return null;
        slot.onPickupFromSlot(p, stack);
        return copy;
    }
}
