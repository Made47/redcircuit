package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.TickerUtil;
import pl.geesio.redcircuit.wire.FaceWireBlock;

/** Przewód Power Alloy przesyłający Azurity (Forge Energy), przyklejany do ścian. */
public class PowerWireBlock extends FaceWireBlock implements EntityBlock {

    public PowerWireBlock(Properties properties) {
        super(properties, 2f, 2f);
        registerDefaultState(noFaces(stateDefinition.any()).setValue(BlockStateProperties.DOWN, true));
    }

    @Override
    public boolean compatible(FaceWireBlock other) {
        return other instanceof PowerWireBlock;
    }

    @Override
    protected boolean connectsToDevice(BlockGetter level, BlockPos pos, Direction side, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        Block b = neighbor.getBlock();
        if (b instanceof BatteryBlock || b instanceof SolarPanelBlock || b instanceof ElectricFurnaceBlock
                || b instanceof GeneratorBlock || b instanceof PumpBlock || b instanceof FuelGeneratorBlock
                || b instanceof ChargerBlock || b instanceof MaceratorBlock || b instanceof SawmillBlock) return true;
        if (b == pl.geesio.redcircuit.registry.ModBlocks.POWER_ALLOY_FURNACE.get()) return true;
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
        return showEnergy(level, pos, player);
    }

    static InteractionResult showEnergy(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyNodeBlockEntity node) {
            player.displayClientMessage(Component.translatable("message.redcircuit.energy",
                    node.getEnergy().getEnergyStored(), node.getEnergy().getMaxEnergyStored()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
