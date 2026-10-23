package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;

import java.util.Optional;

/** Piec elektryczny: przetapia jak zwykły piec, ale 2x szybciej i na Azurity. */
public class ElectricFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 20_000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int COOK_TIME = 100;
    public static final int BATTERY_SLOT = 2;

    /** 0 = wejście, 1 = wynik, 2 = bateria. */
    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != BATTERY_SLOT || BatteryHelper.isBattery(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == BATTERY_SLOT ? 1 : 64;
        }
    };
    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 400, 0, this::setChanged);
    private final IItemHandler automation = new AutomationHandler();
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> COOK_TIME;
                case 2 -> energy.getEnergyStored();
                case 3 -> CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state);
    }

    /** Postęp przetapiania w procentach (0..100). */
    public int getProgressPercent() {
        return progress * 100 / COOK_TIME;
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    private Optional<RecipeHolder<SmeltingRecipe>> recipeFor(Level level, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity be) {
        if (level.isClientSide) return;
        BatteryHelper.discharge(be.items.getStackInSlot(BATTERY_SLOT), be.energy, 400);
        boolean working = false;
        ItemStack input = be.items.getStackInSlot(0);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = be.recipeFor(level, input);
        if (recipe.isPresent()) {
            ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
            boolean fits = !result.isEmpty() && be.items.insertItem(1, result.copy(), true).isEmpty();
            if (fits && be.energy.getEnergyStored() >= pl.geesio.redcircuit.config.RedCircuitConfig.ELECTRIC_FURNACE_FE.get()) {
                working = true;
                be.energy.setEnergy(be.energy.getEnergyStored() - pl.geesio.redcircuit.config.RedCircuitConfig.ELECTRIC_FURNACE_FE.get());
                be.progress++;
                if (be.progress >= COOK_TIME) {
                    be.progress = 0;
                    be.items.extractItem(0, 1, false);
                    be.items.insertItem(1, result.copy(), false);
                }
                be.setChanged();
            }
        } else if (be.progress != 0) {
            be.progress = 0;
            be.setChanged();
        }
        if (state.getValue(ElectricFurnaceBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(ElectricFurnaceBlock.LIT, working), Block.UPDATE_ALL);
        }
    }

    // ---------- GUI ----------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.redcircuit.electric_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ElectricFurnaceMenu(id, inventory, items, data, this);
    }

    // ---------- Zapis ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        BatteryHelper.loadItems(items, registries, tag.getCompound("Items"));
        energy.setEnergy(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
    }

    /** Dla lejków/rur: wkładanie tylko do wejścia (i tylko przetapialnych), wyjmowanie tylko z wyjścia. */
    private class AutomationHandler implements IItemHandler {
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
            if (slot != 0 || !isItemValid(slot, stack)) return stack;
            return items.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? items.extractItem(1, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && level != null && recipeFor(level, stack).isPresent();
        }
    }
}
