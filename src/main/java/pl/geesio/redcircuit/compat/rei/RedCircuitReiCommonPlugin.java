package pl.geesio.redcircuit.compat.rei;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import net.minecraft.resources.Identifier;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.AlloyRecipe;
import pl.geesio.redcircuit.alloy.MaceratingRecipe;
import pl.geesio.redcircuit.alloy.ModRecipes;
import pl.geesio.redcircuit.alloy.SawingRecipe;

/**
 * Część wspólna integracji z REI: od MC 1.21.2 receptury są tylko na serwerze, więc REI buduje
 * wyświetlenia po stronie serwera i wysyła je klientowi (stąd serializery).
 */
public class RedCircuitReiCommonPlugin implements REICommonPlugin {
    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(Identifier.fromNamespaceAndPath(RedCircuit.MODID, "alloying"), AlloyDisplay.SERIALIZER);
        registry.register(Identifier.fromNamespaceAndPath(RedCircuit.MODID, "processing"), ProcessingDisplay.SERIALIZER);
    }

    @Override
    public void registerDisplays(ServerDisplayRegistry registry) {
        registry.beginRecipeFiller(AlloyRecipe.class).filterType(ModRecipes.ALLOYING.get())
                .fill(h -> new AlloyDisplay(h.value()));
        registry.beginRecipeFiller(MaceratingRecipe.class).filterType(ModRecipes.MACERATING.get())
                .fill(h -> new ProcessingDisplay(ReiIds.MACERATING, h.value().ingredient(), h.value().result(),
                        h.value().bonus(), h.value().bonusChance(), h.value().time()));
        registry.beginRecipeFiller(SawingRecipe.class).filterType(ModRecipes.SAWING.get())
                .fill(h -> new ProcessingDisplay(ReiIds.SAWING, h.value().ingredient(), h.value().result(),
                        h.value().bonus(), h.value().bonusChance(), h.value().time()));
    }
}
