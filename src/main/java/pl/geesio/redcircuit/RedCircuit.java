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
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, pl.geesio.redcircuit.config.RedCircuitConfig.SPEC);
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
        // Opcjonalna integracja z CB Multipart (przewody jako części bloku wieloczęściowego)
        if (net.neoforged.fml.ModList.get().isLoaded("cb_multipart")) {
            pl.geesio.redcircuit.compat.cbm.CbmCompat.init(modBus);
        }
        // Opcjonalna integracja z CC: Tweaked (kabel wiązkowy + peryferia)
        if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            pl.geesio.redcircuit.compat.cc.CcCompat.init(modBus);
        }
    }
}
