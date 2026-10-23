package pl.geesio.redcircuit.config;

import pl.geesio.redcircuit.platform.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plik konfiguracji (config/redcircuit-common.toml) - dla twórców modpacków:
 * włączanie/wyłączanie rud oraz balans energii (produkcja generatorów, zużycie maszyn).
 */
public final class RedCircuitConfig {
    private RedCircuitConfig() {}

    public static final List<String> ORES = List.of(
            "tin", "silver", "azurite");

    public static final ModConfigSpec SPEC;
    private static final Map<String, ModConfigSpec.BooleanValue> ORE_ENABLED = new LinkedHashMap<>();

    public static final ModConfigSpec.IntValue SOLAR_FE;
    public static final ModConfigSpec.IntValue SOLID_FUEL_GENERATOR_FE;
    public static final ModConfigSpec.IntValue THERMOPILE_FE_PER_PAIR;
    public static final ModConfigSpec.IntValue WINDMILL_BASE_FE;
    public static final ModConfigSpec.IntValue WINDMILL_MAX_FE;
    public static final ModConfigSpec.IntValue WATER_WHEEL_FLOWING_FE;
    public static final ModConfigSpec.IntValue WATER_WHEEL_SOURCE_FE;
    public static final ModConfigSpec.IntValue WATER_WHEEL_MAX_FE;

    public static final ModConfigSpec.IntValue ELECTRIC_FURNACE_FE;
    public static final ModConfigSpec.IntValue MACERATOR_FE;
    public static final ModConfigSpec.IntValue SAWMILL_FE;
    public static final ModConfigSpec.IntValue POWER_ALLOY_FURNACE_FE;
    public static final ModConfigSpec.IntValue PUMP_FE_PER_BUCKET;
    public static final ModConfigSpec.IntValue REQUESTER_FE;
    public static final ModConfigSpec.IntValue MANAGER_FE;
    public static final ModConfigSpec.IntValue ACCELERATOR_FE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Generowanie rud w świecie / World generation").push("worldgen");
        for (String ore : ORES) {
            ORE_ENABLED.put(ore, b.comment("Generuj: " + ore + " (false = wyłączone, np. gdy inny mod dodaje tę rudę)")
                    .define("generate_" + ore, true));
        }
        b.pop();

        b.comment("Produkcja energii (FE/t) / Energy generation").push("generators");
        SOLAR_FE = b.comment("Panel słoneczny w dzień pod gołym niebem").defineInRange("solar_panel", 10, 0, 100_000);
        SOLID_FUEL_GENERATOR_FE = b.comment("Generator na paliwo stałe, FE/t podczas spalania (węgiel pali się 1600 ticków)").defineInRange("solid_fuel_generator", 20, 0, 100_000);
        THERMOPILE_FE_PER_PAIR = b.comment("Termostos: za każdą parę gorący+zimny sąsiad").defineInRange("thermopile_per_pair", 8, 0, 100_000);
        WINDMILL_BASE_FE = b.comment("Wiatrak na poziomie morza").defineInRange("windmill_base", 6, 0, 100_000);
        WINDMILL_MAX_FE = b.comment("Wiatrak - maksimum (wysoko)").defineInRange("windmill_max", 32, 0, 100_000);
        WATER_WHEEL_FLOWING_FE = b.comment("Koło wodne: za każdy blok płynącej wody").defineInRange("water_wheel_flowing", 4, 0, 100_000);
        WATER_WHEEL_SOURCE_FE = b.comment("Koło wodne: za każdy blok stojącej wody").defineInRange("water_wheel_source", 1, 0, 100_000);
        WATER_WHEEL_MAX_FE = b.comment("Koło wodne - maksimum").defineInRange("water_wheel_max", 24, 0, 100_000);
        b.pop();

        b.comment("Zużycie energii przez maszyny / Machine energy use").push("machines");
        ELECTRIC_FURNACE_FE = b.comment("Piec elektryczny, FE/t").defineInRange("electric_furnace_per_tick", 20, 0, 100_000);
        MACERATOR_FE = b.comment("Macerator, FE/t").defineInRange("macerator_per_tick", 16, 0, 100_000);
        SAWMILL_FE = b.comment("Tartak, FE/t").defineInRange("sawmill_per_tick", 12, 0, 100_000);
        POWER_ALLOY_FURNACE_FE = b.comment("Elektryczny piec do stopów, FE/t").defineInRange("power_alloy_furnace_per_tick", 25, 0, 100_000);
        PUMP_FE_PER_BUCKET = b.comment("Pompa, FE za wiadro (1000 mB)").defineInRange("pump_per_bucket", 200, 0, 1_000_000);
        REQUESTER_FE = b.comment("Requester, FE za pobranie").defineInRange("requester_per_pull", 200, 0, 10_000);
        MANAGER_FE = b.comment("Manager, FE za zamówienie").defineInRange("manager_per_order", 100, 0, 10_000);
        ACCELERATOR_FE = b.comment("Akcelerator, FE za przyspieszenie jednego stosu").defineInRange("accelerator_per_item", 20, 0, 10_000);
        b.pop();

        SPEC = b.build();
    }

    public static boolean oreEnabled(String ore) {
        ModConfigSpec.BooleanValue v = ORE_ENABLED.get(ore);
        return v == null || v.get();
    }
}
