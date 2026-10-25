package pl.geesio.redcircuit.client.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import pl.geesio.redcircuit.compat.rei.AlloyDisplay;
import pl.geesio.redcircuit.compat.rei.ProcessingDisplay;
import pl.geesio.redcircuit.compat.rei.ReiIds;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.registry.ModItems;

import java.util.List;

/**
 * Integracja z REI (Roughly Enough Items) - Fabric; na NeoForge jest JEI. Ładowana tylko, gdy REI jest zainstalowane.
 * - kategorie: piec do stopów, macerator, tartak (wyświetlenia buduje serwer - RedCircuitReiCommonPlugin),
 * - piece / stół kreślarski jako stanowiska,
 * - strony informacyjne dla generatorów, wirników, pił i maszyn.
 */
public class RedCircuitReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<AlloyDisplay> ALLOYING = ReiIds.ALLOYING;
    public static final CategoryIdentifier<ProcessingDisplay> MACERATING = ReiIds.MACERATING;
    public static final CategoryIdentifier<ProcessingDisplay> SAWING = ReiIds.SAWING;

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new AlloyCategory());
        registry.add(new ProcessingCategory(MACERATING, ModBlocks.MACERATOR.get(), "block.redcircuit.macerator"));
        registry.add(new ProcessingCategory(SAWING, ModBlocks.SAWMILL.get(), "block.redcircuit.sawmill"));

        registry.addWorkstations(ALLOYING, EntryStacks.of(ModBlocks.ALLOY_FURNACE.get()), EntryStacks.of(ModBlocks.POWER_ALLOY_FURNACE.get()));
        registry.addWorkstations(MACERATING, EntryStacks.of(ModBlocks.MACERATOR.get()));
        registry.addWorkstations(SAWING, EntryStacks.of(ModBlocks.SAWMILL.get()));
        registry.addWorkstations(BuiltinPlugin.SMELTING, EntryStacks.of(ModBlocks.ELECTRIC_FURNACE.get()));
        registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(ModBlocks.BLUEPRINT_TABLE.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        info(registry, ModBlocks.THERMOPILE.get(), "thermopile");
        info(registry, ModBlocks.MACERATOR.get(), "macerator");
        info(registry, ModBlocks.SAWMILL.get(), "sawmill");
        info(registry, ModBlocks.KINETIC_GENERATOR.get(), "generator");
        info(registry, ModItems.WINDMILL.get(), "windmill");
        info(registry, ModItems.WATER_WHEEL.get(), "water_wheel");
        info(registry, ModBlocks.SOLAR_PANEL.get(), "solar_panel");
        info(registry, ModBlocks.PUMP.get(), "pump");
        info(registry, ModBlocks.FLUID_PIPE.get(), "fluid_pipe");
        info(registry, ModBlocks.TUBE.get(), "tube");
        info(registry, ModBlocks.SORTING_MACHINE.get(), "sorting_machine");
        info(registry, ModBlocks.REQUESTER.get(), "requester");
        info(registry, ModBlocks.CHARGER.get(), "charger");
        info(registry, ModBlocks.SOLID_FUEL_GENERATOR.get(), "solid_fuel_generator");
        info(registry, ModItems.ELECTRIC_DRILL.get(), "electric_drill");
        info(registry, ModItems.ELECTRIC_CHAINSAW.get(), "electric_chainsaw");
        info(registry, ModItems.MULTIMETER.get(), "multimeter");
        info(registry, ModBlocks.FLUID_TANK.get(), "fluid_tank");
        info(registry, ModBlocks.WIRELESS_TRANSMITTER.get(), "wireless_transmitter");
        info(registry, ModBlocks.WIRELESS_RECEIVER.get(), "wireless_receiver");
        info(registry, ModItems.WIRELESS_REMOTE.get(), "wireless_remote");
        info(registry, ModBlocks.RESTRICTION_TUBE.get(), "restriction_tube");
        info(registry, ModBlocks.ACCELERATOR.get(), "accelerator");
        for (GateType g : List.of(GateType.COUNTER, GateType.SEQUENCER, GateType.STATE_CELL, GateType.SYNCHRONIZER)) {
            info(registry, ModBlocks.GATES.get(g).get(), g.getSerializedName());
        }
        for (var l : ModBlocks.LAMPS) info(registry, l.get(), "lamp");
        for (var l : ModBlocks.INVERTED_LAMPS) info(registry, l.get(), "lamp");
        info(registry, ModBlocks.MANAGER.get(), "manager");
        info(registry, ModBlocks.REGULATOR.get(), "regulator");
        info(registry, ModBlocks.RELAY.get(), "relay");
        info(registry, ModBlocks.ITEM_DETECTOR.get(), "item_detector");
        info(registry, ModBlocks.IGNITER.get(), "igniter");
        info(registry, ModBlocks.ASSEMBLER.get(), "assembler");
        info(registry, ModBlocks.EJECTOR.get(), "ejector");
        info(registry, ModBlocks.DEPLOYER.get(), "deployer");
        info(registry, ModBlocks.BUFFER.get(), "buffer");
        info(registry, ModBlocks.BLOCK_BREAKER.get(), "block_breaker");
        info(registry, ModBlocks.BLUEPRINT_TABLE.get(), "blueprint_table");
        info(registry, ModItems.BLANK_PLAN.get(), "blank_plan");
        info(registry, ModItems.SCREWDRIVER.get(), "screwdriver");
        info(registry, ModItems.WIRE_CUTTERS.get(), "wire_cutters");
        info(registry, ModItems.BATTERY.get(), "battery");
        info(registry, ModBlocks.ENCASED_SIGNAL_ALLOY_WIRE.get(), "encased_wire");
        for (var w : ModBlocks.ENCASED_INSULATED_WIRES) info(registry, w.get(), "encased_wire");
        info(registry, ModBlocks.ENCASED_BUNDLED_CABLE.get(), "encased_wire");
        info(registry, ModBlocks.ENCASED_POWER_ALLOY_WIRE.get(), "encased_wire");
        for (var saw : List.of(ModItems.STONE_HANDSAW, ModItems.COPPER_HANDSAW, ModItems.GOLD_HANDSAW,
                ModItems.IRON_HANDSAW, ModItems.SILVER_HANDSAW, ModItems.DIAMOND_HANDSAW, ModItems.NETHERITE_HANDSAW)) {
            info(registry, saw.get(), "handsaw");
        }
    }

    /** Strona informacyjna (teksty z kluczy jei.redcircuit.info.* - wspólne z wersją NeoForge). */
    private static void info(DisplayRegistry registry, ItemLike item, String key) {
        registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(item), new net.minecraft.world.item.ItemStack(item).getHoverName())
                .line(Component.translatable("jei.redcircuit.info." + key)));
    }
}
