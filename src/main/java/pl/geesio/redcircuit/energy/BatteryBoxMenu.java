package pl.geesio.redcircuit.energy;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import pl.geesio.redcircuit.util.transfer.IItemHandler;
import pl.geesio.redcircuit.util.transfer.ItemStackHandler;
import pl.geesio.redcircuit.util.transfer.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModMenus;

public class BatteryBoxMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 44, CHARGE_Y = 17, DISCHARGE_Y = 53;
    private static final int MACHINE_SLOTS = 2;

    private final ContainerData data;
    @Nullable
    private final BatteryBlockEntity blockEntity;

    public BatteryBoxMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(2), new SimpleContainerData(2), null);
    }

    public BatteryBoxMenu(int id, Inventory inventory, IItemHandler items, ContainerData data,
                          @Nullable BatteryBlockEntity be) {
        super(ModMenus.BATTERY_BOX.get(), id);
        this.data = data;
        this.blockEntity = be;

        addSlot(batterySlot(items, BatteryBlockEntity.CHARGE, SLOT_X, CHARGE_Y));
        addSlot(batterySlot(items, BatteryBlockEntity.DISCHARGE, SLOT_X, DISCHARGE_Y));

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

    private static Slot batterySlot(IItemHandler items, int index, int x, int y) {
        return new SlotItemHandler(items, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BatteryHelper.isBattery(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
    }

    public int getEnergy() {
        return (data.get(0) & 0x7FFF) | ((data.get(1) & 0x7FFF) << 15);
    }

    public int getMaxEnergy() {
        return BatteryBlockEntity.CAPACITY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!BatteryHelper.isBattery(stack)) return ItemStack.EMPTY;
            // Pełna bateria -> rozładowanie, pozostałe -> ładowanie
            int first = BatteryHelper.isFull(stack) ? BatteryBlockEntity.DISCHARGE : BatteryBlockEntity.CHARGE;
            if (!moveItemStackTo(stack, first, first + 1, false)
                    && !moveItemStackTo(stack, 1 - first, 2 - first, false)) return ItemStack.EMPTY;
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
