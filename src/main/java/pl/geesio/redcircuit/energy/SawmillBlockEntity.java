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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.alloy.SawingRecipe;
import pl.geesio.redcircuit.alloy.ModRecipes;
import pl.geesio.redcircuit.config.RedCircuitConfig;
import pl.geesio.redcircuit.registry.ModBlockEntities;

import java.util.Optional;

/** Tartak: receptury typu redcircuit:sawing, energia z sieci albo z baterii. */
public class SawmillBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 20_000;
    public static final int INPUT = 0, OUTPUT = 1, BONUS = 2, BATTERY_SLOT = 3;

    private final ItemStackHandler items = new ItemStackHandler(4) {
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
    private int progress, maxProgress = 100;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
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

    public SawmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAWMILL.get(), pos, state);
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

    public int getProgressPercent() {
        return maxProgress <= 0 ? 0 : progress * 100 / maxProgress;
    }

    private Optional<RecipeHolder<SawingRecipe>> recipeFor(Level level, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(ModRecipes.SAWING.get(), new SingleRecipeInput(input), level);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SawmillBlockEntity be) {
        if (level.isClientSide) return;
        BatteryHelper.discharge(be.items.getStackInSlot(BATTERY_SLOT), be.energy, 400);
        int cost = RedCircuitConfig.SAWMILL_FE.get();
        boolean working = false;
        Optional<RecipeHolder<SawingRecipe>> holder = be.recipeFor(level, be.items.getStackInSlot(INPUT));
        if (holder.isPresent()) {
            SawingRecipe recipe = holder.get().value();
            ItemStack result = recipe.result();
            boolean fits = be.items.insertItem(OUTPUT, result.copy(), true).isEmpty()
                    && (recipe.bonus().isEmpty() || be.items.insertItem(BONUS, recipe.bonus().copy(), true).isEmpty());
            be.maxProgress = Math.max(1, recipe.time());
            if (fits && be.energy.getEnergyStored() >= cost) {
                working = true;
                be.energy.setEnergy(be.energy.getEnergyStored() - cost);
                if (++be.progress >= be.maxProgress) {
                    be.progress = 0;
                    be.items.extractItem(INPUT, 1, false);
                    be.items.insertItem(OUTPUT, result.copy(), false);
                    if (!recipe.bonus().isEmpty() && level.random.nextFloat() < recipe.bonusChance()) {
                        be.items.insertItem(BONUS, recipe.bonus().copy(), false);
                    }
                }
                be.setChanged();
            }
        } else if (be.progress != 0) {
            be.progress = 0;
            be.setChanged();
        }
        if (state.getValue(SawmillBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(SawmillBlock.LIT, working), Block.UPDATE_ALL);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.redcircuit.sawmill");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SawmillMenu(id, inventory, items, data, this);
    }

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

    /** Lejki/rury: wkładanie do wejścia (tylko to, co da się zmielić), wyjmowanie z obu wyjść. */
    private class AutomationHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != INPUT || !isItemValid(slot, stack)) return stack;
            return items.insertItem(INPUT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT || slot == BONUS ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT && level != null && recipeFor(level, stack).isPresent();
        }
    }
}
