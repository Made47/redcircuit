package pl.geesio.redcircuit.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/** Plik konfiguracji config/redcircuit.cfg (Forge Configuration). */
public final class RCConfig {
    private RCConfig() {}

    public static boolean genCopper = true, genTin = true, genSilver = true, genAzurite = true;
    public static int copperVeins = 12, tinVeins = 10, silverVeins = 4, azuriteVeins = 6;

    /** Produkcja energii (RF/t) i zużycie maszyn - jak w konfiguracji wersji 1.21. */
    public static int solarRf = 10, fuelGeneratorRf = 20, thermopileRfPerPair = 8, windmillBaseRf = 6, windmillMaxRf = 32,
            waterWheelFlowingRf = 4, waterWheelSourceRf = 1, waterWheelMaxRf = 24;
    public static int electricFurnaceRf = 20, maceratorRf = 16, sawmillRf = 12, powerAlloyFurnaceRf = 25, pumpRfPerBucket = 200,
            requesterRf = 200, managerRf = 100, acceleratorRf = 20;

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        cfg.load();
        String w = "worldgen";
        cfg.setCategoryComment(w, "Generowanie rud w świecie / World generation (false = wyłączone, np. gdy inny mod dodaje tę rudę)");
        genCopper = cfg.get(w, "generate_copper", true, "Ruda miedzi / copper ore").getBoolean(true);
        genTin = cfg.get(w, "generate_tin", true, "Ruda cyny / tin ore").getBoolean(true);
        genSilver = cfg.get(w, "generate_silver", true, "Ruda srebra / silver ore").getBoolean(true);
        genAzurite = cfg.get(w, "generate_azurite", true, "Ruda azurytu / azurite ore").getBoolean(true);
        copperVeins = cfg.get(w, "copper_veins_per_chunk", 12, "Żyły miedzi na chunk", 0, 64).getInt(12);
        tinVeins = cfg.get(w, "tin_veins_per_chunk", 10, "Żyły cyny na chunk", 0, 64).getInt(10);
        silverVeins = cfg.get(w, "silver_veins_per_chunk", 4, "Żyły srebra na chunk", 0, 64).getInt(4);
        azuriteVeins = cfg.get(w, "azurite_veins_per_chunk", 6, "Żyły azurytu na chunk", 0, 64).getInt(6);

        String g = "generators";
        cfg.setCategoryComment(g, "Produkcja energii (RF/t) / Energy generation");
        solarRf = cfg.get(g, "solar_panel", 10, "Panel słoneczny w dzień pod gołym niebem", 0, 100000).getInt();
        fuelGeneratorRf = cfg.get(g, "solid_fuel_generator", 20, "Generator na paliwo stałe, RF/t podczas spalania", 0, 100000).getInt();
        thermopileRfPerPair = cfg.get(g, "thermopile_per_pair", 8, "Termostos: za każdą parę gorący+zimny sąsiad", 0, 100000).getInt();
        windmillBaseRf = cfg.get(g, "windmill_base", 6, "Wiatrak na poziomie morza", 0, 100000).getInt();
        windmillMaxRf = cfg.get(g, "windmill_max", 32, "Wiatrak - maksimum (wysoko)", 0, 100000).getInt();
        waterWheelFlowingRf = cfg.get(g, "water_wheel_flowing", 4, "Koło wodne: za każdy blok płynącej wody", 0, 100000).getInt();
        waterWheelSourceRf = cfg.get(g, "water_wheel_source", 1, "Koło wodne: za każdy blok stojącej wody", 0, 100000).getInt();
        waterWheelMaxRf = cfg.get(g, "water_wheel_max", 24, "Koło wodne - maksimum", 0, 100000).getInt();

        String m = "machines";
        cfg.setCategoryComment(m, "Zużycie energii przez maszyny / Machine energy use");
        electricFurnaceRf = cfg.get(m, "electric_furnace_per_tick", 20, "Piec elektryczny, RF/t", 0, 100000).getInt();
        maceratorRf = cfg.get(m, "macerator_per_tick", 16, "Macerator, RF/t", 0, 100000).getInt();
        sawmillRf = cfg.get(m, "sawmill_per_tick", 12, "Tartak, RF/t", 0, 100000).getInt();
        powerAlloyFurnaceRf = cfg.get(m, "power_alloy_furnace_per_tick", 25, "Elektryczny piec do stopów, RF/t", 0, 100000).getInt();
        pumpRfPerBucket = cfg.get(m, "pump_per_bucket", 200, "Pompa, RF za wiadro (1000 mB)", 0, 1000000).getInt();
        requesterRf = cfg.get(m, "requester_per_pull", 200, "Requester, RF za pobranie", 0, 10000).getInt();
        managerRf = cfg.get(m, "manager_per_order", 100, "Manager, RF za zamówienie", 0, 10000).getInt();
        acceleratorRf = cfg.get(m, "accelerator_per_item", 20, "Akcelerator, RF za przyspieszenie jednego stosu", 0, 10000).getInt();
        if (cfg.hasChanged()) cfg.save();
    }
}
