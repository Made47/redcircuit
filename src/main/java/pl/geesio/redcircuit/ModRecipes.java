package pl.geesio.redcircuit;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import pl.geesio.redcircuit.alloy.AlloyRecipes;
import pl.geesio.redcircuit.block.BlockOre;
import pl.geesio.redcircuit.block.BlockStorage;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.gate.GateType;

import static pl.geesio.redcircuit.item.ModItems.*;

/** Słownik rud (OreDictionary - odpowiednik tagów c:) i receptury. */
public final class ModRecipes {
    private ModRecipes() {}

    public static void registerOreDictionary() {
        ore("oreCopper", new ItemStack(ModBlocks.ore, 1, BlockOre.COPPER));
        ore("oreTin", new ItemStack(ModBlocks.ore, 1, BlockOre.TIN));
        ore("oreSilver", new ItemStack(ModBlocks.ore, 1, BlockOre.SILVER));
        ore("oreAzurite", new ItemStack(ModBlocks.ore, 1, BlockOre.AZURITE));

        ore("blockCopper", new ItemStack(ModBlocks.storage, 1, BlockStorage.COPPER));
        ore("blockTin", new ItemStack(ModBlocks.storage, 1, BlockStorage.TIN));
        ore("blockSilver", new ItemStack(ModBlocks.storage, 1, BlockStorage.SILVER));
        ore("blockAzurite", new ItemStack(ModBlocks.storage, 1, BlockStorage.AZURITE));
        ore("blockBrass", new ItemStack(ModBlocks.storage, 1, BlockStorage.BRASS));
        ore("blockSteel", new ItemStack(ModBlocks.storage, 1, BlockStorage.STEEL));

        ore("ingotCopper", copperIngot);
        ore("ingotTin", tinIngot);
        ore("ingotSilver", silverIngot);
        ore("ingotBrass", brassIngot);
        ore("ingotSteel", steelIngot);
        ore("ingotSignalAlloy", signalAlloyIngot);
        ore("ingotPowerAlloy", powerAlloyIngot);

        ore("nuggetCopper", copperNugget);
        ore("nuggetTin", tinNugget);
        ore("nuggetSilver", silverNugget);

        ore("dustIron", ironDust);
        ore("dustGold", goldDust);
        ore("dustCopper", copperDust);
        ore("dustTin", tinDust);
        ore("dustSilver", silverDust);
        ore("dustAzurite", azurite);

        ore("gearStone", stoneGear);
        ore("gearCopper", copperGear);
        ore("gearGold", goldGear);
        ore("gearIron", ironGear);
        ore("gearBrass", brassGear);
        ore("gearSilver", silverGear);
        ore("gearDiamond", diamondGear);
    }

