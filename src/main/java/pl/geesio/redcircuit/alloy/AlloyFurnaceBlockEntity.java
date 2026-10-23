package pl.geesio.redcircuit.alloy;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.energy.BatteryHelper;
import pl.geesio.redcircuit.energy.ModEnergyStorage;
import pl.geesio.redcircuit.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Piec do stopów: 9 slotów wejścia (0..8), slot paliwa (9) i wynik (10).
 * W wersji elektrycznej slot 9 jest slotem baterii.
 * Wersja elektryczna zamiast paliwa zużywa energię (FE) i pracuje 2x szybciej.
 */
public class AlloyFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUTS = 9, FUEL = 9, OUTPUT = 10;
    public static final int ENERGY_CAPACITY = 20_000, ENERGY_PER_TICK = 25;

    private final boolean electric;

    public final ItemStackHandler items = new ItemStackHandler(11) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != FUEL || !electric || BatteryHelper.isBattery(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == FUEL && electric ? 1 : 64;
        }
    };
    private final ModEnergyStorage energy = new ModEnergyStorage(ENERGY_CAPACITY, 500, 0, this::setChanged);

    private int burnTime, burnDuration, progress, cookTime = 200;
    @Nullable
    private List<ItemStack> lastInputs;
    @Nullable
    private RecipeHolder<AlloyRecipe> cachedRecipe;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> cookTime;
                case 2 -> electric ? energy.getEnergyStored() : burnTime;
                case 3 -> electric ? ENERGY_CAPACITY : burnDuration;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public static AlloyFurnaceBlockEntity fuel(BlockPos pos, BlockState state) {
        return new AlloyFurnaceBlockEntity(ModBlockEntities.ALLOY_FURNACE.get(), pos, state, false);
    }

    public static AlloyFurnaceBlockEntity electric(BlockPos pos, BlockState state) {
        return new AlloyFurnaceBlockEntity(ModBlockEntities.POWER_ALLOY_FURNACE.get(), pos, state, true);
    }

    private AlloyFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean electric) {
        super(type, pos, state);
        this.electric = electric;
    }

    /** Postęp stapiania w procentach (0..100). */
    public int getProgressPercent() {
        return progress * 100 / Math.max(1, cookTime);
    }

    /** Pozostały czas spalania paliwa w tickach (wersja na paliwo). */
    public int getBurnTime() {
        return burnTime;
    }

    public boolean isElectric() {
        return electric;
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    // ------------------------------------------------------------------ logika

    private List<ItemStack> inputs() {
        List<ItemStack> list = new ArrayList<>(INPUTS);
        for (int i = 0; i < INPUTS; i++) list.add(items.getStackInSlot(i));
        return list;
    }

    private Optional<RecipeHolder<AlloyRecipe>> findRecipe(Level level) {
        List<ItemStack> now = inputs();
        if (lastInputs != null && sameStacks(now, lastInputs)) return Optional.ofNullable(cachedRecipe);
        lastInputs = new ArrayList<>();
        for (ItemStack s : now) lastInputs.add(s.copy());
        cachedRecipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.ALLOYING.get(), new AlloyRecipe.Input(now), level).orElse(null);
        return Optional.ofNullable(cachedRecipe);
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        for (int i = 0; i < a.size(); i++) if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        return true;
    }

    private boolean outputFits(ItemStack result) {
        return !result.isEmpty() && items.insertItem(OUTPUT, result.copy(), true).isEmpty();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlloyFurnaceBlockEntity be) {
        if (level.isClientSide) return;
        boolean wasLit = state.getValue(AlloyFurnaceBlock.LIT);
        boolean working = false;

        if (be.burnTime > 0) be.burnTime--;

        Optional<RecipeHolder<AlloyRecipe>> recipe = be.findRecipe(level);
        ItemStack result = recipe.map(r -> r.value().getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
        boolean canWork = recipe.isPresent() && be.outputFits(result);
        if (canWork) be.cookTime = recipe.get().value().cookTime();

        if (be.electric) {
            BatteryHelper.discharge(be.items.getStackInSlot(FUEL), be.energy, 500);
            if (canWork && be.energy.getEnergyStored() >= pl.geesio.redcircuit.config.RedCircuitConfig.POWER_ALLOY_FURNACE_FE.get()) {
                be.energy.setEnergy(be.energy.getEnergyStored() - pl.geesio.redcircuit.config.RedCircuitConfig.POWER_ALLOY_FURNACE_FE.get());
                be.progress += 2;
                working = true;
            }
        } else {
            if (canWork && be.burnTime <= 0) {
                ItemStack fuel = be.items.getStackInSlot(FUEL);
                int burn = pl.geesio.redcircuit.platform.Fuel.burnTime(fuel);
                if (burn > 0) {
                    be.burnTime = be.burnDuration = burn;
                    ItemStack rest = !fuel.getRecipeRemainder().isEmpty() ? fuel.getRecipeRemainder() : ItemStack.EMPTY;
                    ItemStack f = fuel.copy();
                    f.shrink(1);
                    be.items.setStackInSlot(FUEL, f.isEmpty() ? rest : f);
                }
            }
            if (be.burnTime > 0) {
                working = true;
                if (canWork) be.progress++;
            }
        }

        if (!canWork) {
            if (be.progress != 0) be.progress = 0;
        } else if (be.progress >= be.cookTime) {
            be.progress = 0;
            recipe.get().value().consume(be.items, 0, INPUTS);
            be.items.insertItem(OUTPUT, result.copy(), false);
            be.lastInputs = null;
        }

        boolean lit = be.electric ? (working && canWork) : be.burnTime > 0;
        if (lit != wasLit) {
            level.setBlock(pos, state.setValue(AlloyFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        }
        if (working || canWork || be.burnTime > 0) be.setChanged();
    }

    // ------------------------------------------------------------------ automatyzacja

    /** Góra: wejścia; boki: paliwo (lub wejścia w wersji elektrycznej); dół: wyjmowanie wyniku. */
    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 11;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (side == Direction.DOWN || slot == OUTPUT) return stack;
                boolean fuelSide = !electric && side != Direction.UP;
                if (fuelSide) {
                    if (slot != FUEL || pl.geesio.redcircuit.platform.Fuel.burnTime(stack) <= 0) return stack;
                } else if (slot >= INPUTS) {
                    return stack;
                }
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == OUTPUT ? items.extractItem(OUTPUT, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != OUTPUT;
            }
        };
    }

    // ------------------------------------------------------------------ GUI / zapis

    @Override
    public Component getDisplayName() {
        return Component.translatable(electric ? "block.redcircuit.power_alloy_furnace" : "block.redcircuit.alloy_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AlloyFurnaceMenu(id, inventory, items, data, this, electric);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Burn", burnTime);
        tag.putInt("BurnMax", burnDuration);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        energy.setEnergy(tag.getInt("Energy"));
        burnTime = tag.getInt("Burn");
        burnDuration = tag.getInt("BurnMax");
        progress = tag.getInt("Progress");
    }
}
