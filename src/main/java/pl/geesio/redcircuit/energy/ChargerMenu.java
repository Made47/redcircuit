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

public class ChargerMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 35, SLOT_Y = 35;
    private static final int MACHINE_SLOTS = ChargerBlockEntity.SLOTS;

    private final ContainerData data;
    @Nullable
    private final ChargerBlockEntity blockEntity;

    public ChargerMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(ChargerBlockEntity.SLOTS), new SimpleContainerData(2), null);
    }

    public ChargerMenu(int id, Inventory inventory, IItemHandler items, ContainerData data, @Nullable ChargerBlockEntity be) {
        super(ModMenus.CHARGER.get(), id);
        this.data = data;
        this.blockEntity = be;
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            addSlot(new SlotItemHandler(items, i, SLOT_X + i * 18, SLOT_Y) {
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

    public int getEnergy() {
        return (data.get(0) & 0x7FFF) | ((data.get(1) & 0x7FFF) << 15);
    }

    public int getMaxEnergy() {
        return ChargerBlockEntity.CAPACITY;
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
            if (!BatteryHelper.isBattery(stack) || !moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
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
