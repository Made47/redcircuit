package pl.geesio.redcircuit.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;

import java.util.Optional;

/** Paliwo pieców (od 26.3 czas palenia jest komponentem przedmiotu {@code minecraft:cooking_fuel}). */
public final class Fuel {
    private Fuel() {}

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
    }

    /** Czas palenia w tickach dla urządzenia {@code be} (0, jeśli to nie paliwo). */
    public static int burnTime(Level level, BlockEntity be, ItemStack stack) {
        if (!isFuel(stack) || !(level instanceof ServerLevel server)) return 0;
        LootContext ctx = new LootContext.Builder(new LootParams.Builder(server)
                .withParameter(LootContextParams.BLOCK_STATE, be.getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, be)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(be.getBlockPos()))
                .withParameter(LootContextParams.CONTAINER, new SimpleContainer(stack.copy()))
                .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
                .create(LootContextParamSets.CONTAINER_PROCESS))
                .create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, ctx, 0);
    }
}
