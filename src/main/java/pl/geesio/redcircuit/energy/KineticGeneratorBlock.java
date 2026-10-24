package pl.geesio.redcircuit.energy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import pl.geesio.redcircuit.registry.ModItems;

/**
 * Generator: z przodu montuje się wiatrak albo koło wodne (PPM przedmiotem).
 * Shift + PPM pustą ręką zdejmuje wirnik.
 */
public class KineticGeneratorBlock extends GeneratorBlock {
    public enum Rotor implements StringRepresentable {
        NONE("none"), WINDMILL("windmill"), WATER_WHEEL("water_wheel");

        private final String name;

        Rotor(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Rotor> ROTOR = EnumProperty.create("rotor", Rotor.class);
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public KineticGeneratorBlock(Properties properties) {
        super(properties, GeneratorBlockEntity.Kind.KINETIC);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTOR, Rotor.NONE)
                .setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTOR, RUNNING);
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

    private static Item itemFor(Rotor rotor) {
        return rotor == Rotor.WINDMILL ? ModItems.WINDMILL.get() : ModItems.WATER_WHEEL.get();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        Rotor rotor = stack.is(ModItems.WINDMILL.get()) ? Rotor.WINDMILL
                : stack.is(ModItems.WATER_WHEEL.get()) ? Rotor.WATER_WHEEL : Rotor.NONE;
        if (rotor == Rotor.NONE) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(ROTOR) != Rotor.NONE) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(ROTOR, rotor), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.sendOverlayMessage(Component.translatable("message.redcircuit.rotor_installed"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Rotor rotor = state.getValue(ROTOR);
        if (player.isShiftKeyDown() && rotor != Rotor.NONE) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(ROTOR, Rotor.NONE).setValue(RUNNING, false), Block.UPDATE_ALL);
                ItemStack back = new ItemStack(itemFor(rotor));
                if (!player.getInventory().add(back)) player.drop(back, false);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (state.getValue(ROTOR) != Rotor.NONE) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(itemFor(state.getValue(ROTOR))));
        }
    }
}
