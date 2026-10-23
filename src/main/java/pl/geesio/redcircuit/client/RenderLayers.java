package pl.geesio.redcircuit.client;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import pl.geesio.redcircuit.RedCircuit;

/**
 * Warstwy renderowania bloków (Fabric ignoruje "render_type" z modeli JSON).
 * PLIK GENEROWANY przez tools/gen_fabric_layers.py - nie edytuj ręcznie.
 */
public final class RenderLayers {
    private RenderLayers() {}

    private static final String[] CUTOUT = {
            "accelerator",
            "blueprint_table",
            "encased_bundled_cable",
            "encased_insulated_wire_black",
            "encased_insulated_wire_blue",
            "encased_insulated_wire_brown",
            "encased_insulated_wire_cyan",
            "encased_insulated_wire_gray",
            "encased_insulated_wire_green",
            "encased_insulated_wire_light_blue",
            "encased_insulated_wire_light_gray",
            "encased_insulated_wire_lime",
            "encased_insulated_wire_magenta",
            "encased_insulated_wire_orange",
            "encased_insulated_wire_pink",
            "encased_insulated_wire_purple",
            "encased_insulated_wire_red",
            "encased_insulated_wire_white",
            "encased_insulated_wire_yellow",
            "encased_power_alloy_wire",
            "encased_signal_alloy_wire",
            "fluid_pipe",
            "fluid_tank",
            "restriction_tube",
            "tube"
    };

    private static final String[] CUTOUT_MIPPED = {
            "azurite_ore",
            "deepslate_azurite_ore",
            "deepslate_silver_ore",
            "deepslate_tin_ore",
            "silver_ore",
            "tin_ore"
    };

    private static final String[] TRANSLUCENT = {
            
    };

    public static void register() {
        put(CUTOUT, RenderType.cutout());
        put(CUTOUT_MIPPED, RenderType.cutoutMipped());
        put(TRANSLUCENT, RenderType.translucent());
    }

    private static void put(String[] ids, RenderType type) {
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(RedCircuit.MODID, id);
            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(b -> BlockRenderLayerMap.INSTANCE.putBlock(b, type));
        }
    }
}
