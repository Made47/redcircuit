package pl.geesio.redcircuit.energy;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import pl.geesio.redcircuit.util.transfer.IFluidHandler;
import pl.geesio.redcircuit.util.transfer.FluidTank;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.CableBlock;

/**
 * Rura do płynów: mały zbiornik (1000 mB). Rury wyrównują poziom między sobą,
 * a płyn wypychają do podłączonych zbiorników/maszyn - z wyjątkiem strony,
 * z której płyn ostatnio przyszedł (żeby nie oddawać go z powrotem do źródła).
 */
public class FluidPipeBlockEntity extends BlockEntity {
    public static final int CAPACITY = 1000;
    public static final int RATE = 250;          // mB na tick na stronę
    private static final int INPUT_COOLDOWN = 20;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final int[] inputCooldown = new int[6];

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    /** Handler dla danej strony - zapamiętuje, skąd przyszedł płyn. */
    public IFluidHandler getHandler(@Nullable Direction side) {
        return new IFluidHandler() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public FluidResource getResource(int index) {
                return tank.getResource(0);
            }

            @Override
            public long getAmountAsLong(int index) {
                return tank.getAmountAsLong(0);
            }

            @Override
            public long getCapacityAsLong(int index, FluidResource resource) {
                return tank.getCapacityAsLong(0, resource);
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                return tank.isValid(0, resource);
            }

            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                int filled = tank.insert(0, resource, amount, transaction);
                if (filled > 0 && side != null) {
                    // blokada cofania płynu tylko, jeśli transakcja zostanie zatwierdzona
                    new RootCommitJournal(() -> inputCooldown[side.get3DDataValue()] = INPUT_COOLDOWN).updateSnapshots(transaction);
                }
                return filled;
            }

            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return tank.extract(0, resource, amount, transaction);
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        if (level.isClientSide()) return;
        for (int i = 0; i < 6; i++) if (be.inputCooldown[i] > 0) be.inputCooldown[i]--;
        if (be.tank.isEmpty()) return;

        Direction[] dirs = Direction.values();
        int start = (int) (level.getGameTime() % 6);     // rotacja, żeby żadna strona nie była uprzywilejowana
        for (int k = 0; k < 6 && !be.tank.isEmpty(); k++) {
            Direction d = dirs[(start + k) % 6];
            if (!CableBlock.isConnected(state, d)) continue;
            BlockPos np = pos.relative(d);
            if (!level.isLoaded(np)) continue;

            if (level.getBlockEntity(np) instanceof FluidPipeBlockEntity other) {
                FluidStack mine = be.tank.getFluid();
                FluidStack theirs = other.tank.getFluid();
                if (!theirs.isEmpty() && !FluidStack.isSameFluidSameComponents(mine, theirs)) continue;
                int diff = mine.getAmount() - theirs.getAmount();
                if (diff <= 1) continue;
                int move = Math.min(diff / 2, RATE);
                int moved = other.tank.fill(be.tank.drain(move, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.EXECUTE);
                if (moved > 0) be.tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                continue;
            }

            if (be.inputCooldown[d.get3DDataValue()] > 0) continue;  // nie oddawaj do źródła
            IFluidHandler dst = IFluidHandler.wrap(level.getCapability(Capabilities.Fluid.BLOCK, np, d.getOpposite()));
            if (dst == null) continue;
            int moved = dst.fill(be.tank.drain(RATE, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) be.tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output.child("Tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input.childOrEmpty("Tank"));
    }
}
