package pl.geesio.redcircuit.machine;

import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import pl.geesio.redcircuit.util.transfer.IItemHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.tube.ExtractorBlock;

/**
 * Item Detector: wstawia się go w linię rur (tył = wejście, przód = wyjście). Każdy przejeżdżający
 * stos przedmiotów wysyła krótki impuls redstone (do wszystkich stron).
 */
public class ItemDetectorBlock extends ExtractorBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ItemDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public boolean connectsToTube(BlockState state, Direction fromTube) {
        Direction f = state.getValue(FACING);
        return fromTube == f || fromTube == f.getOpposite();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        // Nie reaguje na sygnał - sam go wysyła.
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(ACTIVE) ? 15 : 0;
    }

    /** Wejście od tyłu: przedmiot od razu wychodzi przodem, a detektor wysyła impuls. */
    @Nullable
    public static IItemHandler handler(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        Direction facing = state.getValue(FACING);
        if (side != facing.getOpposite()) return null;
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
                if (stack.isEmpty()) return stack;
                int n = ExtractorBlock.deliverableCount(level, pos, facing, stack, -1);
                if (n <= 0) return stack;
                ItemStack rest = stack.copy();
                ItemStack out = rest.split(n);
                if (!simulate) {
                    ExtractorBlock.deliver(level, pos, facing, out, -1);
                    BlockState cur = level.getBlockState(pos);
                    if (cur.getBlock() instanceof ItemDetectorBlock) {
                        if (!cur.getValue(ACTIVE)) level.setBlock(pos, cur.setValue(ACTIVE, true), Block.UPDATE_ALL);
                        level.scheduleTick(pos, cur.getBlock(), 4);
                    }
                }
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
}
