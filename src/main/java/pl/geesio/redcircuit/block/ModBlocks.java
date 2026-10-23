package pl.geesio.redcircuit.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import pl.geesio.redcircuit.machine.*;
import pl.geesio.redcircuit.tube.BlockExtractor;
import pl.geesio.redcircuit.tube.BlockFilter;
import pl.geesio.redcircuit.tube.BlockSortingMachine;
import pl.geesio.redcircuit.tube.BlockTube;
import pl.geesio.redcircuit.tube.TileTube;
import pl.geesio.redcircuit.tube.*;
import pl.geesio.redcircuit.energy.*;
import pl.geesio.redcircuit.alloy.BlockAlloyFurnace;
import pl.geesio.redcircuit.alloy.TileAlloyFurnace;
import pl.geesio.redcircuit.gate.BlockGate;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.gate.TileGate;
import pl.geesio.redcircuit.light.BlockLamp;
import pl.geesio.redcircuit.wireless.BlockWirelessReceiver;
import pl.geesio.redcircuit.wireless.BlockWirelessTransmitter;
import pl.geesio.redcircuit.wireless.TileWireless;
import pl.geesio.redcircuit.wireless.TileWirelessReceiver;
import pl.geesio.redcircuit.wire.BlockBundledCable;
import pl.geesio.redcircuit.wire.BlockEncasedWire;
import pl.geesio.redcircuit.wire.BlockRedWire;
import pl.geesio.redcircuit.wire.ItemFaceWire;
import pl.geesio.redcircuit.wire.TileWire;

/** Bloki moda (rejestrowane w preInit). */
public final class ModBlocks {
    private ModBlocks() {}

    public static Block ore, storage, alloyFurnace;
    public static Block signalAlloyWire, insulatedWire, bundledCable;
    public static Block encasedSignalAlloyWire, encasedInsulatedWire, encasedBundledCable;
    public static BlockLamp lamp, litLamp, invertedLamp, invertedLampOff;
    public static Block wirelessTransmitter, wirelessReceiver;
    public static Block signalMachineCasing, tube, restrictionTube, extractor, filter, sortingMachine;
    public static Block relay, ejector, buffer, deployer, blockBreaker, igniter, itemDetector, regulator, assembler;
    public static Block powerMachineCasing, powerAlloyWire, encasedPowerAlloyWire, batteryBox, solarPanel, thermopile, generator,
            solidFuelGenerator, electricFurnace, macerator, sawmill, charger, powerAlloyFurnace, requester, manager, accelerator;
    public static Block pump, fluidPipe, fluidTank, blueprintTable;
    /** Bramki w kolejności GateType. */
    public static final BlockGate[] GATES = new BlockGate[GateType.values().length];

    public static BlockGate gate(GateType t) {
        return GATES[t.ordinal()];
    }

