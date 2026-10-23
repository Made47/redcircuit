package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.TickerUtil;
import pl.geesio.redcircuit.wire.EncasedWireBlock;
import pl.geesio.redcircuit.wire.WireTypes;

/** Przewód ze stopu energetycznego w obudowie: przesyła energię środkiem bloku. */
public class EncasedPowerWireBlock extends EncasedWireBlock implements EntityBlock {

    public EncasedPowerWireBlock(Properties properties) {
        super(properties, 6.0);
        registerDefaultState(disconnected(stateDefinition.any()));
    }

    @Override
    public int wireType() {
        return WireTypes.POWER;
    }

    @Override
    protected boolean connectsToDevice(LevelAccessor level, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        return EnergyConnect.hasEnergy(level, neighborPos, neighbor, dir.getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerWireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : TickerUtil.create(type, ModBlockEntities.POWER_WIRE.get(), EnergyNodeBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return PowerWireBlock.showEnergy(level, pos, player);
    }
}
