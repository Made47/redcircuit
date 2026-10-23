package pl.geesio.redcircuit.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import pl.geesio.redcircuit.config.RedCircuitConfig;

/**
 * Jak neoforge:add_features, ale z kluczem rudy ("ore") - rudę można wyłączyć w pliku konfiguracji.
 */
public record ConfiguredOreModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features,
                                    GenerationStep.Decoration step, String ore) implements BiomeModifier {

    public static final MapCodec<ConfiguredOreModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfiguredOreModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ConfiguredOreModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(ConfiguredOreModifier::step),
            Codec.STRING.fieldOf("ore").forGetter(ConfiguredOreModifier::ore)
    ).apply(i, ConfiguredOreModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biomes.contains(biome) || !RedCircuitConfig.oreEnabled(ore)) return;
        var gen = builder.getGenerationSettings();
        for (Holder<PlacedFeature> f : features) gen.addFeature(step, f);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModWorldgen.CONFIGURED_ORE.get();
    }
}
