package pl.geesio.redcircuit.energy;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModMenus;

public class ElectricFurnaceMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 3;
    private static final int TOTAL_SLOTS = MACHINE_SLOTS + 36;

    private final ContainerData data;
    @Nullable
    private final ElectricFurnaceBlockEntity blockEntity;

    /** Konstruktor po stronie klienta. */
    public ElectricFurnaceMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(3), new SimpleContainerData(4), null);
    }

    public ElectricFurnaceMenu(int id, Inventory inventory, IItemHandler items, ContainerData data,
                               @Nullable ElectricFurnaceBlockEntity blockEntity) {
        super(ModMenus.ELECTRIC_FURNACE.get(), id);
        this.data = data;
        this.blockEntity = blockEntity;

        addSlot(new SlotItemHandler(items, 0, 56, 17));
        addSlot(new SlotItemHandler(items, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(items, ElectricFurnaceBlockEntity.BATTERY_SLOT, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BatteryHelper.isBattery(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

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

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getEnergy() {
        return data.get(2);
    }

    public int getMaxEnergy() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, TOTAL_SLOTS, true)) return ItemStack.EMPTY;
        } else {
            int target = BatteryHelper.isBattery(stack) ? ElectricFurnaceBlockEntity.BATTERY_SLOT : 0;
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || Container.stillValidBlockEntity(blockEntity, player);
    }
}
