package pl.geesio.redcircuit.worldgen;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pl.geesio.redcircuit.RedCircuit;

public final class ModWorldgen {
    private ModWorldgen() {}

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RedCircuit.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfiguredOreModifier>> CONFIGURED_ORE =
            BIOME_MODIFIERS.register("configured_ore", () -> ConfiguredOreModifier.CODEC);
}
