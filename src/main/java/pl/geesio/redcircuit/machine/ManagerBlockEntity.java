package pl.geesio.redcircuit.machine;

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
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.energy.ModEnergyStorage;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.tube.MachineContainer;
import pl.geesio.redcircuit.tube.ExtractorBlock;
import pl.geesio.redcircuit.tube.TubeBlockEntity;
import pl.geesio.redcircuit.tube.TubeNetwork;

import java.util.Set;

/**
 * Manager: pilnuje zapasów w inwentarzu za sobą. Filtr (9 slotów) = lista przedmiotów i ich ilości.
 * Brakujące przedmioty zamawia z sieci rur (z przodu), a przedmioty spoza listy odsyła do sieci.
 */
public class ManagerBlockEntity extends BlockEntity {
    public static final int CAPACITY = 10_000, ENERGY_PER_ORDER = 100, INTERVAL = 20, ORDER_TIMEOUT = 5;

    public final MachineContainer filter = new MachineContainer(this, 9);
    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 500, 0, this::setChanged);
    private int timer, waiting, next;

    public ManagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANAGER.get(), pos, state);
    }

    public ModEnergyStorage getEnergy() {
        return energy;
    }

    @Nullable
    private IItemHandler managed() {
        if (level == null) return null;
        Direction facing = getBlockState().getValue(ExtractorBlock.FACING);
        return pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.ItemHandler.BLOCK, worldPosition.relative(facing.getOpposite()), facing);
    }

    private static int count(IItemHandler h, ItemStack pattern) {
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(s, pattern)) n += s.getCount();
        }
        return n;
    }

    private boolean listed(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            ItemStack p = filter.getItem(i);
            if (!p.isEmpty() && ItemStack.isSameItemSameComponents(p, stack)) return true;
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ManagerBlockEntity be) {
        if (level.isClientSide() || ++be.timer < INTERVAL) return;
        be.timer = 0;
        IItemHandler inv = be.managed();
        if (inv == null || be.filter.isEmpty()) return;
        Direction facing = state.getValue(ExtractorBlock.FACING);
        BlockPos front = pos.relative(facing);
        if (!(level.getBlockEntity(front) instanceof TubeBlockEntity)) return;

        // 1. Przedmioty spoza listy wracają do sieci
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.extractItem(i, 64, true);
            if (s.isEmpty() || be.listed(s)) continue;
            int n = ExtractorBlock.deliverableCount(level, pos, facing, s, -1);
            if (n <= 0) break;
            ExtractorBlock.deliver(level, pos, facing, inv.extractItem(i, n, false), -1);
            break;
        }

        // 2. Zamawianie brakujących (jedno zamówienie naraz)
        if (be.waiting > 0) {
            be.waiting--;
            return;
        }
        if (be.energy.getEnergyStored() < pl.geesio.redcircuit.config.RedCircuitConfig.MANAGER_FE.get()) return;
        for (int k = 0; k < 9; k++) {
            int slot = (be.next + k) % 9;
            ItemStack p = be.filter.getItem(slot);
            if (p.isEmpty()) continue;
            int need = p.getCount() - count(inv, p);
            if (need <= 0) continue;
            int sent = TubeNetwork.fetch(level, pos, front, Set.of(pos.relative(facing.getOpposite())),
                    s -> ItemStack.isSameItemSameComponents(p, s), Math.min(need, p.getMaxStackSize()));
            if (sent > 0) {
                be.energy.setEnergy(be.energy.getEnergyStored() - pl.geesio.redcircuit.config.RedCircuitConfig.MANAGER_FE.get());
                be.waiting = ORDER_TIMEOUT;
                be.next = (slot + 1) % 9;
                be.setChanged();
                return;
            }
        }
    }

    /** Wejście od przodu: zamówione przedmioty trafiają do pilnowanego inwentarza. */
    @Nullable
    public IItemHandler getHandler(@Nullable Direction side) {
        Direction facing = getBlockState().getValue(ExtractorBlock.FACING);
        if (side != facing) return null;
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
                IItemHandler inv = managed();
                if (inv == null) return stack;
                ItemStack rest = ItemHandlerHelper.insertItem(inv, stack, simulate);
                if (!simulate && rest.getCount() < stack.getCount()) waiting = 0;
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filter.save(output);
        output.putInt("Energy", energy.getEnergyStored());
        output.putInt("Next", next);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter.load(input);
        energy.setEnergy(input.getIntOr("Energy", 0));
        next = input.getIntOr("Next", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
            Containers.dropContents(level, pos, this.filter);
    }
}
