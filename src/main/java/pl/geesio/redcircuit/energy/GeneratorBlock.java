package pl.geesio.redcircuit.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.util.TickerUtil;

/** Blok generatora (Termostos lub Generator). PPM pokazuje produkcję. */
public class GeneratorBlock extends Block implements EntityBlock {
    private final GeneratorBlockEntity.Kind kind;

    public GeneratorBlock(Properties properties, GeneratorBlockEntity.Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == GeneratorBlockEntity.Kind.THERMOPILE
                ? GeneratorBlockEntity.thermopile(pos, state)
                : GeneratorBlockEntity.kinetic(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return TickerUtil.create(type,
                kind == GeneratorBlockEntity.Kind.THERMOPILE ? ModBlockEntities.THERMOPILE.get() : ModBlockEntities.KINETIC_GENERATOR.get(),
                GeneratorBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GeneratorBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("message.redcircuit.solar",
                    be.getLastRate(), be.getEnergy().getEnergyStored()));
        }
        return InteractionResult.SUCCESS;
    }
}
