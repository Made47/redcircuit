package pl.geesio.redcircuit;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.slf4j.Logger;
import pl.geesio.redcircuit.alloy.ModRecipes;
import pl.geesio.redcircuit.config.RedCircuitConfig;
import pl.geesio.redcircuit.craft.ScrewdriverItem;
import pl.geesio.redcircuit.platform.capabilities.RegisterCapabilitiesEvent;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.registry.ModCapabilities;
import pl.geesio.redcircuit.registry.ModDataComponents;
import pl.geesio.redcircuit.registry.ModItems;
import pl.geesio.redcircuit.registry.ModMenus;
import pl.geesio.redcircuit.registry.ModSounds;
import pl.geesio.redcircuit.registry.ModTabs;
import pl.geesio.redcircuit.wire.FaceWireBlock;

/**
 * RedCircuit: przewody redstone, bramki logiczne, rury i energia - wersja dla Fabric 1.21.1.
 */
public class RedCircuit implements ModInitializer {
    public static final String MODID = "redcircuit";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        RedCircuitConfig.SPEC.load(FabricLoader.getInstance().getConfigDir().resolve("redcircuit-common.properties"));

        // Kolejność ma znaczenie: przedmioty używają komponentów danych, typy bloków-encji - bloków.
        ModSounds.SOUNDS.register();
        ModDataComponents.COMPONENTS.register();
        ModBlocks.BLOCKS.register();
        ModItems.ITEMS.register();
        ModBlockEntities.BLOCK_ENTITIES.register();
        ModMenus.MENUS.register();
        ModRecipes.TYPES.register();
        ModRecipes.SERIALIZERS.register();
        ModTabs.TABS.register();

        ModCapabilities.register(new RegisterCapabilitiesEvent());
        pl.geesio.redcircuit.network.ModNetwork.register();
        pl.geesio.redcircuit.worldgen.ModWorldgen.register();

        FuelValueEvents.BUILD.register((builder, ctx) -> builder.add(ModItems.SAWDUST.get(), 100));

        // Śrubokręt działa przed interakcją z blokiem (NeoForge: onItemUseFirst)
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ScrewdriverItem screwdriver) {
                return screwdriver.onItemUseFirst(stack, new UseOnContext(player, hand, hit));
            }
            return InteractionResult.PASS;
        });

        // Kilka przewodów w jednym bloku: niszczony jest tylko ten, w który celuje gracz
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
            if (state.getBlock() instanceof FaceWireBlock wire) {
                return wire.onDestroyedByPlayer(state, level, pos, player);
            }
            return true;
        });

        // Opcjonalna integracja z CC: Tweaked (kabel wiązkowy + peryferia)
        if (FabricLoader.getInstance().isModLoaded("computercraft")) {
            pl.geesio.redcircuit.compat.cc.CcCompat.init();
        }
    }
}
