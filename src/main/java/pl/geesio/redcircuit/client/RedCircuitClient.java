package pl.geesio.redcircuit.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.registry.ModMenus;
import pl.geesio.redcircuit.wire.FaceWireBlock;

import java.util.function.Function;

/** Kod tylko po stronie klienta (renderery, ekrany GUI, modele przewodów). */
@Mod(value = RedCircuit.MODID, dist = Dist.CLIENT)
public class RedCircuitClient {
    public RedCircuitClient(IEventBus modBus) {
        modBus.addListener(RedCircuitClient::registerRenderers);
        modBus.addListener(RedCircuitClient::registerScreens);
        modBus.addListener(RedCircuitClient::onModifyBakingResult);
        modBus.addListener(RedCircuitClient::registerBlockColors);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, ClientRecipes::onRecipesReceived);
    }

    /**
     * Sztabki i surowce używają tekstur sztabki/surowego żelaza z gry, przebarwionych na kolor metalu
     * (przebarwienie przedmiotów jest w plikach assets/redcircuit/items/*.json - "tints").
     */
    private static final int TIN = 0xC8D7E6, SILVER = 0xF5F8FF;

    /** Bloki metali używają tekstury bloku żelaza z gry, przebarwionej tak samo jak sztabki. */
    private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        tintBlock(event, TIN, ModBlocks.TIN_BLOCK.get());
        tintBlock(event, SILVER, ModBlocks.SILVER_BLOCK.get());
    }

    private static void tintBlock(RegisterColorHandlersEvent.BlockTintSources event, int rgb, net.minecraft.world.level.block.Block block) {
        final int argb = 0xFF000000 | rgb;
        event.register(java.util.List.of(state -> argb), block);
    }


    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TUBE.get(), TubeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ACCELERATOR.get(), TubeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.KINETIC_GENERATOR.get(), RotorRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        event.register(ModMenus.MACERATOR.get(), MaceratorScreen::new);
        event.register(ModMenus.SAWMILL.get(), SawmillScreen::new);
        event.register(ModMenus.BATTERY_BOX.get(), BatteryBoxScreen::new);
        event.register(ModMenus.CHARGER.get(), ChargerScreen::new);
        event.register(ModMenus.SOLID_FUEL_GENERATOR.get(), FuelGeneratorScreen::new);
        event.register(ModMenus.BLUEPRINT_TABLE.get(), BlueprintTableScreen::new);
        event.register(ModMenus.ALLOY_FURNACE.get(), AlloyFurnaceScreen::new);
        event.register(ModMenus.POWER_ALLOY_FURNACE.get(), AlloyFurnaceScreen::new);
    }

    /** Podmienia modele przewodów na dynamiczne (przyklejane do ścian). */
    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var atlas = event.getBlockAtlasPreparations();
        Function<Identifier, TextureAtlasSprite> textures = id -> {
            TextureAtlasSprite s = atlas.getSprite(id);
            return s != null ? s : atlas.missing();
        };
        replace(event, ModBlocks.SIGNAL_ALLOY_WIRE.get(),
                sprite(textures, "signal_alloy_off"), sprite(textures, "signal_alloy_on"));
        for (DyeColor color : DyeColor.values()) {
            replace(event, ModBlocks.INSULATED_WIRES.get(color.getId()).get(),
                    sprite(textures, "insulated_" + color.getName()), null);
        }
        replace(event, ModBlocks.BUNDLED_CABLE.get(), sprite(textures, "bundled_cable"), null);
        replace(event, ModBlocks.POWER_ALLOY_WIRE.get(), sprite(textures, "power_alloy_wire"), null);
    }

    private static TextureAtlasSprite sprite(Function<Identifier, TextureAtlasSprite> textures, String name) {
        return textures.apply(Identifier.fromNamespaceAndPath(RedCircuit.MODID, "block/" + name));
    }

    private static void replace(ModelEvent.ModifyBakingResult event, FaceWireBlock block,
                                TextureAtlasSprite sprite, @Nullable TextureAtlasSprite powered) {
        FaceWireModel model = new FaceWireModel(block, sprite, powered);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            event.getBakingResult().blockStateModels().put(state, model);
        }
    }
}
