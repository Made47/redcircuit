package pl.geesio.redcircuit.tube;

import net.minecraft.world.level.LevelReader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.BlockHitResult;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.component.DataComponents;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import net.minecraft.core.component.DataComponents;
import pl.geesio.redcircuit.util.CableBlock;
import net.minecraft.core.component.DataComponents;
import pl.geesio.redcircuit.util.TickerUtil;

/**
 * Rura pneumatyczna transportująca przedmioty.
 * Można ją pomalować barwnikiem (PPM) - pomalowana rura przyjmuje tylko przedmioty w tym kolorze.
 * Shift + PPM pustą ręką usuwa farbę.
 */
public class TubeBlock extends CableBlock implements EntityBlock {
    public static final int NO_PAINT = 16;
    public static final IntegerProperty PAINT = IntegerProperty.create("paint", 0, NO_PAINT);

    public TubeBlock(Properties properties) {
        super(properties, 6.0);
        registerDefaultState(disconnected(stateDefinition.any()).setValue(PAINT, NO_PAINT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PAINT);
    }

    /** Koszt przejazdu przez tę rurę przy wyznaczaniu trasy (rura restrykcyjna jest "droga"). */
    public int routeCost() {
        return 1;
    }

    /** Czy przedmiot o danym kolorze może przejść przez rurę w tym stanie. */
    public static boolean passable(BlockState tube, int itemColor) {
        int paint = tube.getValue(PAINT);
        return paint == NO_PAINT || paint == itemColor;
    }

    @Override
    protected boolean canConnectTo(LevelReader level, BlockPos pos, BlockState self, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        Block b = neighbor.getBlock();
        if (b instanceof TubeBlock) {
            int p1 = self.getValue(PAINT), p2 = neighbor.getValue(PAINT);
            return p1 == NO_PAINT || p2 == NO_PAINT || p1 == p2;
        }
        if (b instanceof TubeConnectable tc) return tc.connectsToTube(neighbor, dir);
        if (b instanceof CableBlock || b instanceof pl.geesio.redcircuit.wire.FaceWireBlock) return false;
        if (level instanceof Level lvl && !neighbor.isAir()) {
            return pl.geesio.redcircuit.platform.capabilities.CapLookup.block(lvl, Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite()) != null;
        }
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return TickerUtil.create(type, ModBlockEntities.TUBE.get(), TubeBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem && stack.get(DataComponents.DYE) != null) {
            int color = stack.get(DataComponents.DYE).getId();
            if (state.getValue(PAINT) == color) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) repaint(level, pos, state, color);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && state.getValue(PAINT) != NO_PAINT) {
            if (!level.isClientSide()) repaint(level, pos, state, NO_PAINT);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void repaint(Level level, BlockPos pos, BlockState state, int paint) {
        BlockState painted = computeConnections(level, pos, state.setValue(PAINT, paint));
        level.setBlock(pos, painted, Block.UPDATE_ALL);
    }
}
