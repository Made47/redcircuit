package pl.geesio.redcircuit.client;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.world.level.ItemLike;
import pl.geesio.redcircuit.registry.ModItems;
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
        if (net.neoforged.fml.ModList.get().isLoaded("cb_multipart")) {
            pl.geesio.redcircuit.compat.cbm.client.CbmCompatClient.init(modBus);
        }
        modBus.addListener(RedCircuitClient::registerScreens);
        modBus.addListener(RedCircuitClient::onModifyBakingResult);
        modBus.addListener(RedCircuitClient::registerItemColors);
        modBus.addListener(RedCircuitClient::registerBlockColors);
    }

    /**
     * Sztabki i surowce używają tekstur sztabki/surowego żelaza z gry, przebarwionych na kolor metalu.
     * Dzięki temu wyglądają dokładnie jak przedmioty z Minecrafta (i pasują do pakietów tekstur).
     */
    private static final int TIN = 0xC8D7E6, SILVER = 0xF5F8FF;

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        tint(event, TIN, ModItems.TIN_INGOT.get(), ModItems.RAW_TIN.get(), ModItems.TIN_NUGGET.get(), ModBlocks.TIN_BLOCK.get());
        tint(event, SILVER, ModItems.SILVER_INGOT.get(), ModItems.RAW_SILVER.get(), ModItems.SILVER_NUGGET.get(), ModBlocks.SILVER_BLOCK.get());
        tint(event, 0xFF4A3C, ModItems.SIGNAL_ALLOY_INGOT.get());
        tint(event, 0x5A82FF, ModItems.POWER_ALLOY_INGOT.get());
        tint(event, 0xF2C75C, ModItems.BRASS_INGOT.get());
        tint(event, 0x8C949E, ModItems.STEEL_INGOT.get());
    }

    /** Bloki metali używają tekstury bloku żelaza z gry, przebarwionej tak samo jak sztabki. */
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        tintBlock(event, TIN, ModBlocks.TIN_BLOCK.get());
        tintBlock(event, SILVER, ModBlocks.SILVER_BLOCK.get());
    }

    private static void tintBlock(RegisterColorHandlersEvent.Block event, int rgb, net.minecraft.world.level.block.Block block) {
        final int argb = 0xFF000000 | rgb;
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? argb : 0xFFFFFFFF, block);
    }

    private static void tint(RegisterColorHandlersEvent.Item event, int rgb, ItemLike... items) {
        final int argb = 0xFF000000 | rgb;
        event.register((stack, tintIndex) -> tintIndex == 0 ? argb : 0xFFFFFFFF, items);
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
        Function<Material, TextureAtlasSprite> textures = event.getTextureGetter();
        replace(event, ModBlocks.SIGNAL_ALLOY_WIRE.get(),
                sprite(textures, "signal_alloy_off"), sprite(textures, "signal_alloy_on"));
        for (DyeColor color : DyeColor.values()) {
            replace(event, ModBlocks.INSULATED_WIRES.get(color.getId()).get(),
                    sprite(textures, "insulated_" + color.getName()), null);
        }
        replace(event, ModBlocks.BUNDLED_CABLE.get(), sprite(textures, "bundled_cable"), null);
        replace(event, ModBlocks.POWER_ALLOY_WIRE.get(), sprite(textures, "power_alloy_wire"), null);
    }

    private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> textures, String name) {
        return textures.apply(new Material(InventoryMenu.BLOCK_ATLAS,
                ResourceLocation.fromNamespaceAndPath(RedCircuit.MODID, "block/" + name)));
    }

    private static void replace(ModelEvent.ModifyBakingResult event, FaceWireBlock block,
                                TextureAtlasSprite sprite, @Nullable TextureAtlasSprite powered) {
        FaceWireModel model = new FaceWireModel(block, sprite, powered);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            event.getModels().put(BlockModelShaper.stateToModelLocation(state), model);
        }
    }
}
