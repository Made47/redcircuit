package pl.geesio.redcircuit.platform.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Odpowiednik BlockCapability z NeoForge. Najpierw pyta dostawców zarejestrowanych przez RedCircuit,
 * potem (dla bloków innych modów i vanilli) odpowiednie API Fabrica - przez {@link Fallback}.
 */
public final class BlockCapability<T, C> {
    @FunctionalInterface
    public interface BlockEntityProvider<T, C, BE extends BlockEntity> {
        @Nullable
        T getCapability(BE be, C context);
    }

    @FunctionalInterface
    public interface BlockProvider<T, C> {
        @Nullable
        T getCapability(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, C context);
    }

    @FunctionalInterface
    public interface Fallback<T> {
        @Nullable
        T find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side);
    }

    private final Identifier name;
    private final Map<BlockEntityType<?>, BlockEntityProvider<T, C, ?>> beProviders = new HashMap<>();
    private final Map<Block, BlockProvider<T, C>> blockProviders = new HashMap<>();
    private Fallback<T> fallback = (l, p, s, b, d) -> null;

    BlockCapability(Identifier name) {
        this.name = name;
    }

    public Identifier name() {
        return name;
    }

    void setFallback(Fallback<T> fallback) {
        this.fallback = fallback;
    }

    <BE extends BlockEntity> void addProvider(BlockEntityType<BE> type, BlockEntityProvider<T, C, ? super BE> provider) {
        beProviders.put(type, provider);
    }

    void addProvider(Block block, BlockProvider<T, C> provider) {
        blockProviders.put(block, provider);
    }

    /** Tylko dostawcy RedCircuit (bez API Fabrica). */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T getOwn(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, C context) {
        BlockProvider<T, C> bp = blockProviders.get(state.getBlock());
        if (bp != null) return bp.getCapability(level, pos, state, be, context);
        if (be != null) {
            BlockEntityProvider p = beProviders.get(be.getType());
            if (p != null) return (T) p.getCapability(be, context);
        }
        return null;
    }

    @Nullable
    public T getCapability(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity be, C context) {
        if (state == null) state = level.getBlockState(pos);
        if (be == null && state.hasBlockEntity()) be = level.getBlockEntity(pos);
        if (blockProviders.containsKey(state.getBlock()) || (be != null && beProviders.containsKey(be.getType()))) {
            return getOwn(level, pos, state, be, context);
        }
        return fallback.find(level, pos, state, be, context instanceof Direction d ? d : null);
    }
}
