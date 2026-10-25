package pl.geesio.redcircuit.registry;

import net.minecraft.world.item.ItemStack;
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

    // Tagi materiałów naprawy (przedmioty moda nie są jeszcze zarejestrowane, gdy powstają narzędzia).
    private static final net.minecraft.tags.TagKey<Item> INGOTS_STEEL = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ingots/steel"));
    private static final net.minecraft.tags.TagKey<Item> INGOTS_SILVER = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ingots/silver"));

    public static final DeferredItem<Item> SIGNAL_ALLOY_INGOT = ITEMS.registerSimpleItem("signal_alloy_ingot");
    public static final DeferredItem<Item> POWER_ALLOY_INGOT = ITEMS.registerSimpleItem("power_alloy_ingot");
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.registerSimpleItem("brass_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");

    public static final DeferredItem<Item> POWER_CIRCUIT = ITEMS.registerSimpleItem("power_circuit");
    public static final DeferredItem<Item> SIGNAL_CIRCUIT = ITEMS.registerSimpleItem("signal_circuit");
    public static final DeferredItem<Item> WINDMILL = ITEMS.register("windmill", RegIds.item(() -> new Item(RegIds.itemProps().stacksTo(1))));
    public static final DeferredItem<Item> WATER_WHEEL = ITEMS.register("water_wheel", RegIds.item(() -> new Item(RegIds.itemProps().stacksTo(1))));

    // Zębatki
    public static final DeferredItem<Item> STONE_GEAR = ITEMS.registerSimpleItem("stone_gear");
    public static final DeferredItem<Item> COPPER_GEAR = ITEMS.registerSimpleItem("copper_gear");
    public static final DeferredItem<Item> GOLD_GEAR = ITEMS.registerSimpleItem("gold_gear");
    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> BRASS_GEAR = ITEMS.registerSimpleItem("brass_gear");
    public static final DeferredItem<Item> SILVER_GEAR = ITEMS.registerSimpleItem("silver_gear");
    public static final DeferredItem<Item> DIAMOND_GEAR = ITEMS.registerSimpleItem("diamond_gear");
    public static final DeferredItem<Item> NETHERITE_GEAR = ITEMS.register("netherite_gear", RegIds.item(() -> new Item(RegIds.itemProps().fireResistant())));

    // Piły ręczne (wytrzymałość jak narzędzia z danego materiału)
    public static final DeferredItem<WireCuttersItem> WIRE_CUTTERS = ITEMS.register("wire_cutters", RegIds.item(() -> new WireCuttersItem(RegIds.itemProps().durability(256).repairable(INGOTS_STEEL))));
    public static final DeferredItem<Item> FINE_COPPER_WIRE = ITEMS.registerSimpleItem("fine_copper_wire");
    public static final DeferredItem<Item> FINE_IRON_WIRE = ITEMS.registerSimpleItem("fine_iron_wire");
    public static final DeferredItem<BatteryItem> BATTERY = ITEMS.register("battery", RegIds.item(() -> new BatteryItem(RegIds.itemProps().stacksTo(1))));
    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerSimpleItem("copper_coil");
    public static final DeferredItem<pl.geesio.redcircuit.wireless.WirelessRemoteItem> WIRELESS_REMOTE = ITEMS.register("wireless_remote", RegIds.item(() -> new pl.geesio.redcircuit.wireless.WirelessRemoteItem(RegIds.itemProps().stacksTo(1))));

    // ---------- Narzędzia elektryczne i miernik ----------
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> MINEABLE_DRILL =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(pl.geesio.redcircuit.RedCircuit.MODID, "mineable/drill"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> MINEABLE_CHAINSAW =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(pl.geesio.redcircuit.RedCircuit.MODID, "mineable/chainsaw"));
    public static final DeferredItem<pl.geesio.redcircuit.tool.ElectricToolItem> ELECTRIC_DRILL = ITEMS.register("electric_drill", RegIds.item(() -> new pl.geesio.redcircuit.tool.ElectricToolItem(MINEABLE_DRILL, 1.0f, -2.8f, RegIds.itemProps(), true)));
    public static final DeferredItem<pl.geesio.redcircuit.tool.ElectricToolItem> ELECTRIC_CHAINSAW = ITEMS.register("electric_chainsaw", RegIds.item(() -> new pl.geesio.redcircuit.tool.ElectricToolItem(MINEABLE_CHAINSAW, 5.0f, -3.0f, RegIds.itemProps())));
    public static final DeferredItem<pl.geesio.redcircuit.tool.MultimeterItem> MULTIMETER = ITEMS.register("multimeter", RegIds.item(() -> new pl.geesio.redcircuit.tool.MultimeterItem(RegIds.itemProps())));

    public static final DeferredItem<ScrewdriverItem> SCREWDRIVER = ITEMS.register("screwdriver", RegIds.item(() -> new ScrewdriverItem(RegIds.itemProps().stacksTo(1))));
    public static final DeferredItem<HandsawItem> STONE_HANDSAW = ITEMS.register("stone_handsaw", RegIds.item(() -> new HandsawItem(RegIds.itemProps().durability(131).repairable(ItemTags.STONE_TOOL_MATERIALS))));
    public static final DeferredItem<HandsawItem> COPPER_HANDSAW = ITEMS.register("copper_handsaw", RegIds.item(() -> new HandsawItem(RegIds.itemProps().durability(190).repairable(Items.COPPER_INGOT))));
    public static final DeferredItem<HandsawItem> GOLD_HANDSAW = ITEMS.register("gold_handsaw", RegIds.item(() -> new HandsawItem(RegIds.itemProps().durability(32).repairable(Items.GOLD_INGOT))));
    public static final DeferredItem<HandsawItem> IRON_HANDSAW = ITEMS.register("iron_handsaw", RegIds.item(() -> new HandsawItem(RegIds.itemProps().durability(250).repairable(Items.IRON_INGOT))));
    public static final DeferredItem<HandsawItem> SILVER_HANDSAW = ITEMS.register("silver_handsaw", RegIds.item(() -> new HandsawItem(RegIds.itemProps().durability(200).repairable(INGOTS_SILVER))));
    public static final DeferredItem<HandsawItem> DIAMOND_HANDSAW = ITEMS.register("diamond_handsaw", RegIds.item(() -> new HandsawItem(RegIds.itemProps().durability(1561).repairable(Items.DIAMOND))));
    public static final DeferredItem<HandsawItem> NETHERITE_HANDSAW = ITEMS.register("netherite_handsaw", RegIds.item(() -> new HandsawItem(RegIds.itemProps().durability(2031).fireResistant().repairable(Items.NETHERITE_INGOT))));
    public static final DeferredItem<Item> CIRCUIT_PLATE = ITEMS.registerSimpleItem("circuit_plate");

    // Surowce z rud
    public static final DeferredItem<Item> RAW_TIN = ITEMS.registerSimpleItem("raw_tin");
    public static final DeferredItem<Item> RAW_SILVER = ITEMS.registerSimpleItem("raw_silver");
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> AZURITE = ITEMS.registerSimpleItem("azurite");
    // Plany
    public static final DeferredItem<Item> BLANK_PLAN = ITEMS.registerSimpleItem("blank_plan");
    public static final DeferredItem<PlanItem> PLAN = ITEMS.register("plan", RegIds.item(() -> new PlanItem(RegIds.itemProps().stacksTo(1))));

    public static final DeferredItem<Item> TIN_NUGGET = ITEMS.registerSimpleItem("tin_nugget");
    public static final DeferredItem<Item> SILVER_NUGGET = ITEMS.registerSimpleItem("silver_nugget");

    // Pyły z maceratora (przetapiane na sztabki)
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> TIN_DUST = ITEMS.registerSimpleItem("tin_dust");
    public static final DeferredItem<Item> SILVER_DUST = ITEMS.registerSimpleItem("silver_dust");
    public static final DeferredItem<Item> SAWDUST = ITEMS.register("sawdust", RegIds.item(() -> new Item(RegIds.itemProps()
            .cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL))));   // paliwo jak patyk

    // Przedmiot (BlockItem) dla każdego zarejestrowanego bloku.
    static {
        for (DeferredHolder<Block, ? extends Block> holder : ModBlocks.BLOCKS.getEntries()) {
            if (holder == ModBlocks.FLUID_TANK) {
                ITEMS.register(holder.getId().getPath(), RegIds.item(() -> new BlockItem(holder.get(), RegIds.itemProps().useBlockDescriptionPrefix()) {
                    @Override
                    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                        pl.geesio.redcircuit.fluid.FluidTankBlock.appendTooltip(stack, tooltip);
                    }
                }));
                continue;
            }
            ITEMS.register(holder.getId().getPath(), RegIds.item(() -> new BlockItem(holder.get(), RegIds.itemProps().useBlockDescriptionPrefix())));
        }
    }
}
