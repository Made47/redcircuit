package pl.geesio.redcircuit.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import pl.geesio.redcircuit.platform.registries.DeferredHolder;
import pl.geesio.redcircuit.platform.registries.DeferredItem;
import pl.geesio.redcircuit.platform.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import pl.geesio.redcircuit.craft.HandsawItem;
import pl.geesio.redcircuit.craft.ScrewdriverItem;
import pl.geesio.redcircuit.craft.WireCuttersItem;
import pl.geesio.redcircuit.energy.BatteryItem;
import pl.geesio.redcircuit.craft.PlanItem;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedCircuit.MODID);

    public static final DeferredItem<Item> SIGNAL_ALLOY_INGOT = ITEMS.registerSimpleItem("signal_alloy_ingot");
    public static final DeferredItem<Item> POWER_ALLOY_INGOT = ITEMS.registerSimpleItem("power_alloy_ingot");
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");

    public static final DeferredItem<Item> POWER_CIRCUIT = ITEMS.registerSimpleItem("power_circuit");
    public static final DeferredItem<Item> SIGNAL_CIRCUIT = ITEMS.registerSimpleItem("signal_circuit");
    public static final DeferredItem<Item> WINDMILL = ITEMS.register("windmill", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WATER_WHEEL = ITEMS.register("water_wheel", () -> new Item(new Item.Properties().stacksTo(1)));

    // Zębatki
    public static final DeferredItem<Item> STONE_GEAR = ITEMS.registerSimpleItem("stone_gear");
    public static final DeferredItem<Item> COPPER_GEAR = ITEMS.registerSimpleItem("copper_gear");
    public static final DeferredItem<Item> GOLD_GEAR = ITEMS.registerSimpleItem("gold_gear");
    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> BRASS_GEAR = ITEMS.registerSimpleItem("brass_gear");
    public static final DeferredItem<Item> SILVER_GEAR = ITEMS.registerSimpleItem("silver_gear");
    public static final DeferredItem<Item> DIAMOND_GEAR = ITEMS.registerSimpleItem("diamond_gear");
    public static final DeferredItem<Item> NETHERITE_GEAR = ITEMS.register("netherite_gear",
            () -> new Item(new Item.Properties().fireResistant()));

    // Piły ręczne (wytrzymałość jak narzędzia z danego materiału)
    public static final DeferredItem<WireCuttersItem> WIRE_CUTTERS = ITEMS.register("wire_cutters",
            () -> new WireCuttersItem(new Item.Properties().durability(256), s -> s.is(ModItems.STEEL_INGOT.get())));
    public static final DeferredItem<Item> FINE_COPPER_WIRE = ITEMS.registerSimpleItem("fine_copper_wire");
    public static final DeferredItem<Item> FINE_IRON_WIRE = ITEMS.registerSimpleItem("fine_iron_wire");
    public static final DeferredItem<BatteryItem> BATTERY = ITEMS.register("battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerSimpleItem("copper_coil");
    public static final DeferredItem<pl.geesio.redcircuit.wireless.WirelessRemoteItem> WIRELESS_REMOTE = ITEMS.register("wireless_remote",
            () -> new pl.geesio.redcircuit.wireless.WirelessRemoteItem(new Item.Properties().stacksTo(1)));

    // ---------- Narzędzia elektryczne i miernik ----------
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> MINEABLE_DRILL =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(pl.geesio.redcircuit.RedCircuit.MODID, "mineable/drill"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> MINEABLE_CHAINSAW =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(pl.geesio.redcircuit.RedCircuit.MODID, "mineable/chainsaw"));
    public static final DeferredItem<pl.geesio.redcircuit.tool.ElectricToolItem> ELECTRIC_DRILL = ITEMS.register("electric_drill",
            () -> new pl.geesio.redcircuit.tool.ElectricToolItem(MINEABLE_DRILL, 1.0f, -2.8f, new Item.Properties(), true));
    public static final DeferredItem<pl.geesio.redcircuit.tool.ElectricToolItem> ELECTRIC_CHAINSAW = ITEMS.register("electric_chainsaw",
            () -> new pl.geesio.redcircuit.tool.ElectricToolItem(MINEABLE_CHAINSAW, 5.0f, -3.0f, new Item.Properties()));
    public static final DeferredItem<pl.geesio.redcircuit.tool.MultimeterItem> MULTIMETER = ITEMS.register("multimeter",
            () -> new pl.geesio.redcircuit.tool.MultimeterItem(new Item.Properties()));

    public static final DeferredItem<ScrewdriverItem> SCREWDRIVER = ITEMS.register("screwdriver",
            () -> new ScrewdriverItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<HandsawItem> STONE_HANDSAW = ITEMS.register("stone_handsaw",
            () -> new HandsawItem(new Item.Properties().durability(131), s -> s.is(ItemTags.STONE_TOOL_MATERIALS)));
    public static final DeferredItem<HandsawItem> COPPER_HANDSAW = ITEMS.register("copper_handsaw",
            () -> new HandsawItem(new Item.Properties().durability(190), s -> s.is(Items.COPPER_INGOT)));
    public static final DeferredItem<HandsawItem> GOLD_HANDSAW = ITEMS.register("gold_handsaw",
            () -> new HandsawItem(new Item.Properties().durability(32), s -> s.is(Items.GOLD_INGOT)));
    public static final DeferredItem<HandsawItem> IRON_HANDSAW = ITEMS.register("iron_handsaw",
            () -> new HandsawItem(new Item.Properties().durability(250), s -> s.is(Items.IRON_INGOT)));
    public static final DeferredItem<HandsawItem> SILVER_HANDSAW = ITEMS.register("silver_handsaw",
            () -> new HandsawItem(new Item.Properties().durability(200), s -> s.is(ModItems.SILVER_INGOT.get())));
    public static final DeferredItem<HandsawItem> DIAMOND_HANDSAW = ITEMS.register("diamond_handsaw",
            () -> new HandsawItem(new Item.Properties().durability(1561), s -> s.is(Items.DIAMOND)));
    public static final DeferredItem<HandsawItem> NETHERITE_HANDSAW = ITEMS.register("netherite_handsaw",
            () -> new HandsawItem(new Item.Properties().durability(2031).fireResistant(), s -> s.is(Items.NETHERITE_INGOT)));
    public static final DeferredItem<Item> CIRCUIT_PLATE = ITEMS.registerSimpleItem("circuit_plate");

    // Surowce z rud
    public static final DeferredItem<Item> RAW_TIN = ITEMS.registerSimpleItem("raw_tin");
    public static final DeferredItem<Item> RAW_SILVER = ITEMS.registerSimpleItem("raw_silver");
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> AZURITE = ITEMS.registerSimpleItem("azurite");
    // Plany
    public static final DeferredItem<Item> BLANK_PLAN = ITEMS.registerSimpleItem("blank_plan");
    public static final DeferredItem<PlanItem> PLAN = ITEMS.register("plan",
            () -> new PlanItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> TIN_NUGGET = ITEMS.registerSimpleItem("tin_nugget");
    public static final DeferredItem<Item> SILVER_NUGGET = ITEMS.registerSimpleItem("silver_nugget");

    // Pyły z maceratora (przetapiane na sztabki)
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> TIN_DUST = ITEMS.registerSimpleItem("tin_dust");
    public static final DeferredItem<Item> SILVER_DUST = ITEMS.registerSimpleItem("silver_dust");
    public static final DeferredItem<Item> SAWDUST = ITEMS.registerSimpleItem("sawdust");

    // Przedmiot (BlockItem) dla każdego zarejestrowanego bloku.
    static {
        for (DeferredHolder<Block, ? extends Block> holder : ModBlocks.BLOCKS.getEntries()) {
            ITEMS.register(holder.getId().getPath(), () -> new BlockItem(holder.get(), new Item.Properties()));
        }
    }
}
