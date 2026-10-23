package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/**
 * Akumulator: 1 000 000 FE + dwa sloty na baterie:
 * górny ładuje baterię z akumulatora, dolny rozładowuje baterię do akumulatora.
 */
public class BatteryBlockEntity extends EnergyNodeBlockEntity implements MenuProvider {
    public static final int CAPACITY = 1_000_000;
    public static final int CHARGE = 0, DISCHARGE = 1;
    public static final int ITEM_RATE = 1_000;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return BatteryHelper.isBattery(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    /** Energia wysyłana w kawałkach po 15 bitów (pakiet danych menu przesyła tylko short). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            int e = energy.getEnergyStored();
            return i == 0 ? e & 0x7FFF : (e >> 15) & 0x7FFF;
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state, CAPACITY, 1_000);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
        if (level.isClientSide) return;
        BatteryHelper.charge(be.items.getStackInSlot(CHARGE), be.energy, ITEM_RATE);
        BatteryHelper.discharge(be.items.getStackInSlot(DISCHARGE), be.energy, ITEM_RATE);
        EnergyNodeBlockEntity.tick(level, pos, state, be);
    }

    /** Góra: wkładanie do ładowania; boki: do rozładowania; dół: wyjmowanie gotowych baterii. */
    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 2;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (side == Direction.DOWN) return stack;
                int target = side == Direction.UP ? CHARGE : DISCHARGE;
                if (slot != target) return stack;
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (side != Direction.DOWN && side != null) return ItemStack.EMPTY;
                ItemStack s = items.getStackInSlot(slot);
                boolean done = slot == CHARGE ? BatteryHelper.isFull(s) : BatteryHelper.isEmpty(s);
                return done ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return BatteryHelper.isBattery(stack);
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.redcircuit.battery_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BatteryBoxMenu(id, inventory, items, data, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) BatteryHelper.loadItems(items, registries, tag.getCompound("Items"));
    }
}
