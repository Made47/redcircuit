package pl.geesio.redcircuit.worldgen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.config.RedCircuitConfig;

/**
 * Rudy w Overworldzie (Fabric Biome API zamiast modyfikatorów biomów NeoForge).
 * Cechy (configured/placed feature) są w data/redcircuit/worldgen; rudę można wyłączyć w konfiguracji.
 */
public final class ModWorldgen {
    private ModWorldgen() {}

    public static void register() {
        for (String ore : RedCircuitConfig.ORES) {
            if (!RedCircuitConfig.oreEnabled(ore)) continue;
            BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(RedCircuit.MODID, ore + "_ore")));
        }
    }
}
