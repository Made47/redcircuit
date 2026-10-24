package pl.geesio.redcircuit.energy;

import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.util.transfer.IItemHandler;
import pl.geesio.redcircuit.util.transfer.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/** Ładowarka: 4 sloty na przedmioty z energią (baterie, narzędzia elektryczne, przedmioty innych modów). */
public class ChargerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 4, CAPACITY = 50_000, RATE = 1_000;

    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, RATE * SLOTS, 0, this::setChanged);
    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
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

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER.get(), pos, state);
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChargerBlockEntity be) {
        if (level.isClientSide()) return;
        boolean charging = false;
        for (int i = 0; i < SLOTS; i++) {
            if (BatteryHelper.charge(be.items.getStackInSlot(i), be.energy, RATE) > 0) charging = true;
        }
        if (state.getValue(ChargerBlock.LIT) != charging) {
            level.setBlock(pos, state.setValue(ChargerBlock.LIT, charging), 3);
        }
    }

    /** Lejki/rury: wkładanie z góry i boków, od dołu wyjmowanie w pełni naładowanych. */
    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return side == Direction.DOWN ? stack : items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (side != Direction.DOWN && side != null) return ItemStack.EMPTY;
                return BatteryHelper.isFull(items.getStackInSlot(slot)) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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
        return Component.translatable("block.redcircuit.charger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ChargerMenu(id, inventory, items, data, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        items.save(output.child("Items"));
        output.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.load(input.childOrEmpty("Items"));
        energy.setEnergy(input.getIntOr("Energy", 0));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
            for (int i = 0; i < this.getItems().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), this.getItems().getStackInSlot(i));
            }
    }
}
