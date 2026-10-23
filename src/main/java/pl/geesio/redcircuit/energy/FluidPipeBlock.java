package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.CableBlock;
import pl.geesio.redcircuit.util.TickerUtil;
import pl.geesio.redcircuit.wire.FaceWireBlock;

/** Rura do płynów (mosiężna). Łączy się z innymi rurami i z każdym blokiem przyjmującym płyny. */
public class FluidPipeBlock extends CableBlock implements EntityBlock {

    public FluidPipeBlock(Properties properties) {
        super(properties, 6.0);
        registerDefaultState(disconnected(stateDefinition.any()));
    }

    @Override
    protected boolean canConnectTo(LevelAccessor level, BlockPos pos, BlockState self, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        Block b = neighbor.getBlock();
        if (b instanceof FluidPipeBlock) return true;
        if (b instanceof CableBlock || b instanceof FaceWireBlock) return false;
        if (b instanceof PumpBlock) return true;
        if (level instanceof Level lvl && !neighbor.isAir() && neighbor.hasBlockEntity()) {
            return pl.geesio.redcircuit.platform.capabilities.CapLookup.block(lvl, Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite()) != null;
        }
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : TickerUtil.create(type, ModBlockEntities.FLUID_PIPE.get(), FluidPipeBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
            var fluid = be.getTank().getFluid();
            Component name = fluid.isEmpty() ? Component.translatable("message.redcircuit.pump_empty") : fluid.getHoverName();
            player.displayClientMessage(Component.translatable("message.redcircuit.pipe", name,
                    be.getTank().getFluidAmount(), FluidPipeBlockEntity.CAPACITY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
