package pl.geesio.redcircuit.energy;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemStackHandler;
import pl.geesio.redcircuit.platform.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModMenus;

public class FuelGeneratorMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 80, SLOT_Y = 53;

    private final ContainerData data;
    @Nullable
    private final FuelGeneratorBlockEntity blockEntity;

    public FuelGeneratorMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(1), new SimpleContainerData(5), null);
    }

    public FuelGeneratorMenu(int id, Inventory inventory, IItemHandler items, ContainerData data, @Nullable FuelGeneratorBlockEntity be) {
        super(ModMenus.SOLID_FUEL_GENERATOR.get(), id);
        this.data = data;
        this.blockEntity = be;
        addSlot(new SlotItemHandler(items, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FuelGeneratorBlockEntity.isFuel(stack, inventory.player.level());
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

    public int getEnergy() {
        return (data.get(0) & 0x7FFF) | ((data.get(1) & 0x7FFF) << 15);
    }

    public int getMaxEnergy() {
        return FuelGeneratorBlockEntity.CAPACITY;
    }

    /** Postęp spalania 0..1 (1 = świeże paliwo). */
    public float getBurnFraction() {
        int total = data.get(3);
        return total <= 0 ? 0f : Math.min(1f, data.get(2) / (float) total);
    }

    public int getRate() {
        return data.get(4);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!FuelGeneratorBlockEntity.isFuel(stack, player.level()) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
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
