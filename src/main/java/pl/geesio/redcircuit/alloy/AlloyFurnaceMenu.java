package pl.geesio.redcircuit.alloy;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import pl.geesio.redcircuit.util.transfer.IItemHandler;
import pl.geesio.redcircuit.util.transfer.ItemStackHandler;
import pl.geesio.redcircuit.util.transfer.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.energy.BatteryHelper;
import pl.geesio.redcircuit.registry.ModMenus;

public class AlloyFurnaceMenu extends AbstractContainerMenu {
    public static final int GRID_X = 30, GRID_Y = 17, FUEL_X = 8, FUEL_Y = 53, OUT_X = 124, OUT_Y = 35;

    private final ContainerData data;
    private final boolean electric;
    private final int machineSlots;
    @Nullable
    private final AlloyFurnaceBlockEntity blockEntity;

    public static AlloyFurnaceMenu clientFuel(int id, Inventory inv) {
        return new AlloyFurnaceMenu(id, inv, new ItemStackHandler(11), new SimpleContainerData(4), null, false);
    }

    public static AlloyFurnaceMenu clientElectric(int id, Inventory inv) {
        return new AlloyFurnaceMenu(id, inv, new ItemStackHandler(11), new SimpleContainerData(4), null, true);
    }

    public AlloyFurnaceMenu(int id, Inventory inventory, IItemHandler items, ContainerData data,
                            @Nullable AlloyFurnaceBlockEntity be, boolean electric) {
        super(type(electric), id);
        this.data = data;
        this.electric = electric;
        this.blockEntity = be;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(items, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addSlot(new SlotItemHandler(items, AlloyFurnaceBlockEntity.OUTPUT, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        if (!electric) {
            addSlot(new SlotItemHandler(items, AlloyFurnaceBlockEntity.FUEL, FUEL_X, FUEL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return pl.geesio.redcircuit.util.Fuel.isFuel(stack);
                }
            });
        } else {
            // Slot baterii (w miejscu slotu paliwa)
            addSlot(new SlotItemHandler(items, AlloyFurnaceBlockEntity.FUEL, FUEL_X, FUEL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return BatteryHelper.isBattery(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        machineSlots = slots.size();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    private static MenuType<?> type(boolean electric) {
        return electric ? ModMenus.POWER_ALLOY_FURNACE.get() : ModMenus.ALLOY_FURNACE.get();
    }

    public boolean isElectric() {
        return electric;
    }

    public int progress() { return data.get(0); }
    public int cookTime() { return data.get(1); }
    /** Paliwo: pozostały czas spalania; wersja elektryczna: energia. */
    public int power() { return data.get(2); }
    public int powerMax() { return data.get(3); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int invStart = machineSlots, invEnd = slots.size();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (electric ? BatteryHelper.isBattery(stack) : pl.geesio.redcircuit.util.Fuel.isFuel(stack)) {
                moved = moveItemStackTo(stack, 10, 11, false);
            }
            if (!moved && !moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || Container.stillValidBlockEntity(blockEntity, player);
    }
}
