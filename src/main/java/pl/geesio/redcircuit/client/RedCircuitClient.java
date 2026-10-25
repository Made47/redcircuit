package pl.geesio.redcircuit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.registry.ModItems;
import pl.geesio.redcircuit.registry.ModMenus;
import pl.geesio.redcircuit.wire.FaceWireBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Kod tylko po stronie klienta (renderery, ekrany GUI, modele przewodów) - Fabric. */
public class RedCircuitClient implements ClientModInitializer {
    /**
     * Sztabki i surowce używają tekstur sztabki/surowego żelaza z gry, przebarwionych na kolor metalu
     * (przebarwienie przedmiotów jest w plikach assets/redcircuit/items/*.json - "tints").
     */
    private static final int TIN = 0xC8D7E6, SILVER = 0xF5F8FF;

    @Override
    public void onInitializeClient() {
        registerRenderers();
        registerScreens();
        registerColors();
        registerWireModels();
    }

    /** Bloki metali używają tekstury bloku żelaza z gry, przebarwionej tak samo jak sztabki. */
    private static void registerColors() {
        tintBlock(TIN, ModBlocks.TIN_BLOCK.get());
        tintBlock(SILVER, ModBlocks.SILVER_BLOCK.get());
    }

    private static void tintBlock(int rgb, Block block) {
        final int argb = 0xFF000000 | rgb;
        BlockColorRegistry.register(List.of(state -> argb), block);
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

    /** Podmienia modele przewodów na dynamiczne (przyklejane do ścian). */
    private static void registerWireModels() {
        Map<Block, String[]> wires = new HashMap<>();
        wires.put(ModBlocks.SIGNAL_ALLOY_WIRE.get(), new String[]{"signal_alloy_off", "signal_alloy_on"});
        for (DyeColor color : DyeColor.values()) {
            wires.put(ModBlocks.INSULATED_WIRES.get(color.getId()).get(), new String[]{"insulated_" + color.getName(), null});
        }
        wires.put(ModBlocks.BUNDLED_CABLE.get(), new String[]{"bundled_cable", null});
        wires.put(ModBlocks.POWER_ALLOY_WIRE.get(), new String[]{"power_alloy_wire", null});

        ModelLoadingPlugin.register(ctx -> {
            Map<Block, FaceWireModel> built = new HashMap<>();   // każde przeładowanie zasobów = nowe tekstury
            ctx.modifyBlockModelAfterBake().register((model, context) -> {
                Block block = context.state().getBlock();
                String[] ws = wires.get(block);
                if (ws == null || !(block instanceof FaceWireBlock wire)) return model;
                return built.computeIfAbsent(block, b -> {
                    MaterialBaker materials = context.baker().materials();
                    return new FaceWireModel(wire, material(materials, ws[0]), ws[1] == null ? null : material(materials, ws[1]));
                });
            });
        });
    }

    private static Material.Baked material(MaterialBaker materials, String name) {
        return materials.get(new Material(Identifier.fromNamespaceAndPath(RedCircuit.MODID, "block/" + name)), () -> "redcircuit:" + name);
    }
}
