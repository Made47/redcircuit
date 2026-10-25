package pl.geesio.redcircuit.tube;

import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.energy.ModEnergyStorage;
import pl.geesio.redcircuit.registry.ModBlockEntities;

import java.util.Set;

/**
 * Requester: na impuls redstone przeszukuje sieć rur podłączoną z tyłu, znajduje w inwentarzach
 * przedmiot pasujący do filtra i ściąga go rurami do siebie, a potem wypycha go do przodu.
 * Filtr (9 slotów) jest obsługiwany po kolei; pusty filtr = dowolny przedmiot. Zużywa energię.
 */
public class RequesterBlockEntity extends BlockEntity {
    public static final int CAPACITY = 10_000, ENERGY_PER_RETRIEVE = 200;

    public final MachineContainer filter = new MachineContainer(this, 9);
    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 500, 0, this::setChanged);
    private int nextSlot;

    public RequesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REQUESTER.get(), pos, state);
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    /** Wejście od strony rury (tył): przyjmuje zamówione przedmioty i od razu wypycha je do przodu. */
    public IItemHandler getHandler(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                Direction facing = getBlockState().getValue(RequesterBlock.FACING);
                if (level == null || side != facing.getOpposite() || stack.isEmpty()) return stack;
                int n = ExtractorBlock.deliverableCount(level, worldPosition, facing, stack, -1);
                if (n <= 0) return stack;
                ItemStack rest = stack.copy();
                ItemStack out = rest.split(n);
                if (!simulate) ExtractorBlock.deliver(level, worldPosition, facing, out, -1);
                return rest;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    /** Wykonuje jedno zamówienie. */
    public void retrieve(Level level, BlockPos pos, BlockState state) {
        if (energy.getEnergyStored() < pl.geesio.redcircuit.config.RedCircuitConfig.REQUESTER_FE.get()) return;
        Direction facing = state.getValue(RequesterBlock.FACING);
        BlockPos startTube = pos.relative(facing.getOpposite());
        if (!(level.getBlockEntity(startTube) instanceof TubeBlockEntity)) return;

        boolean anyPattern = false;
        for (int i = 0; i < 9; i++) {
            int slot = (nextSlot + i) % 9;
            ItemStack pattern = filter.getItem(slot);
            if (pattern.isEmpty()) continue;
            anyPattern = true;
            if (fetch(level, pos, startTube, pattern)) {
                nextSlot = (slot + 1) % 9;
                setChanged();
                return;
            }
        }
        if (!anyPattern) fetch(level, pos, startTube, ItemStack.EMPTY);
    }

    /** Szuka w sieci rur inwentarza z pasującym przedmiotem; zwraca true, jeśli coś wysłano. */
    private boolean fetch(Level level, BlockPos self, BlockPos startTube, ItemStack pattern) {
        int max = pattern.isEmpty() ? 64 : pattern.getCount();
        int sent = TubeNetwork.fetch(level, self, startTube, Set.of(), 
                s -> pattern.isEmpty() || ItemStack.isSameItemSameComponents(pattern, s), max);
        if (sent <= 0) return false;
        energy.setEnergy(energy.getEnergyStored() - pl.geesio.redcircuit.config.RedCircuitConfig.REQUESTER_FE.get());
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filter.save(output);
        output.putInt("Energy", energy.getEnergyStored());
        output.putInt("Next", nextSlot);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter.load(input);
        energy.setEnergy(input.getIntOr("Energy", 0));
        nextSlot = input.getIntOr("Next", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
            Containers.dropContents(level, pos, this.filter);
    }
}
