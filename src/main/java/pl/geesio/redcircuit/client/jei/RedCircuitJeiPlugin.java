package pl.geesio.redcircuit.client.jei;

import pl.geesio.redcircuit.client.ClientRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.AlloyRecipe;
import pl.geesio.redcircuit.alloy.ModRecipes;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.registry.ModItems;

import java.util.List;

/**
 * Integracja z JEI (Just Enough Items). Ładowana tylko, gdy JEI jest zainstalowane.
 * - kategoria "Piec do stopów" z recepturami redcircuit:alloying,
 * - piece / stół kreślarski jako katalizatory,
 * - strony informacyjne dla generatorów, wirników, pił i maszyn.
 */
@JeiPlugin
public class RedCircuitJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(RedCircuit.MODID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlloyRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new MaceratingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new SawingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AlloyRecipeCategory.TYPE, ClientRecipes.all(ModRecipes.ALLOYING.get()));
        registration.addRecipes(MaceratingRecipeCategory.TYPE, ClientRecipes.all(ModRecipes.MACERATING.get()));
        registration.addRecipes(SawingRecipeCategory.TYPE, ClientRecipes.all(ModRecipes.SAWING.get()));

        info(registration, ModBlocks.THERMOPILE.get(), "thermopile");
        info(registration, ModBlocks.MACERATOR.get(), "macerator");
        info(registration, ModBlocks.SAWMILL.get(), "sawmill");
        info(registration, ModBlocks.KINETIC_GENERATOR.get(), "generator");
        info(registration, ModItems.WINDMILL.get(), "windmill");
        info(registration, ModItems.WATER_WHEEL.get(), "water_wheel");
        info(registration, ModBlocks.SOLAR_PANEL.get(), "solar_panel");
        info(registration, ModBlocks.PUMP.get(), "pump");
        info(registration, ModBlocks.FLUID_PIPE.get(), "fluid_pipe");
        info(registration, ModBlocks.TUBE.get(), "tube");
        info(registration, ModBlocks.SORTING_MACHINE.get(), "sorting_machine");
        info(registration, ModBlocks.REQUESTER.get(), "requester");
        info(registration, ModBlocks.CHARGER.get(), "charger");
        info(registration, ModBlocks.SOLID_FUEL_GENERATOR.get(), "solid_fuel_generator");
        info(registration, ModItems.ELECTRIC_DRILL.get(), "electric_drill");
        info(registration, ModItems.ELECTRIC_CHAINSAW.get(), "electric_chainsaw");
        info(registration, ModItems.MULTIMETER.get(), "multimeter");
        info(registration, ModBlocks.FLUID_TANK.get(), "fluid_tank");
        info(registration, ModBlocks.WIRELESS_TRANSMITTER.get(), "wireless_transmitter");
        info(registration, ModBlocks.WIRELESS_RECEIVER.get(), "wireless_receiver");
        info(registration, ModItems.WIRELESS_REMOTE.get(), "wireless_remote");
        info(registration, ModBlocks.RESTRICTION_TUBE.get(), "restriction_tube");
        info(registration, ModBlocks.ACCELERATOR.get(), "accelerator");
        for (var g : java.util.List.of(pl.geesio.redcircuit.gate.GateType.COUNTER, pl.geesio.redcircuit.gate.GateType.SEQUENCER,
                pl.geesio.redcircuit.gate.GateType.STATE_CELL, pl.geesio.redcircuit.gate.GateType.SYNCHRONIZER)) {
            info(registration, ModBlocks.GATES.get(g).get(), g.getSerializedName());
        }
        for (var l : ModBlocks.LAMPS) info(registration, l.get(), "lamp");
        for (var l : ModBlocks.INVERTED_LAMPS) info(registration, l.get(), "lamp");
        info(registration, ModBlocks.MANAGER.get(), "manager");
        info(registration, ModBlocks.REGULATOR.get(), "regulator");
        info(registration, ModBlocks.RELAY.get(), "relay");
        info(registration, ModBlocks.ITEM_DETECTOR.get(), "item_detector");
        info(registration, ModBlocks.IGNITER.get(), "igniter");
        info(registration, ModBlocks.ASSEMBLER.get(), "assembler");
        info(registration, ModBlocks.EJECTOR.get(), "ejector");
        info(registration, ModBlocks.DEPLOYER.get(), "deployer");
        info(registration, ModBlocks.BUFFER.get(), "buffer");
        info(registration, ModBlocks.BLOCK_BREAKER.get(), "block_breaker");
        info(registration, ModBlocks.BLUEPRINT_TABLE.get(), "blueprint_table");
        info(registration, ModItems.BLANK_PLAN.get(), "blank_plan");
        info(registration, ModItems.SCREWDRIVER.get(), "screwdriver");
        info(registration, ModItems.WIRE_CUTTERS.get(), "wire_cutters");
        info(registration, ModItems.BATTERY.get(), "battery");
        info(registration, ModBlocks.ENCASED_SIGNAL_ALLOY_WIRE.get(), "encased_wire");
        for (var w : ModBlocks.ENCASED_INSULATED_WIRES) info(registration, w.get(), "encased_wire");
        info(registration, ModBlocks.ENCASED_BUNDLED_CABLE.get(), "encased_wire");
        info(registration, ModBlocks.ENCASED_POWER_ALLOY_WIRE.get(), "encased_wire");
        for (var saw : List.of(ModItems.STONE_HANDSAW, ModItems.COPPER_HANDSAW, ModItems.GOLD_HANDSAW,
                ModItems.IRON_HANDSAW, ModItems.SILVER_HANDSAW, ModItems.DIAMOND_HANDSAW, ModItems.NETHERITE_HANDSAW)) {
            info(registration, saw.get(), "handsaw");
        }
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addItemStackInfo(new ItemStack(item), Component.translatable("jei.redcircuit.info." + key));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AlloyRecipeCategory.TYPE, ModBlocks.ALLOY_FURNACE.get());
        registration.addCraftingStation(AlloyRecipeCategory.TYPE, ModBlocks.POWER_ALLOY_FURNACE.get());
        registration.addCraftingStation(RecipeTypes.SMELTING, ModBlocks.ELECTRIC_FURNACE.get());
        registration.addCraftingStation(MaceratingRecipeCategory.TYPE, ModBlocks.MACERATOR.get());
        registration.addCraftingStation(SawingRecipeCategory.TYPE, ModBlocks.SAWMILL.get());
        registration.addCraftingStation(RecipeTypes.CRAFTING, ModBlocks.BLUEPRINT_TABLE.get());
    }
}
