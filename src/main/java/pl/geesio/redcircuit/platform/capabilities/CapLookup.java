package pl.geesio.redcircuit.platform.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Zamiennik metod Level#getCapability i ItemStack#getCapability z NeoForge. */
public final class CapLookup {
    private CapLookup() {}

    @Nullable
    public static <T, C> T block(Level level, BlockCapability<T, C> cap, BlockPos pos, C context) {
        if (!level.isLoaded(pos)) return null;
        return cap.getCapability(level, pos, null, null, context);
    }

    @Nullable
    public static <T, C> T block(Level level, BlockCapability<T, C> cap, BlockPos pos,
                                 @Nullable BlockState state, @Nullable BlockEntity be, C context) {
        return cap.getCapability(level, pos, state, be, context);
    }

    @Nullable
    public static <T> T item(ItemStack stack, ItemCapability<T, Void> cap) {
        return cap.getCapability(stack, null);
    }

    @Nullable
    public static <T, C> T item(ItemStack stack, ItemCapability<T, C> cap, C context) {
        return cap.getCapability(stack, context);
    }
}
