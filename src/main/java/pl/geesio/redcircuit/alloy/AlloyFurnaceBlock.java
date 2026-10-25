package pl.geesio.redcircuit.alloy;

import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.TickerUtil;

/** Piec do stopów (na paliwo) lub elektryczny (na energię). */
public class AlloyFurnaceBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final boolean electric;

    public AlloyFurnaceBlock(Properties properties, boolean electric) {
        super(properties);
        this.electric = electric;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return electric ? AlloyFurnaceBlockEntity.electric(pos, state) : AlloyFurnaceBlockEntity.fuel(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return TickerUtil.create(type,
                electric ? ModBlockEntities.POWER_ALLOY_FURNACE.get() : ModBlockEntities.ALLOY_FURNACE.get(),
                AlloyFurnaceBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AlloyFurnaceBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }


    /** Dym i trzask ognia jak w zwykłym piecu (tylko wersja na paliwo). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (electric || !state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.0f, false);
        }
        Direction d = state.getValue(FACING);
        double off = random.nextDouble() * 0.6 - 0.3;
        double px = x + d.getStepX() * 0.52 + (d.getAxis() == Direction.Axis.Z ? off : 0);
        double pz = z + d.getStepZ() * 0.52 + (d.getAxis() == Direction.Axis.X ? off : 0);
        double py = y + random.nextDouble() * 6.0 / 16.0;
        level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0, 0);
    }
}
