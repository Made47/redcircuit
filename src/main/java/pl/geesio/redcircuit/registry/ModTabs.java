package pl.geesio.redcircuit.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.energy.BatteryItem;
import pl.geesio.redcircuit.tool.ElectricToolItem;

import java.util.Set;

/**
 * Zakładki trybu kreatywnego: Redstone, Logistyka i maszyny, Energia i płyny, Narzędzia, Materiały.
 * Przedmiot trafia do zakładki na podstawie swojego ID ({@link #categoryOf}); nowe przedmioty bez
 * dopasowania lądują w "Materiały", więc nic nie zginie.
 */
public final class ModTabs {
    private ModTabs() {}

    public enum Category { REDSTONE, LOGISTICS, ENERGY, TOOLS, MATERIALS }

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RedCircuit.MODID);

    // Zakładki RedCircuit stoją za wszystkimi zakładkami Minecrafta (zwykle na drugiej stronie).
    // "main" zostaje pierwszą zakładką (używa jej m.in. książka Patchouli).
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            tab("main", "itemGroup.redcircuit", "signal_alloy_wire", Category.REDSTONE, null);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LOGISTICS =
            tab("logistics", "itemGroup.redcircuit.logistics", "sorting_machine", Category.LOGISTICS, MAIN.getKey());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENERGY =
            tab("energy", "itemGroup.redcircuit.energy", "battery_box", Category.ENERGY, LOGISTICS.getKey());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS =
            tab("tools", "itemGroup.redcircuit.tools", "electric_drill", Category.TOOLS, ENERGY.getKey());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS =
            tab("materials", "itemGroup.redcircuit.materials", "signal_alloy_ingot", Category.MATERIALS, TOOLS.getKey());

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, String title, String icon,
                                                                        Category category,
                                                                        @org.jetbrains.annotations.Nullable net.minecraft.resources.ResourceKey<CreativeModeTab> before) {
        return TABS.register(id, () -> {
            CreativeModeTab.Builder b = CreativeModeTab.builder();
            // Każda zakładka moda za ostatnią zakładką gry (Jaja spawnu) i za poprzednią zakładką moda
            if (before != null) b.withTabsBefore(CreativeModeTabs.SPAWN_EGGS, before);
            else b.withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
            return b
                .title(Component.translatable(title))
                .icon(() -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RedCircuit.MODID, icon))))
                .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(h -> {
                    Item item = h.get();
                    if (categoryOf(h.getId().getPath()) != category) return;
                    output.accept(item);
                    if (item instanceof BatteryItem battery) output.accept(BatteryItem.charged(battery));
                    if (item instanceof ElectricToolItem tool) output.accept(ElectricToolItem.charged(tool));
                }))
                .build();
        });
    }

    private static final Set<String> LOGISTICS_IDS = Set.of("tube", "restriction_tube", "accelerator", "extractor", "filter",
            "sorting_machine", "requester", "manager", "regulator", "relay", "item_detector", "buffer",
            "igniter", "assembler", "ejector", "deployer", "block_breaker");
    private static final Set<String> ENERGY_IDS = Set.of("power_alloy_wire", "encased_power_alloy_wire", "solar_panel", "solid_fuel_generator",
            "thermopile", "generator", "windmill", "water_wheel", "battery_box", "battery", "charger",
            "electric_furnace", "macerator", "sawmill", "power_alloy_furnace", "power_circuit", "pump", "fluid_pipe", "fluid_tank");
    private static final Set<String> TOOLS_IDS = Set.of("screwdriver", "wire_cutters", "multimeter", "electric_drill",
            "electric_chainsaw", "alloy_furnace", "blueprint_table", "blank_plan", "plan", "wireless_remote");

    /** Kategoria przedmiotu na podstawie ID. */
    public static Category categoryOf(String id) {
        if (LOGISTICS_IDS.contains(id)) return Category.LOGISTICS;
        if (ENERGY_IDS.contains(id)) return Category.ENERGY;
        if (TOOLS_IDS.contains(id) || id.endsWith("_handsaw")) return Category.TOOLS;
        if (id.equals("signal_alloy_wire") || id.equals("encased_signal_alloy_wire") || id.equals("bundled_cable")
                || id.equals("encased_bundled_cable") || id.contains("insulated_wire") || id.endsWith("_gate")
                || id.contains("lamp_") || id.startsWith("wireless_")) return Category.REDSTONE;
        return Category.MATERIALS;
    }
}