    public static void register() {
        ore = new BlockOre();
        GameRegistry.registerBlock(ore, ItemBlockMeta.class, "ore");
        storage = new BlockStorage();
        GameRegistry.registerBlock(storage, ItemBlockMeta.class, "storage");
        alloyFurnace = new BlockAlloyFurnace();
        GameRegistry.registerBlock(alloyFurnace, "alloy_furnace");
        GameRegistry.registerTileEntity(TileAlloyFurnace.class, "redcircuit:alloy_furnace");

        // Przewody (etap 3)
        signalAlloyWire = new BlockRedWire(false);
        GameRegistry.registerBlock(signalAlloyWire, ItemFaceWire.class, "signal_alloy_wire");
        insulatedWire = new BlockRedWire(true);
        GameRegistry.registerBlock(insulatedWire, ItemFaceWire.class, "insulated_wire");
        bundledCable = new BlockBundledCable();
        GameRegistry.registerBlock(bundledCable, ItemFaceWire.class, "bundled_cable");
        encasedSignalAlloyWire = new BlockEncasedWire(BlockEncasedWire.Kind.SIGNAL);
        GameRegistry.registerBlock(encasedSignalAlloyWire, ItemBlockMeta.class, "encased_signal_alloy_wire");
        encasedInsulatedWire = new BlockEncasedWire(BlockEncasedWire.Kind.INSULATED);
        GameRegistry.registerBlock(encasedInsulatedWire, ItemBlockMeta.class, "encased_insulated_wire");
        encasedBundledCable = new BlockEncasedWire(BlockEncasedWire.Kind.BUNDLED);
        GameRegistry.registerBlock(encasedBundledCable, ItemBlockMeta.class, "encased_bundled_cable");
        GameRegistry.registerTileEntity(TileWire.class, "redcircuit:wire");

        // Bramki logiczne (etap 4): osobny blok dla każdego typu
        for (GateType t : GateType.values()) {
            BlockGate g = new BlockGate(t);
            GameRegistry.registerBlock(g, t.getSerializedName() + "_gate");
            GATES[t.ordinal()] = g;
        }
        GameRegistry.registerTileEntity(TileGate.class, "redcircuit:gate");

        // Lampy (etap 5): para bloków zgaszona/świecąca dla zwykłej i odwróconej, metadane = kolor
        lamp = new BlockLamp(false, false);
        litLamp = new BlockLamp(false, true);
        BlockLamp.pair(lamp, litLamp);
        GameRegistry.registerBlock(lamp, ItemBlockMeta.class, "lamp");
        GameRegistry.registerBlock(litLamp, ItemBlockMeta.class, "lit_lamp");
        invertedLamp = new BlockLamp(true, true);
        invertedLampOff = new BlockLamp(true, false);
        BlockLamp.pair(invertedLamp, invertedLampOff);
        GameRegistry.registerBlock(invertedLamp, ItemBlockMeta.class, "inverted_lamp");
        GameRegistry.registerBlock(invertedLampOff, ItemBlockMeta.class, "inverted_lamp_off");

        // Redstone bezprzewodowy (etap 5)
        wirelessTransmitter = new BlockWirelessTransmitter();
        GameRegistry.registerBlock(wirelessTransmitter, "wireless_transmitter");
        wirelessReceiver = new BlockWirelessReceiver();
        GameRegistry.registerBlock(wirelessReceiver, "wireless_receiver");
        GameRegistry.registerTileEntity(TileWireless.class, "redcircuit:wireless");
        GameRegistry.registerTileEntity(TileWirelessReceiver.class, "redcircuit:wireless_receiver");

        // Rury i logistyka (etap 6)
        signalMachineCasing = reg(new BlockSimple("signal_machine_casing", Material.iron, 3.0F, Block.soundTypeMetal), "signal_machine_casing");
        tube = reg(new BlockTube(false), "tube");
        restrictionTube = reg(new BlockTube(true), "restriction_tube");
        extractor = reg(new BlockExtractor(), "extractor");
        filter = reg(new BlockFilter(), "filter");
        sortingMachine = reg(new BlockSortingMachine(), "sorting_machine");
        relay = reg(new BlockRelay(), "relay");
        ejector = reg(new BlockEjector(), "ejector");
        buffer = reg(new BlockBuffer(), "buffer");
        deployer = reg(new BlockDeployer(), "deployer");
        blockBreaker = reg(new BlockBreaker(), "block_breaker");
        igniter = reg(new BlockIgniter(), "igniter");
        itemDetector = reg(new BlockItemDetector(), "item_detector");
        regulator = reg(new BlockRegulator(), "regulator");
        assembler = reg(new BlockAssembler(), "assembler");
        GameRegistry.registerTileEntity(TileTube.class, "redcircuit:tube");
        GameRegistry.registerTileEntity(TileMachine.class, "redcircuit:machine");
        GameRegistry.registerTileEntity(BlockItemDetector.Tile.class, "redcircuit:item_detector");

        // Energia RF (etap 7)
        powerMachineCasing = reg(new BlockSimple("power_machine_casing", Material.iron, 3.0F, Block.soundTypeMetal), "power_machine_casing");
        powerAlloyWire = new BlockPowerWire();
        GameRegistry.registerBlock(powerAlloyWire, ItemFaceWire.class, "power_alloy_wire");
        encasedPowerAlloyWire = new BlockEncasedWire(BlockEncasedWire.Kind.POWER);
        GameRegistry.registerBlock(encasedPowerAlloyWire, ItemBlockMeta.class, "encased_power_alloy_wire");
        batteryBox = reg(new BlockBatteryBox(), "battery_box");
        solarPanel = reg(new BlockSolarPanel(), "solar_panel");
        thermopile = reg(new BlockEnergyMachine("thermopile", BlockEnergyMachine.Faces.PILLAR,
                () -> new TileGenerator(TileGenerator.Kind.THERMOPILE), 0), "thermopile");
        generator = reg(new BlockKineticGenerator(), "generator");
        solidFuelGenerator = reg(new BlockEnergyMachine("solid_fuel_generator", BlockEnergyMachine.Faces.FURNACE,
                TileFuelGenerator::new, 13), "solid_fuel_generator");
        electricFurnace = reg(new BlockEnergyMachine("electric_furnace", BlockEnergyMachine.Faces.FURNACE,
                TileElectricFurnace::new, 13), "electric_furnace");
        macerator = reg(new BlockEnergyMachine("macerator", BlockEnergyMachine.Faces.FURNACE,
                TileProcessor.Macerator::new, 0), "macerator");
        sawmill = reg(new BlockEnergyMachine("sawmill", BlockEnergyMachine.Faces.FURNACE,
                TileProcessor.Sawmill::new, 0), "sawmill");
        charger = reg(new BlockEnergyMachine("charger", BlockEnergyMachine.Faces.LIT_SIDES, TileCharger::new, 7), "charger");
        powerAlloyFurnace = reg(new BlockAlloyFurnace(true), "power_alloy_furnace");
        requester = reg(new BlockRequester(), "requester");
        manager = reg(new BlockManager(), "manager");
        accelerator = reg(new BlockTube(BlockTube.Kind.ACCELERATOR), "accelerator");
        GameRegistry.registerTileEntity(TilePowerWire.class, "redcircuit:power_wire");
        GameRegistry.registerTileEntity(TileBatteryBox.class, "redcircuit:battery_box");
        GameRegistry.registerTileEntity(TileGenerator.class, "redcircuit:generator");
        GameRegistry.registerTileEntity(TileFuelGenerator.class, "redcircuit:solid_fuel_generator");
        GameRegistry.registerTileEntity(TileElectricFurnace.class, "redcircuit:electric_furnace");
        GameRegistry.registerTileEntity(TileProcessor.Macerator.class, "redcircuit:macerator");
        GameRegistry.registerTileEntity(TileProcessor.Sawmill.class, "redcircuit:sawmill");
        GameRegistry.registerTileEntity(TileCharger.class, "redcircuit:charger");
        GameRegistry.registerTileEntity(TileRequester.class, "redcircuit:requester");
        GameRegistry.registerTileEntity(TileManager.class, "redcircuit:manager");
        GameRegistry.registerTileEntity(TileAccelerator.class, "redcircuit:accelerator");

        // Płyny (etap 8)
        pump = reg(new pl.geesio.redcircuit.fluid.BlockPump(), "pump");
        fluidPipe = reg(new pl.geesio.redcircuit.fluid.BlockFluidPipe(), "fluid_pipe");
        fluidTank = new pl.geesio.redcircuit.fluid.BlockFluidTank();
        GameRegistry.registerBlock(fluidTank, pl.geesio.redcircuit.fluid.ItemBlockFluidTank.class, "fluid_tank");
        GameRegistry.registerTileEntity(pl.geesio.redcircuit.fluid.TilePump.class, "redcircuit:pump");
        GameRegistry.registerTileEntity(pl.geesio.redcircuit.fluid.TileFluidPipe.class, "redcircuit:fluid_pipe");
        GameRegistry.registerTileEntity(pl.geesio.redcircuit.fluid.TileFluidTank.class, "redcircuit:fluid_tank");

        // Stół kreślarski (etap 9)
        blueprintTable = reg(new pl.geesio.redcircuit.craft.BlockBlueprintTable(), "blueprint_table");
        GameRegistry.registerTileEntity(pl.geesio.redcircuit.craft.TileBlueprintTable.class, "redcircuit:blueprint_table");
    }

    private static Block reg(Block block, String name) {
        GameRegistry.registerBlock(block, name);
        return block;
    }
}
