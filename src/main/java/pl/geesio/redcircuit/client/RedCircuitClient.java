package pl.geesio.redcircuit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.registry.ModItems;
import pl.geesio.redcircuit.registry.ModMenus;
import pl.geesio.redcircuit.wire.FaceWireBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Kod tylko po stronie klienta (renderery, ekrany GUI, modele przewodów) - Fabric. */
public class RedCircuitClient implements ClientModInitializer {
    /**
     * Sztabki i surowce używają tekstur sztabki/surowego żelaza z gry, przebarwionych na kolor metalu.
     * Dzięki temu wyglądają dokładnie jak przedmioty z Minecrafta (i pasują do pakietów tekstur).
     */
    private static final int TIN = 0xC8D7E6, SILVER = 0xF5F8FF;

    @Override
    public void onInitializeClient() {
        RenderLayers.register();
        registerRenderers();
        registerScreens();
        registerColors();
        registerWireModels();
    }

    private static void registerColors() {
        tint(TIN, ModItems.TIN_INGOT.get(), ModItems.RAW_TIN.get(), ModItems.TIN_NUGGET.get(), ModBlocks.TIN_BLOCK.get());
        tint(SILVER, ModItems.SILVER_INGOT.get(), ModItems.RAW_SILVER.get(), ModItems.SILVER_NUGGET.get(), ModBlocks.SILVER_BLOCK.get());
        tint(0xFF4A3C, ModItems.SIGNAL_ALLOY_INGOT.get());
        tint(0x5A82FF, ModItems.POWER_ALLOY_INGOT.get());
        tint(0xF2C75C, ModItems.BRASS_INGOT.get());
        tint(0x8C949E, ModItems.STEEL_INGOT.get());
        // Bloki metali używają tekstury bloku żelaza z gry, przebarwionej tak samo jak sztabki.
        tintBlock(TIN, ModBlocks.TIN_BLOCK.get());
        tintBlock(SILVER, ModBlocks.SILVER_BLOCK.get());
    }

    private static void tintBlock(int rgb, Block block) {
        final int argb = 0xFF000000 | rgb;
        ColorProviderRegistry.BLOCK.register((state, level, pos, tintIndex) -> tintIndex == 0 ? argb : 0xFFFFFFFF, block);
    }

    private static void tint(int rgb, ItemLike... items) {
        final int argb = 0xFF000000 | rgb;
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0 ? argb : 0xFFFFFFFF, items);
    }

    private static void registerRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.TUBE.get(), TubeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ACCELERATOR.get(), TubeRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.KINETIC_GENERATOR.get(), RotorRenderer::new);
    }

    private static void registerScreens() {
        MenuScreens.register(ModMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        MenuScreens.register(ModMenus.MACERATOR.get(), MaceratorScreen::new);
        MenuScreens.register(ModMenus.SAWMILL.get(), SawmillScreen::new);
        MenuScreens.register(ModMenus.BATTERY_BOX.get(), BatteryBoxScreen::new);
        MenuScreens.register(ModMenus.CHARGER.get(), ChargerScreen::new);
        MenuScreens.register(ModMenus.SOLID_FUEL_GENERATOR.get(), FuelGeneratorScreen::new);
        MenuScreens.register(ModMenus.BLUEPRINT_TABLE.get(), BlueprintTableScreen::new);
        MenuScreens.register(ModMenus.ALLOY_FURNACE.get(), AlloyFurnaceScreen::new);
        MenuScreens.register(ModMenus.POWER_ALLOY_FURNACE.get(), AlloyFurnaceScreen::new);
    }

    /** Tekstury modeli przewodów: [zwykła, zasilona albo null]. */
    private record WireSprites(String normal, @Nullable String powered) {}

    /** Podmienia modele przewodów na dynamiczne (przyklejane do ścian). */
    private static void registerWireModels() {
        Map<FaceWireBlock, WireSprites> wires = new HashMap<>();
        wires.put(ModBlocks.SIGNAL_ALLOY_WIRE.get(), new WireSprites("signal_alloy_off", "signal_alloy_on"));
        for (DyeColor color : DyeColor.values()) {
            wires.put(ModBlocks.INSULATED_WIRES.get(color.getId()).get(), new WireSprites("insulated_" + color.getName(), null));
        }
        wires.put(ModBlocks.BUNDLED_CABLE.get(), new WireSprites("bundled_cable", null));
        wires.put(ModBlocks.POWER_ALLOY_WIRE.get(), new WireSprites("power_alloy_wire", null));

        Map<ModelResourceLocation, FaceWireBlock> byModel = new HashMap<>();
        for (FaceWireBlock block : wires.keySet()) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                byModel.put(BlockModelShaper.stateToModelLocation(state), block);
            }
        }
        Map<FaceWireBlock, FaceWireModel> built = new HashMap<>();

        ModelLoadingPlugin.register(ctx -> {
            built.clear();   // każde przeładowanie zasobów = nowe tekstury
            ctx.modifyModelAfterBake().register((model, context) -> {
            ModelResourceLocation id = context.topLevelId();
            if (id == null) return model;
            FaceWireBlock block = byModel.get(id);
            if (block == null) return model;
            return built.computeIfAbsent(block, b -> {
                WireSprites ws = wires.get(b);
                Function<Material, TextureAtlasSprite> textures = context.textureGetter();
                return new FaceWireModel(b, sprite(textures, ws.normal()), ws.powered() == null ? null : sprite(textures, ws.powered()));
            });
            });
        });
    }

    private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> textures, String name) {
        return textures.apply(new Material(InventoryMenu.BLOCK_ATLAS,
                ResourceLocation.fromNamespaceAndPath(RedCircuit.MODID, "block/" + name)));
    }
}
