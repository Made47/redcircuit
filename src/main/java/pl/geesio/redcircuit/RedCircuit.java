package pl.geesio.redcircuit;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import pl.geesio.redcircuit.alloy.ModRecipes;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.registry.ModCapabilities;
import pl.geesio.redcircuit.registry.ModItems;
import pl.geesio.redcircuit.registry.ModDataComponents;
import pl.geesio.redcircuit.registry.ModMenus;
import pl.geesio.redcircuit.registry.ModTabs;

/**
 * RedCircuit - kable, bramki logiczne, rury i Azurity.
 * RedCircuit: przewody redstone, bramki logiczne, rury i energia dla NeoForge 1.21.1.
 */
@Mod(RedCircuit.MODID)
public class RedCircuit {
    public static final String MODID = "redcircuit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RedCircuit(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.LOCAL, pl.geesio.redcircuit.config.RedCircuitConfig.SPEC);
        pl.geesio.redcircuit.worldgen.ModWorldgen.BIOME_MODIFIERS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        pl.geesio.redcircuit.registry.ModSounds.SOUNDS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModTabs.TABS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(pl.geesio.redcircuit.network.ModNetwork::register);
        // Od 1.21.2 serwer nie wysyła receptur klientom - wysyłamy własne typy (dla JEI)
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e) ->
                e.sendRecipes(ModRecipes.ALLOYING.get(), ModRecipes.MACERATING.get(), ModRecipes.SAWING.get()));
        // CB Multipart nie jest dostępny dla Minecraft 26.x - integracja wyłączona w tej gałęzi.
        // Opcjonalna integracja z CC: Tweaked (kabel wiązkowy + peryferia)
        if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            pl.geesio.redcircuit.compat.cc.CcCompat.init(modBus);
        }
    }
}