    public static void register() {
        // Metale: blok <-> 9 sztabek, sztabka <-> 9 bryłek
        storage(BlockStorage.COPPER, "ingotCopper", copperIngot);
        storage(BlockStorage.TIN, "ingotTin", tinIngot);
        storage(BlockStorage.SILVER, "ingotSilver", silverIngot);
        storage(BlockStorage.AZURITE, "dustAzurite", azurite);
        storage(BlockStorage.BRASS, "ingotBrass", brassIngot);
        storage(BlockStorage.STEEL, "ingotSteel", steelIngot);
        nuggets("nuggetCopper", copperNugget, "ingotCopper", copperIngot);
        nuggets("nuggetTin", tinNugget, "ingotTin", tinIngot);
        nuggets("nuggetSilver", silverNugget, "ingotSilver", silverIngot);

        // Przetapianie rud i pyłów
        smelt(new ItemStack(ModBlocks.ore, 1, BlockOre.COPPER), copperIngot, 0.7F);
        smelt(new ItemStack(ModBlocks.ore, 1, BlockOre.TIN), tinIngot, 0.7F);
        smelt(new ItemStack(ModBlocks.ore, 1, BlockOre.SILVER), silverIngot, 1.0F);
        GameRegistry.addSmelting(new ItemStack(ModBlocks.ore, 1, BlockOre.AZURITE), new ItemStack(azurite, 4), 0.7F);
        smelt(new ItemStack(ironDust), Items.iron_ingot, 0.1F);
        smelt(new ItemStack(goldDust), Items.gold_ingot, 0.1F);
        smelt(new ItemStack(copperDust), copperIngot, 0.1F);
        smelt(new ItemStack(tinDust), tinIngot, 0.1F);
        smelt(new ItemStack(silverDust), silverIngot, 0.1F);

        // Zębatki
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(stoneGear), " M ", "M M", " M ", 'M', "cobblestone"));
        gear(copperGear, "ingotCopper");
        gear(goldGear, "ingotGold");
        gear(ironGear, "ingotIron");
        gear(brassGear, "ingotBrass");
        gear(silverGear, "ingotSilver");
        gear(diamondGear, "gemDiamond");

        // Piec do stopów: 8 cegieł
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ModBlocks.alloyFurnace), "BBB", "B B", "BBB", 'B', Blocks.brick_block));

        AlloyRecipes.registerDefaults();
        registerWires();
        registerGates();
        registerLampsAndWireless();
        registerLogistics();
        registerEnergy();
        pl.geesio.redcircuit.energy.ProcessingRecipes.register();
    }

    /** Przewody: stop sygnałowy, izolowane (wełna + 4 przewody), kabel wiązkowy, wersje w obudowie. */
    /** Płytka obwodu, śrubokręt i bramki (przepisy bezkształtne jak w 1.21). */
    private static void registerGates() {
        GameRegistry.addShapelessRecipe(new ItemStack(circuitPlate, 2), new ItemStack(Blocks.stone_slab, 1, 0), Items.redstone);
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(screwdriver), " I", "S ", 'I', "ingotIron", 'S', "stickWood"));

        Object P = circuitPlate, T = Blocks.redstone_torch, W = ModBlocks.signalAlloyWire, R = Items.redstone;
        Object lever = Blocks.lever, repeater = Items.repeater, clock = Items.clock;
        gate(GateType.NOT, P, T);
        gate(GateType.BUFFER, P, T, T);
        gate(GateType.AND, P, T, T, T);
        gate(GateType.NAND, P, T, T, T, R);
        gate(GateType.OR, P, W, W);
        gate(GateType.NOR, P, W, W, T);
        gate(GateType.XOR, P, W, W, T, T);
        gate(GateType.XNOR, P, W, W, T, T, T);
        gate(GateType.RS_LATCH, P, lever, T, T);
        gate(GateType.TOGGLE, P, lever, W);
        gate(GateType.PULSE, P, repeater, T);
        gate(GateType.TIMER, P, clock);
        gate(GateType.RANDOMIZER, P, Items.quartz);
        gate(GateType.COUNTER, P, Items.comparator, T, W);
        gate(GateType.SEQUENCER, P, clock, W, W);
        gate(GateType.STATE_CELL, P, clock, lever);
        gate(GateType.SYNCHRONIZER, P, repeater, W, W);
    }

    private static final String[] DYES = {"dyeWhite", "dyeOrange", "dyeMagenta", "dyeLightBlue", "dyeYellow", "dyeLime",
            "dyePink", "dyeGray", "dyeLightGray", "dyeCyan", "dyePurple", "dyeBlue", "dyeBrown", "dyeGreen", "dyeRed", "dyeBlack"};

    /** Lampy (szyby, jasnopył, barwnik, redstone), zamiana zwykła <-> odwrócona pochodnią, nadajnik, odbiornik, pilot. */
    private static void registerLampsAndWireless() {
        for (int c = 0; c < 16; c++) {
            GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ModBlocks.lamp, 1, c), "GSG", "GDG", "GRG",
                    'G', "paneGlass", 'S', Items.glowstone_dust, 'D', DYES[c], 'R', Items.redstone));
            GameRegistry.addShapelessRecipe(new ItemStack(ModBlocks.invertedLamp, 1, c), new ItemStack(ModBlocks.lamp, 1, c), Blocks.redstone_torch);
            GameRegistry.addShapelessRecipe(new ItemStack(ModBlocks.lamp, 1, c), new ItemStack(ModBlocks.invertedLamp, 1, c), Blocks.redstone_torch);
        }
        Object P = circuitPlate, W = ModBlocks.signalAlloyWire;
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.wirelessTransmitter),
                P, W, Items.ender_pearl, "ingotGold", Blocks.redstone_torch));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.wirelessReceiver),
                P, W, Items.ender_pearl, "ingotGold", Items.comparator));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(wirelessRemote),
                ModBlocks.wirelessTransmitter, Blocks.stone_button, "ingotIron"));
    }

    /** Rury, ekstraktor, filtr, maszyna sortująca, obwód sygnałowy, obudowa i maszyny logistyczne (jak w 1.21). */
    private static void registerLogistics() {
        Object I = "ingotIron", W = ModBlocks.signalAlloyWire;
        net.minecraft.block.Block casing = ModBlocks.signalMachineCasing;
        add(new ItemStack(ModBlocks.tube, 8), "CGC", 'C', "ingotCopper", 'G', "blockGlass");
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.restrictionTube), ModBlocks.tube, "ingotIron"));
        GameRegistry.addShapelessRecipe(new ItemStack(ModBlocks.tube), ModBlocks.restrictionTube);

        add(new ItemStack(signalCircuit), "ISI", "TPT", "IWI", 'I', I, 'S', "ingotSignalAlloy", 'T', Blocks.redstone_torch,
                'P', circuitPlate, 'W', W);
        add(new ItemStack(casing), "ITI", "TCT", "ITI", 'I', I, 'T', "ingotTin", 'C', signalCircuit);

        add(new ItemStack(ModBlocks.extractor), "PPP", "CTC", "CWC", 'P', "plankWood", 'C', "cobblestone", 'T', Blocks.piston, 'W', casing);
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.filter), ModBlocks.extractor, Blocks.hopper, "ingotSignalAlloy"));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.sortingMachine), ModBlocks.filter, Blocks.chest,
                Items.comparator, "ingotSignalAlloy", "ingotSignalAlloy"));

        add(new ItemStack(ModBlocks.regulator), "IFI", "TCT", "IWI", 'I', I, 'F', ModBlocks.filter, 'T', ModBlocks.extractor,
                'C', Items.comparator, 'W', casing);
        add(new ItemStack(ModBlocks.relay), "ICI", "IHI", "IWI", 'I', I, 'C', Blocks.chest, 'H', Blocks.hopper, 'W', casing);
        add(new ItemStack(ModBlocks.ejector), "IDI", "IHI", "IWI", 'I', I, 'D', Blocks.dropper, 'H', Blocks.hopper, 'W', casing);
        // W 1.7.10 nie ma obserwatora - zamiast niego komparator
        add(new ItemStack(ModBlocks.itemDetector), "IPI", "GOG", "IWI", 'I', I, 'P', Blocks.stone_pressure_plate,
                'G', "blockGlass", 'O', Items.comparator, 'W', casing);
        add(new ItemStack(ModBlocks.igniter), "NNN", "IFI", "IWI", 'N', Blocks.netherrack, 'I', I, 'F', Items.flint_and_steel, 'W', casing);
        add(new ItemStack(ModBlocks.assembler), "IPI", "CMC", "IBI", 'I', I, 'P', Blocks.piston, 'C', Blocks.chest, 'M', casing,
                'B', ModBlocks.bundledCable);
        add(new ItemStack(ModBlocks.deployer), "SPS", "SHS", "SWS", 'S', "cobblestone", 'P', Blocks.piston, 'H', Blocks.chest, 'W', casing);
        add(new ItemStack(ModBlocks.blockBreaker), "SPS", "SKS", "SWS", 'S', "cobblestone", 'P', Blocks.piston, 'K', Items.iron_pickaxe,
                'W', casing);
        add(new ItemStack(ModBlocks.buffer), "ICI", "C C", "ICI", 'I', I, 'C', Blocks.chest);
    }

    /** Narzędzie z wytrzymałością jako składnik (dowolne zużycie). */
    private static ItemStack tool(Item item) {
        return new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE);
    }

    /** Energia RF: przewody, akumulator, generatory, maszyny, narzędzia wytwórcze (jak w 1.21). */
    private static void registerEnergy() {
        Object I = "ingotIron", B = "ingotPowerAlloy";
        net.minecraft.block.Block AW = ModBlocks.powerAlloyWire, PC = ModBlocks.powerMachineCasing;
        for (Item saw : new Item[]{stoneHandsaw, copperHandsaw, goldHandsaw, ironHandsaw, silverHandsaw, diamondHandsaw}) {
            OreDictionary.registerOre("toolHandsaw", tool(saw));
        }
        add(new ItemStack(stoneHandsaw), "MMS", 'M', "cobblestone", 'S', "stickWood");
        add(new ItemStack(copperHandsaw), "MMS", 'M', "ingotCopper", 'S', "stickWood");
        add(new ItemStack(goldHandsaw), "MMS", 'M', "ingotGold", 'S', "stickWood");
        add(new ItemStack(ironHandsaw), "MMS", 'M', "ingotIron", 'S', "stickWood");
        add(new ItemStack(silverHandsaw), "MMS", 'M', "ingotSilver", 'S', "stickWood");
        add(new ItemStack(diamondHandsaw), "MMS", 'M', "gemDiamond", 'S', "stickWood");
        add(new ItemStack(wireCutters), "S S", " S ", "R R", 'S', "ingotSteel", 'R', "dyeRed");
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(fineCopperWire), "ingotCopper", tool(wireCutters)));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(fineIronWire), "ingotIron", tool(wireCutters)));
        add(new ItemStack(copperCoil), " W ", "WIW", " W ", 'W', fineCopperWire, 'I', I);
        add(new ItemStack(powerCircuit), "IBI", "CGC", "IWI", 'I', I, 'B', B, 'C', copperCoil, 'G', "gearIron", 'W', AW);
        add(new ItemStack(PC), "IBI", "BCB", "IBI", 'I', I, 'B', "ingotBrass", 'C', powerCircuit);
        add(new ItemStack(Items.paper, 2), "SSS", 'S', sawdust);

        add(new ItemStack(AW, 12), "III", 'I', B);
        add(new ItemStack(ModBlocks.encasedPowerAlloyWire, 8), "WWW", "WSW", "WWW", 'W', AW, 'S', new ItemStack(Blocks.stone_slab, 1, 0));
        GameRegistry.addShapelessRecipe(new ItemStack(AW), ModBlocks.encasedPowerAlloyWire);
        add(new ItemStack(battery), " C ", "TNT", "TNT", 'C', "ingotCopper", 'T', "ingotTin", 'N', "dustAzurite");
        add(new ItemStack(ModBlocks.batteryBox), "IBI", "BRB", "IBI", 'I', I, 'B', B, 'R', Blocks.redstone_block);
        add(new ItemStack(ModBlocks.charger), "IAI", "ABA", "IWI", 'I', I, 'A', B, 'B', battery, 'W', AW);
        add(new ItemStack(ModBlocks.solarPanel), "GGG", "LBL", "SSS", 'G', "blockGlass", 'L', "gemLapis", 'B', B,
                'S', new ItemStack(Blocks.stone_slab, 1, 0));
        add(new ItemStack(ModBlocks.thermopile), "CTC", "TWT", "CTC", 'C', "ingotCopper", 'T', "ingotTin", 'W', AW);
        add(new ItemStack(ModBlocks.generator), "IGI", "GMG", "IWI", 'I', I, 'G', "gearIron", 'M', PC, 'W', AW);
        add(new ItemStack(windmill), "WSW", "SGS", "WSW", 'W', new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE),
                'S', "stickWood", 'G', "gearIron");
        add(new ItemStack(waterWheel), "PSP", "SGS", "PSP", 'P', "plankWood", 'S', "stickWood", 'G', "gearIron");
        add(new ItemStack(ModBlocks.solidFuelGenerator), "III", "CFC", "IWI", 'I', I, 'C', copperCoil, 'F', Blocks.furnace, 'W', AW);
        add(new ItemStack(ModBlocks.electricFurnace), "BBB", "BFB", "IMI", 'B', B, 'F', Blocks.furnace, 'I', I, 'M', PC);
        add(new ItemStack(ModBlocks.macerator), "FFF", "GMG", "IWI", 'F', Items.flint, 'G', "gearIron", 'M', PC, 'I', I, 'W', AW);
        add(new ItemStack(ModBlocks.sawmill), "PHP", "GMG", "IWI", 'P', "plankWood", 'H', "toolHandsaw", 'G', "gearIron", 'M', PC,
                'I', I, 'W', AW);
        add(new ItemStack(ModBlocks.powerAlloyFurnace), "BBB", "BAB", "IMI", 'B', B, 'A', ModBlocks.alloyFurnace, 'I', I, 'M', PC);
        add(new ItemStack(ModBlocks.requester), "IFI", "GMG", "IWI", 'I', I, 'F', ModBlocks.filter, 'G', "ingotGold", 'M', PC, 'W', AW);
        add(new ItemStack(ModBlocks.manager), "IRI", "CHC", "IWI", 'I', I, 'R', ModBlocks.requester, 'C', Items.comparator,
                'H', Blocks.chest, 'W', AW);
        add(new ItemStack(ModBlocks.accelerator), "ITI", "WMW", "ITI", 'I', I, 'T', ModBlocks.tube, 'W', AW, 'M', PC);

        // Płyny (etap 8)
        add(new ItemStack(ModBlocks.fluidTank), "IGI", "G G", "IGI", 'I', I, 'G', "blockGlass");
        add(new ItemStack(ModBlocks.pump), "IBI", "GMG", "IWI", 'I', I, 'B', Items.bucket, 'G', "gearBrass", 'M', PC, 'W', AW);
        add(new ItemStack(ModBlocks.fluidPipe, 8), "BGB", 'B', "ingotBrass", 'G', "blockGlass");

        // Narzędzia i stół kreślarski (etap 9)
        add(new ItemStack(electricDrill), "PPP", "IMI", "IBI", 'P', Items.diamond_pickaxe, 'I', I, 'M', powerCircuit, 'B', battery);
        add(new ItemStack(electricChainsaw), "AAA", "IMI", "IBI", 'A', Items.diamond_axe, 'I', I, 'M', powerCircuit, 'B', battery);
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(multimeter), Items.comparator, ModBlocks.signalAlloyWire, AW,
                "paneGlass", "ingotIron"));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(blankPlan), Items.paper, "dyeBlue"));
        add(new ItemStack(ModBlocks.blueprintTable), "SSS", "PTP", "PCP", 'S', Blocks.stone, 'P', "plankWood",
                'T', Blocks.crafting_table, 'C', Blocks.chest);
    }

    private static void add(ItemStack result, Object... recipe) {
        GameRegistry.addRecipe(new ShapedOreRecipe(result, recipe));
    }

    private static void gate(GateType type, Object... ingredients) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.gate(type)), ingredients));
    }

    private static void registerWires() {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ModBlocks.signalAlloyWire, 12), "III", 'I', "ingotSignalAlloy"));
        ItemStack wire = new ItemStack(ModBlocks.signalAlloyWire);
        ItemStack slab = new ItemStack(Blocks.stone_slab, 1, 0);
        for (int c = 0; c < 16; c++) {
            ItemStack insulated = new ItemStack(ModBlocks.insulatedWire, 1, c);
            GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.insulatedWire, 4, c),
                    new ItemStack(Blocks.wool, 1, c), wire, wire, wire, wire));
            encased(new ItemStack(ModBlocks.encasedInsulatedWire, 8, c), insulated, slab);
        }
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ModBlocks.bundledCable, 2), "SIS", "III", "SIS",
                'S', Items.string, 'I', new ItemStack(ModBlocks.insulatedWire, 1, OreDictionary.WILDCARD_VALUE)));
        encased(new ItemStack(ModBlocks.encasedSignalAlloyWire, 8), wire, slab);
        encased(new ItemStack(ModBlocks.encasedBundledCable, 8), new ItemStack(ModBlocks.bundledCable), slab);
    }

    /** 8 przewodów wokół płyty kamiennej = 8 w obudowie; przewód w obudowie sam w siatce = zwykły przewód. */
    private static void encased(ItemStack result, ItemStack wire, ItemStack slab) {
        GameRegistry.addRecipe(new ShapedOreRecipe(result, "WWW", "WSW", "WWW", 'W', wire, 'S', slab));
        ItemStack single = result.copy();
        single.stackSize = 1;
        ItemStack back = wire.copy();
        back.stackSize = 1;
        GameRegistry.addRecipe(new ShapelessOreRecipe(back, single));
    }

    private static void ore(String name, Item item) {
        OreDictionary.registerOre(name, item);
    }

    private static void ore(String name, ItemStack stack) {
        OreDictionary.registerOre(name, stack);
    }

    private static void storage(int meta, String oreName, Item item) {
        ItemStack block = new ItemStack(ModBlocks.storage, 1, meta);
        GameRegistry.addRecipe(new ShapedOreRecipe(block, "MMM", "MMM", "MMM", 'M', oreName));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(item, 9), block.copy()));
    }

    private static void nuggets(String nuggetOre, Item nugget, String ingotOre, Item ingot) {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ingot), "NNN", "NNN", "NNN", 'N', nuggetOre));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(nugget, 9), ingotOre));
    }

    private static void gear(Item gear, String material) {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(gear), " M ", "MGM", " M ", 'M', material, 'G', "gearStone"));
    }

    private static void smelt(ItemStack input, Item output, float xp) {
        GameRegistry.addSmelting(input, new ItemStack(output), xp);
    }
}
