package pl.geesio.redcircuit.item;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import pl.geesio.redcircuit.RedCircuit;

/** Przedmioty moda (rejestrowane w preInit). */
public final class ModItems {
    private ModItems() {}

    /** Kolory metali (przebarwienie tekstur z gry). */
    public static final int COPPER = 0xE8915A, TIN = 0xC8D7E6, SILVER = 0xF5F8FF, BRASS = 0xF2C75C, STEEL = 0x8C949E,
            SIGNAL = 0xFF4A3C, POWER = 0x5A82FF;

    public static Item copperIngot, tinIngot, silverIngot, brassIngot, steelIngot, signalAlloyIngot, powerAlloyIngot;
    public static Item copperNugget, tinNugget, silverNugget;
    public static Item ironDust, goldDust, copperDust, tinDust, silverDust, azurite;
    public static Item stoneGear, copperGear, goldGear, ironGear, brassGear, silverGear, diamondGear;
    public static Item circuitPlate, screwdriver;
    public static Item wirelessRemote;
    public static Item signalCircuit;
    public static Item battery, windmill, waterWheel, sawdust, fineCopperWire, fineIronWire, copperCoil, powerCircuit, wireCutters;
    public static Item electricDrill, electricChainsaw, multimeter, blankPlan, plan;
    public static Item stoneHandsaw, copperHandsaw, goldHandsaw, ironHandsaw, silverHandsaw, diamondHandsaw;

    public static void register() {
        copperIngot = ingot("copper_ingot", COPPER);
        tinIngot = ingot("tin_ingot", TIN);
        silverIngot = ingot("silver_ingot", SILVER);
        brassIngot = ingot("brass_ingot", BRASS);
        steelIngot = ingot("steel_ingot", STEEL);
        signalAlloyIngot = ingot("signal_alloy_ingot", SIGNAL);
        powerAlloyIngot = ingot("power_alloy_ingot", POWER);

        copperNugget = nugget("copper_nugget", COPPER);
        tinNugget = nugget("tin_nugget", TIN);
        silverNugget = nugget("silver_nugget", SILVER);

        ironDust = simple("iron_dust");
        goldDust = simple("gold_dust");
        copperDust = simple("copper_dust");
        tinDust = simple("tin_dust");
        silverDust = simple("silver_dust");
        azurite = simple("azurite");

        stoneGear = simple("stone_gear");
        copperGear = simple("copper_gear");
        goldGear = simple("gold_gear");
        ironGear = simple("iron_gear");
        brassGear = simple("brass_gear");
        silverGear = simple("silver_gear");
        diamondGear = simple("diamond_gear");

        // Etap 4: bramki
        circuitPlate = simple("circuit_plate");
        screwdriver = reg(new pl.geesio.redcircuit.tool.ItemScrewdriver(), "screwdriver");

        // Etap 5: pilot bezprzewodowy
        wirelessRemote = reg(new pl.geesio.redcircuit.wireless.ItemWirelessRemote(), "wireless_remote");

        // Etap 6: obwód sygnałowy (korpus maszyn logistycznych)
        signalCircuit = simple("signal_circuit");

        // Etap 7: energia
        battery = reg(new pl.geesio.redcircuit.energy.ItemBattery(), "battery");
        windmill = simple("windmill");
        windmill.setMaxStackSize(1);
        waterWheel = simple("water_wheel");
        waterWheel.setMaxStackSize(1);
        sawdust = simple("sawdust");
        fineCopperWire = simple("fine_copper_wire");
        fineIronWire = simple("fine_iron_wire");
        copperCoil = simple("copper_coil");
        powerCircuit = simple("power_circuit");
        wireCutters = reg(new ItemCraftingTool("wire_cutters", 256, "ingotSteel"), "wire_cutters");
        stoneHandsaw = reg(new ItemCraftingTool("stone_handsaw", 131, "cobblestone"), "stone_handsaw");
        copperHandsaw = reg(new ItemCraftingTool("copper_handsaw", 190, "ingotCopper"), "copper_handsaw");
        goldHandsaw = reg(new ItemCraftingTool("gold_handsaw", 32, "ingotGold"), "gold_handsaw");
        ironHandsaw = reg(new ItemCraftingTool("iron_handsaw", 250, "ingotIron"), "iron_handsaw");
        silverHandsaw = reg(new ItemCraftingTool("silver_handsaw", 200, "ingotSilver"), "silver_handsaw");
        diamondHandsaw = reg(new ItemCraftingTool("diamond_handsaw", 1561, "gemDiamond"), "diamond_handsaw");

        // Etap 9: narzędzia, plany
        electricDrill = reg(new pl.geesio.redcircuit.tool.ItemElectricTool("electric_drill", true), "electric_drill");
        electricChainsaw = reg(new pl.geesio.redcircuit.tool.ItemElectricTool("electric_chainsaw", false), "electric_chainsaw");
        multimeter = reg(new pl.geesio.redcircuit.tool.ItemMultimeter(), "multimeter");
        blankPlan = reg(new pl.geesio.redcircuit.craft.ItemPlan(true), "blank_plan");
        plan = reg(new pl.geesio.redcircuit.craft.ItemPlan(false), "plan");
    }

    private static Item ingot(String name, int color) {
        return reg(new ItemMaterial(name, "iron_ingot", color), name);
    }

    private static Item nugget(String name, int color) {
        return reg(new ItemMaterial(name, RedCircuit.MODID + ":nugget_base", color), name);
    }

    private static Item simple(String name) {
        return reg(new ItemMaterial(name), name);
    }

    private static Item reg(Item item, String name) {
        GameRegistry.registerItem(item, name);
        return item;
    }
}
