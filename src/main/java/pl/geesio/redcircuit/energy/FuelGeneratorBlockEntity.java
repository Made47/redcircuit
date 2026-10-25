package pl.geesio.redcircuit.energy;

import pl.geesio.redcircuit.util.Recipes;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.config.RedCircuitConfig;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/**
 * Generator na paliwo stałe. Czas spalania jak w piecu (węgiel 1600 ticków), produkcja z konfiguracji
 * (domyślnie 20 FE/t, czyli 32 000 FE z jednego węgla). Bufor 20 000 FE, oddaje do 400 FE/t na stronę.
 */
public class FuelGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 20_000, OUTPUT = 400;

    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, OUTPUT, this::setChanged);
    private int burnTime, burnTotal, lastRate;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level != null && isFuel(stack, level);
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            int e = energy.getEnergyStored();
            return switch (i) {
                case 0 -> e & 0x7FFF;
                case 1 -> (e >> 15) & 0x7FFF;
                case 2 -> Math.min(burnTime, 0x7FFF);
                case 3 -> Math.min(burnTotal, 0x7FFF);
                default -> lastRate;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLID_FUEL_GENERATOR.get(), pos, state);
    }

    public static boolean isFuel(ItemStack stack, net.minecraft.world.level.Level level) {
        return pl.geesio.redcircuit.platform.Fuel.burnTime(stack, level) > 0;
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public int getLastRate() {
        return lastRate;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FuelGeneratorBlockEntity be) {
        if (level.isClientSide()) return;
        int rate = RedCircuitConfig.SOLID_FUEL_GENERATOR_FE.get();
        boolean powered = level.hasNeighborSignal(pos);

        // Nowe paliwo tylko, gdy jest miejsce na energię i brak sygnału redstone
        if (be.burnTime <= 0 && !powered && be.energy.getEnergyStored() + rate <= CAPACITY) {
            ItemStack fuel = be.items.getStackInSlot(0);
            int time = pl.geesio.redcircuit.platform.Fuel.burnTime(fuel, level);
            if (time > 0) {
                be.burnTime = be.burnTotal = time;
                ItemStack remainder = pl.geesio.redcircuit.util.Recipes.remainder(fuel);   // np. pusty kubeł po lawie
                fuel.shrink(1);
                if (fuel.isEmpty() && !remainder.isEmpty()) be.items.setStackInSlot(0, remainder);
                else be.items.setStackInSlot(0, fuel);
                be.setChanged();
            }
        }

        be.lastRate = 0;
        if (be.burnTime > 0 && be.energy.getEnergyStored() < CAPACITY) {
            be.burnTime--;
            be.lastRate = be.energy.addInternal(rate);
            be.setChanged();
        }

        boolean lit = be.burnTime > 0 && be.lastRate > 0;
        if (state.getValue(FuelGeneratorBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(FuelGeneratorBlock.LIT, lit), 3);
        }

        for (Direction d : Direction.values()) {
            if (be.energy.getEnergyStored() <= 0) break;
            BlockPos np = pos.relative(d);
            if (!level.isLoaded(np)) continue;
            IEnergyStorage dst = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.EnergyStorage.BLOCK, np, d.getOpposite());
            if (dst != null && dst.canReceive()) {
                int sent = dst.receiveEnergy(Math.min(be.energy.getEnergyStored(), OUTPUT), false);
                if (sent > 0) be.energy.setEnergy(be.energy.getEnergyStored() - sent);
            }
        }
    }

    /** Lejki/rury: wkładanie paliwa z góry i boków, od dołu wyjmowanie tylko resztek (pustych kubłów). */
    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
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
                return level != null && isFuel(items.getStackInSlot(slot), level) ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return level != null && isFuel(stack, level);
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.redcircuit.solid_fuel_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FuelGeneratorMenu(id, inventory, items, data, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        items.save(output.child("Items"));
        output.putInt("Energy", energy.getEnergyStored());
        output.putInt("BurnTime", burnTime);
        output.putInt("BurnTotal", burnTotal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (input.child("Items").isPresent()) items.load(input.childOrEmpty("Items"));
        energy.setEnergy(input.getIntOr("Energy", 0));
        burnTime = input.getIntOr("BurnTime", 0);
        burnTotal = input.getIntOr("BurnTotal", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), this.getItems().getStackInSlot(0));
    }
}
