package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
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
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return tank.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return CAPACITY;
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return true;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                int filled = tank.fill(resource, action);
                if (filled > 0 && action.execute() && side != null) inputCooldown[side.get3DDataValue()] = INPUT_COOLDOWN;
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return tank.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return tank.drain(maxDrain, action);
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        if (level.isClientSide) return;
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
            IFluidHandler dst = level.getCapability(Capabilities.FluidHandler.BLOCK, np, d.getOpposite());
            if (dst == null) continue;
            int moved = dst.fill(be.tank.drain(RATE, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) be.tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
