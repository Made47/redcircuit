package pl.geesio.redcircuit.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.AlloyFurnaceMenu;
import pl.geesio.redcircuit.craft.BlueprintTableMenu;
import pl.geesio.redcircuit.energy.BatteryBoxMenu;
import pl.geesio.redcircuit.energy.ElectricFurnaceMenu;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RedCircuit.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<pl.geesio.redcircuit.energy.SawmillMenu>> SAWMILL =
            MENUS.register("sawmill", () -> new MenuType<>(pl.geesio.redcircuit.energy.SawmillMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<pl.geesio.redcircuit.energy.MaceratorMenu>> MACERATOR =
            MENUS.register("macerator", () -> new MenuType<>(pl.geesio.redcircuit.energy.MaceratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
            MENUS.register("electric_furnace", () -> new MenuType<>(ElectricFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<AlloyFurnaceMenu>> ALLOY_FURNACE =
            MENUS.register("alloy_furnace", () -> new MenuType<>(AlloyFurnaceMenu::clientFuel, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<AlloyFurnaceMenu>> POWER_ALLOY_FURNACE =
            MENUS.register("power_alloy_furnace", () -> new MenuType<>(AlloyFurnaceMenu::clientElectric, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<BatteryBoxMenu>> BATTERY_BOX =
            MENUS.register("battery_box", () -> new MenuType<>(BatteryBoxMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<pl.geesio.redcircuit.energy.FuelGeneratorMenu>> SOLID_FUEL_GENERATOR =
            MENUS.register("solid_fuel_generator", () -> new MenuType<>(pl.geesio.redcircuit.energy.FuelGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<pl.geesio.redcircuit.energy.ChargerMenu>> CHARGER =
            MENUS.register("charger", () -> new MenuType<>(pl.geesio.redcircuit.energy.ChargerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<BlueprintTableMenu>> BLUEPRINT_TABLE =
            MENUS.register("blueprint_table", () -> new MenuType<>(BlueprintTableMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
