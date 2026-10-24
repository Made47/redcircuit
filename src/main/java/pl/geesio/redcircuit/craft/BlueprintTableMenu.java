package pl.geesio.redcircuit.craft;

import pl.geesio.redcircuit.util.Recipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import pl.geesio.redcircuit.util.transfer.IItemHandler;
import pl.geesio.redcircuit.util.transfer.ItemStackHandler;
import pl.geesio.redcircuit.util.transfer.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModItems;
import pl.geesio.redcircuit.registry.ModMenus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlueprintTableMenu extends AbstractContainerMenu {
    // Indeksy slotów
    public static final int GRID_START = 0, RESULT = 9, PLAN = 10, STORAGE_START = 11, STORAGE_END = 29;
    public static final int INV_START = 29, INV_END = 65;

    public static final int BUTTON_PLAN = 0;

    private final IItemHandler grid;
    private final IItemHandler storage;
    private final IItemHandler plan;
    private final ResultContainer result = new ResultContainer();
    private final Player player;
    @Nullable
    private final BlueprintTableBlockEntity blockEntity;
    private List<ItemStack> lastGrid = List.of();

    /** Konstruktor po stronie klienta. */
    public BlueprintTableMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public BlueprintTableMenu(int id, Inventory inventory, @Nullable BlueprintTableBlockEntity be) {
        super(ModMenus.BLUEPRINT_TABLE.get(), id);
        this.blockEntity = be;
        this.player = inventory.player;
        this.grid = be != null ? be.grid : new ItemStackHandler(9);
        this.storage = be != null ? be.storage : new ItemStackHandler(18);
        this.plan = be != null ? be.plan : new ItemStackHandler(1);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(grid, col + row * 3, 48 + col * 18, 18 + row * 18));
            }
        }
        addSlot(new ResultSlot(result, 0, 142, 36));
        addSlot(new SlotItemHandler(plan, 0, 17, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.BLANK_PLAN.get()) || stack.is(ModItems.PLAN.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(storage, col + row * 9, 8 + col * 18, 90 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }
        updateResult(true);
    }

    // ------------------------------------------------------------------ receptura

    private List<ItemStack> gridItems() {
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) items.add(grid.getStackInSlot(i).copy());
        return items;
    }

    private Optional<RecipeHolder<CraftingRecipe>> findRecipe(Level level, CraftingInput input) {
        return Recipes.find(level, RecipeType.CRAFTING, input);
    }

    /** Przelicza wynik (tylko na serwerze; slot wyniku synchronizuje się sam). */
    private void updateResult(boolean force) {
        Level level = player.level();
        if (level.isClientSide()) return;
        List<ItemStack> now = gridItems();
        if (!force && sameItems(now, lastGrid)) return;
        lastGrid = now;
        CraftingInput input = CraftingInput.of(3, 3, now);
        ItemStack out = findRecipe(level, input)
                .map(r -> r.value().assemble(input))
                .orElse(ItemStack.EMPTY);
        result.setItem(0, out);
    }

    private static boolean sameItems(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    @Override
    public void broadcastChanges() {
        updateResult(false);
        super.broadcastChanges();
    }

    /** Zużywa składniki po wytworzeniu: najpierw z magazynu, potem z siatki. */
    private void consumeIngredients() {
        Level level = player.level();
        List<ItemStack> items = gridItems();
        CraftingInput input = CraftingInput.of(3, 3, items);
        Optional<RecipeHolder<CraftingRecipe>> recipe = findRecipe(level, input);
        NonNullList<ItemStack> remaining = recipe.isPresent()
                ? recipe.get().value().getRemainingItems(input)
                : NonNullList.withSize(9, ItemStack.EMPTY);

        // CraftingInput może być przycięty do mniejszego rozmiaru - zużywamy po prostu każdy niepusty slot siatki.
        for (int i = 0; i < 9; i++) {
            ItemStack wanted = grid.getStackInSlot(i);
            if (wanted.isEmpty()) continue;
            boolean fromStorage = false;
            for (int s = 0; s < storage.getSlots(); s++) {
                ItemStack st = storage.getStackInSlot(s);
                if (!st.isEmpty() && ItemStack.isSameItemSameComponents(st, wanted)) {
                    storage.extractItem(s, 1, false);
                    fromStorage = true;
                    break;
                }
            }
            if (!fromStorage) grid.extractItem(i, 1, false);
        }
        for (ItemStack rest : remaining) {
            if (rest.isEmpty()) continue;
            ItemStack left = rest.copy();
            for (int s = 0; s < storage.getSlots() && !left.isEmpty(); s++) {
                left = storage.insertItem(s, left, false);
            }
            if (!left.isEmpty() && !player.getInventory().add(left)) player.drop(left, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    private class ResultSlot extends Slot {
        ResultSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player p, ItemStack stack) {
            stack.onCraftedBy(p, stack.getCount());
            if (!p.level().isClientSide()) {
                consumeIngredients();
                updateResult(true);
            }
            super.onTake(p, stack);
        }
    }

    // ------------------------------------------------------------------ plan

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id != BUTTON_PLAN || p.level().isClientSide()) return id == BUTTON_PLAN;
        ItemStack planStack = plan.getStackInSlot(0);
        if (planStack.is(ModItems.BLANK_PLAN.get())) {
            ItemStack out = result.getItem(0);
            if (out.isEmpty()) return false;
            if (plan instanceof ItemStackHandler h) {
                h.setStackInSlot(0, PlanItem.create(new ItemStack(ModItems.PLAN.get()), gridItems(), out));
            }
            return true;
        }
        if (planStack.is(ModItems.PLAN.get())) {
            // Wczytaj plan: uzupełnij puste pola siatki przedmiotami z magazynu.
            NonNullList<ItemStack> data = PlanItem.read(planStack);
            for (int i = 0; i < 9; i++) {
                ItemStack wanted = data.get(i);
                if (wanted.isEmpty() || !grid.getStackInSlot(i).isEmpty()) continue;
                for (int s = 0; s < storage.getSlots(); s++) {
                    ItemStack st = storage.getStackInSlot(s);
                    if (!st.isEmpty() && ItemStack.isSameItemSameComponents(st, wanted)) {
                        grid.insertItem(i, storage.extractItem(s, 1, false), false);
                        break;
                    }
                }
            }
            updateResult(true);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ shift-klik

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        if (index == RESULT) {
            // Wytwarzaj wielokrotnie, dopóki są składniki (max 64 razy).
            ItemStack first = slot.getItem().copy();
            for (int n = 0; n < 64; n++) {
                ItemStack out = slot.getItem();
                if (out.isEmpty() || !ItemStack.isSameItemSameComponents(out, first)) break;
                ItemStack moving = out.copy();
                if (!moveItemStackTo(moving, INV_START, INV_END, true)) break;
                slot.onTake(p, out.copy());
                if (!moving.isEmpty()) p.drop(moving, false, net.minecraft.util.Prediction.PREDICTED);
                result.setItem(0, ItemStack.EMPTY);
                updateResult(true);
            }
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (stack.is(ModItems.BLANK_PLAN.get()) || stack.is(ModItems.PLAN.get())) {
                if (!moveItemStackTo(stack, PLAN, PLAN + 1, false)
                        && !moveItemStackTo(stack, STORAGE_START, STORAGE_END, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, STORAGE_START, STORAGE_END, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return blockEntity == null || Container.stillValidBlockEntity(blockEntity, p);
    }
}
