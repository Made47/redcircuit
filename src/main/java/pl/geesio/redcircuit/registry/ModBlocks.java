package pl.geesio.redcircuit.registry;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import pl.geesio.redcircuit.platform.registries.DeferredBlock;
import pl.geesio.redcircuit.platform.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.AlloyFurnaceBlock;
import pl.geesio.redcircuit.craft.BlueprintTableBlock;
import pl.geesio.redcircuit.energy.BatteryBlock;
import pl.geesio.redcircuit.light.LampBlock;
import pl.geesio.redcircuit.energy.EncasedPowerWireBlock;
import pl.geesio.redcircuit.wire.EncasedBundledCableBlock;
import pl.geesio.redcircuit.wire.EncasedRedWireBlock;
import pl.geesio.redcircuit.energy.PowerWireBlock;
import pl.geesio.redcircuit.energy.ElectricFurnaceBlock;
import pl.geesio.redcircuit.energy.GeneratorBlock;
import pl.geesio.redcircuit.energy.GeneratorBlockEntity;
import pl.geesio.redcircuit.energy.KineticGeneratorBlock;
import pl.geesio.redcircuit.energy.PumpBlock;
import pl.geesio.redcircuit.energy.FluidPipeBlock;
import pl.geesio.redcircuit.energy.SolarPanelBlock;
import pl.geesio.redcircuit.gate.GateBlock;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.tube.FilterBlock;
import pl.geesio.redcircuit.tube.RequesterBlock;
import pl.geesio.redcircuit.machine.*;
import pl.geesio.redcircuit.tube.SortingMachineBlock;
import pl.geesio.redcircuit.tube.ExtractorBlock;
import pl.geesio.redcircuit.tube.TubeBlock;
import pl.geesio.redcircuit.wire.BundledCableBlock;
import pl.geesio.redcircuit.wire.RedWireBlock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedCircuit.MODID);

    private static BlockBehaviour.Properties wireProps(SoundType sound) {
        return RegIds.blockProps()
                .instabreak()
                .noCollision()
                .noOcclusion()
                .sound(sound)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties machineProps() {
        return RegIds.blockProps()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 6.0f)
                .sound(SoundType.STONE);
    }


    // ---------- Rudy ----------
    public static final List<DeferredBlock<DropExperienceBlock>> ORES = new ArrayList<>();
    static {
        registerOre("tin", ConstantInt.of(0));
        registerOre("silver", ConstantInt.of(0));
        registerOre("azurite", UniformInt.of(1, 5));
    }

    private static void registerOre(String name, IntProvider xp) {
        ORES.add(BLOCKS.register(name + "_ore", RegIds.block(() -> new DropExperienceBlock(xp, RegIds.blockProps()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.0f, 3.0f)
                .sound(SoundType.STONE)))));
        ORES.add(BLOCKS.register("deepslate_" + name + "_ore", RegIds.block(() -> new DropExperienceBlock(xp, RegIds.blockProps()
                .mapColor(MapColor.DEEPSLATE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(4.5f, 3.0f)
                .sound(SoundType.DEEPSLATE)))));
    }

    // ---------- Bloki przechowywania ----------
    private static BlockBehaviour.Properties storageProps(MapColor color) {
        return RegIds.blockProps()
                .mapColor(color)
                .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops()
                .strength(5.0f, 6.0f)
                .sound(SoundType.METAL);
    }

    public static final DeferredBlock<Block> TIN_BLOCK = BLOCKS.register("tin_block", RegIds.block(() -> new Block(storageProps(MapColor.METAL))));
    public static final DeferredBlock<Block> SILVER_BLOCK = BLOCKS.register("silver_block", RegIds.block(() -> new Block(storageProps(MapColor.SNOW))));
    public static final DeferredBlock<Block> AZURITE_BLOCK = BLOCKS.register("azurite_block", RegIds.block(() -> new Block(storageProps(MapColor.COLOR_LIGHT_BLUE))));

    // ---------- Stół kreślarski ----------
    public static final DeferredBlock<BlueprintTableBlock> BLUEPRINT_TABLE = BLOCKS.register("blueprint_table", RegIds.block(() -> new BlueprintTableBlock(RegIds.blockProps()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.5f)
                    .sound(SoundType.WOOD))));

    // ---------- Piece do stopów ----------
    public static final DeferredBlock<AlloyFurnaceBlock> ALLOY_FURNACE = BLOCKS.register("alloy_furnace", RegIds.block(() -> new AlloyFurnaceBlock(RegIds.blockProps()
                    .mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .lightLevel(s -> s.getValue(AlloyFurnaceBlock.LIT) ? 13 : 0), false)));
    public static final DeferredBlock<AlloyFurnaceBlock> POWER_ALLOY_FURNACE = BLOCKS.register("power_alloy_furnace", RegIds.block(() -> new AlloyFurnaceBlock(RegIds.blockProps()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(AlloyFurnaceBlock.LIT) ? 10 : 0), true)));

    // ---------- Generatory ----------
    public static final DeferredBlock<GeneratorBlock> THERMOPILE = BLOCKS.register("thermopile", RegIds.block(() -> new GeneratorBlock(machineProps().sound(SoundType.METAL), GeneratorBlockEntity.Kind.THERMOPILE)));
    public static final DeferredBlock<KineticGeneratorBlock> KINETIC_GENERATOR = BLOCKS.register("generator", RegIds.block(() -> new KineticGeneratorBlock(machineProps().sound(SoundType.METAL))));

    public static final DeferredBlock<PumpBlock> PUMP = BLOCKS.register("pump", RegIds.block(() -> new PumpBlock(machineProps().sound(SoundType.METAL))));
    public static final DeferredBlock<FluidPipeBlock> FLUID_PIPE = BLOCKS.register("fluid_pipe", RegIds.block(() -> new FluidPipeBlock(RegIds.blockProps().mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5f).noOcclusion().sound(SoundType.COPPER))));

    // ---------- Kable ----------
    public static final DeferredBlock<RedWireBlock> SIGNAL_ALLOY_WIRE = BLOCKS.register("signal_alloy_wire", RegIds.block(() -> new RedWireBlock(wireProps(SoundType.METAL), -1)));

    public static final List<DeferredBlock<RedWireBlock>> INSULATED_WIRES = new ArrayList<>();
    static {
        for (DyeColor color : DyeColor.values()) {
            final int id = color.getId();
            INSULATED_WIRES.add(BLOCKS.register("insulated_wire_" + color.getName(), RegIds.block(() -> new RedWireBlock(wireProps(SoundType.WOOL), id))));
        }
    }

    public static final DeferredBlock<BundledCableBlock> BUNDLED_CABLE = BLOCKS.register("bundled_cable", RegIds.block(() -> new BundledCableBlock(wireProps(SoundType.WOOL))));

    // ---------- Przewody w obudowie (Encased) ----------
    private static BlockBehaviour.Properties casingProps() {
        return RegIds.blockProps().mapColor(MapColor.STONE).strength(0.5f).noOcclusion().sound(SoundType.STONE);
    }

    public static final DeferredBlock<EncasedRedWireBlock> ENCASED_SIGNAL_ALLOY_WIRE = BLOCKS.register("encased_signal_alloy_wire", RegIds.block(() -> new EncasedRedWireBlock(casingProps(), -1)));

    public static final List<DeferredBlock<EncasedRedWireBlock>> ENCASED_INSULATED_WIRES = new ArrayList<>();
    static {
        for (DyeColor color : DyeColor.values()) {
            final int id = color.getId();
            ENCASED_INSULATED_WIRES.add(BLOCKS.register("encased_insulated_wire_" + color.getName(), RegIds.block(() -> new EncasedRedWireBlock(casingProps(), id))));
        }
    }

    public static final DeferredBlock<EncasedBundledCableBlock> ENCASED_BUNDLED_CABLE = BLOCKS.register("encased_bundled_cable", RegIds.block(() -> new EncasedBundledCableBlock(casingProps())));

    public static final DeferredBlock<EncasedPowerWireBlock> ENCASED_POWER_ALLOY_WIRE = BLOCKS.register("encased_power_alloy_wire", RegIds.block(() -> new EncasedPowerWireBlock(casingProps())));

    public static final DeferredBlock<pl.geesio.redcircuit.energy.ChargerBlock> CHARGER = BLOCKS.register("charger", RegIds.block(() -> new pl.geesio.redcircuit.energy.ChargerBlock(machineProps().sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(pl.geesio.redcircuit.energy.ChargerBlock.LIT) ? 7 : 0))));

    // ---------- Zbiornik na ciecz ----------
    public static final DeferredBlock<pl.geesio.redcircuit.fluid.FluidTankBlock> FLUID_TANK = BLOCKS.register("fluid_tank", RegIds.block(() -> new pl.geesio.redcircuit.fluid.FluidTankBlock(RegIds.blockProps()
                    .mapColor(MapColor.METAL).strength(1.5f).noOcclusion().sound(SoundType.GLASS)
                    .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false))));

    // ---------- Redstone bezprzewodowy ----------
    private static BlockBehaviour.Properties wirelessProps() {
        return RegIds.blockProps().mapColor(MapColor.STONE).instabreak().noOcclusion()
                .sound(SoundType.STONE).pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredBlock<pl.geesio.redcircuit.wireless.WirelessTransmitterBlock> WIRELESS_TRANSMITTER =
            BLOCKS.register("wireless_transmitter", RegIds.block(() -> new pl.geesio.redcircuit.wireless.WirelessTransmitterBlock(wirelessProps())));
    public static final DeferredBlock<pl.geesio.redcircuit.wireless.WirelessReceiverBlock> WIRELESS_RECEIVER =
            BLOCKS.register("wireless_receiver", RegIds.block(() -> new pl.geesio.redcircuit.wireless.WirelessReceiverBlock(wirelessProps())));

    // ---------- Lampy ----------
    private static BlockBehaviour.Properties lampProps() {
        return RegIds.blockProps().strength(0.3f).sound(SoundType.GLASS)
                .lightLevel(s -> s.getValue(LampBlock.LIT) ? 15 : 0)
                .isRedstoneConductor((s, l, p) -> true);
    }

    public static final List<DeferredBlock<LampBlock>> LAMPS = new ArrayList<>();
    public static final List<DeferredBlock<LampBlock>> INVERTED_LAMPS = new ArrayList<>();
    static {
        for (DyeColor color : DyeColor.values()) {
            final int id = color.getId();
            LAMPS.add(BLOCKS.register("lamp_" + color.getName(), RegIds.block(() -> new LampBlock(lampProps().mapColor(color), id, false))));
        }
        for (DyeColor color : DyeColor.values()) {
            final int id = color.getId();
            INVERTED_LAMPS.add(BLOCKS.register("inverted_lamp_" + color.getName(), RegIds.block(() -> new LampBlock(lampProps().mapColor(color), id, true))));
        }
    }

    // ---------- Bramki ----------
    public static final Map<GateType, DeferredBlock<GateBlock>> GATES = new EnumMap<>(GateType.class);
    static {
        for (GateType type : GateType.values()) {
            GATES.put(type, BLOCKS.register(type.getSerializedName() + "_gate", RegIds.block(() -> new GateBlock(RegIds.blockProps()
                            .mapColor(MapColor.STONE)
                            .instabreak()
                            .noOcclusion()
                            .sound(SoundType.STONE)
                            .pushReaction(PushReaction.DESTROY), type))));
        }
    }

    // ---------- Rury ----------
    public static final DeferredBlock<TubeBlock> TUBE = BLOCKS.register("tube", RegIds.block(() -> new TubeBlock(RegIds.blockProps()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5f)
                    .noOcclusion()
                    .sound(SoundType.COPPER))));
    public static final DeferredBlock<pl.geesio.redcircuit.tube.RestrictionTubeBlock> RESTRICTION_TUBE = BLOCKS.register("restriction_tube", RegIds.block(() -> new pl.geesio.redcircuit.tube.RestrictionTubeBlock(RegIds.blockProps()
                    .mapColor(MapColor.METAL)
                    .strength(0.6f)
                    .noOcclusion()
                    .sound(SoundType.METAL))));
    public static final DeferredBlock<pl.geesio.redcircuit.tube.AcceleratorBlock> ACCELERATOR = BLOCKS.register("accelerator", RegIds.block(() -> new pl.geesio.redcircuit.tube.AcceleratorBlock(RegIds.blockProps()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(1.0f)
                    .noOcclusion()
                    .sound(SoundType.METAL))));

    public static final DeferredBlock<ExtractorBlock> EXTRACTOR = BLOCKS.register("extractor", RegIds.block(() -> new ExtractorBlock(machineProps())));
    public static final DeferredBlock<FilterBlock> FILTER = BLOCKS.register("filter", RegIds.block(() -> new FilterBlock(machineProps())));
    public static final DeferredBlock<RequesterBlock> REQUESTER = BLOCKS.register("requester", RegIds.block(() -> new RequesterBlock(machineProps().sound(SoundType.METAL))));
    public static final DeferredBlock<SortingMachineBlock> SORTING_MACHINE = BLOCKS.register("sorting_machine", RegIds.block(() -> new SortingMachineBlock(machineProps())));

    // ---------- Maszyny logistyczne ----------
    public static final DeferredBlock<ManagerBlock> MANAGER = BLOCKS.register("manager", RegIds.block(() -> new ManagerBlock(machineProps().sound(SoundType.METAL))));
    public static final DeferredBlock<RegulatorBlock> REGULATOR = BLOCKS.register("regulator", RegIds.block(() -> new RegulatorBlock(machineProps())));
    public static final DeferredBlock<RelayBlock> RELAY = BLOCKS.register("relay", RegIds.block(() -> new RelayBlock(machineProps(), () -> ModBlockEntities.RELAY.get())));
    public static final DeferredBlock<ItemDetectorBlock> ITEM_DETECTOR = BLOCKS.register("item_detector", RegIds.block(() -> new ItemDetectorBlock(machineProps())));
    public static final DeferredBlock<IgniterBlock> IGNITER = BLOCKS.register("igniter", RegIds.block(() -> new IgniterBlock(machineProps())));
    public static final DeferredBlock<AssemblerBlock> ASSEMBLER = BLOCKS.register("assembler", RegIds.block(() -> new AssemblerBlock(machineProps(), () -> ModBlockEntities.ASSEMBLER.get())));
    public static final DeferredBlock<EjectorBlock> EJECTOR = BLOCKS.register("ejector", RegIds.block(() -> new EjectorBlock(machineProps(), () -> ModBlockEntities.EJECTOR.get())));
    public static final DeferredBlock<DeployerBlock> DEPLOYER = BLOCKS.register("deployer", RegIds.block(() -> new DeployerBlock(machineProps(), () -> ModBlockEntities.DEPLOYER.get())));
    public static final DeferredBlock<BufferBlock> BUFFER = BLOCKS.register("buffer", RegIds.block(() -> new BufferBlock(machineProps(), () -> ModBlockEntities.BUFFER.get())));
    public static final DeferredBlock<BlockBreakerBlock> BLOCK_BREAKER = BLOCKS.register("block_breaker", RegIds.block(() -> new BlockBreakerBlock(machineProps())));

    // ---------- Azurity ----------
    public static final DeferredBlock<PowerWireBlock> POWER_ALLOY_WIRE = BLOCKS.register("power_alloy_wire", RegIds.block(() -> new PowerWireBlock(wireProps(SoundType.METAL))));
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.register("solar_panel", RegIds.block(() -> new SolarPanelBlock(RegIds.blockProps()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(1.5f)
                    .noOcclusion()
                    .sound(SoundType.METAL))));
    public static final DeferredBlock<BatteryBlock> BATTERY_BOX = BLOCKS.register("battery_box", RegIds.block(() -> new BatteryBlock(machineProps().sound(SoundType.METAL))));
    public static final DeferredBlock<pl.geesio.redcircuit.energy.FuelGeneratorBlock> SOLID_FUEL_GENERATOR = BLOCKS.register("solid_fuel_generator", RegIds.block(() -> new pl.geesio.redcircuit.energy.FuelGeneratorBlock(machineProps().sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(pl.geesio.redcircuit.energy.FuelGeneratorBlock.LIT) ? 13 : 0))));
    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.register("electric_furnace", RegIds.block(() -> new ElectricFurnaceBlock(machineProps()
                    .lightLevel(s -> s.getValue(ElectricFurnaceBlock.LIT) ? 13 : 0))));
    /** Obudowa maszyny energetycznej - składnik maszyn na energię. */
    public static final DeferredBlock<Block> POWER_MACHINE_CASING = BLOCKS.register("power_machine_casing", RegIds.block(() -> new Block(machineProps().sound(SoundType.METAL))));
    /** Obudowa maszyny sygnałowej - składnik maszyn sterowanych redstone (bez energii). */
    public static final DeferredBlock<Block> SIGNAL_MACHINE_CASING = BLOCKS.register("signal_machine_casing", RegIds.block(() -> new Block(machineProps().sound(SoundType.METAL))));
    public static final DeferredBlock<pl.geesio.redcircuit.energy.SawmillBlock> SAWMILL = BLOCKS.register("sawmill", RegIds.block(() -> new pl.geesio.redcircuit.energy.SawmillBlock(machineProps().sound(SoundType.METAL))));
    public static final DeferredBlock<pl.geesio.redcircuit.energy.MaceratorBlock> MACERATOR = BLOCKS.register("macerator", RegIds.block(() -> new pl.geesio.redcircuit.energy.MaceratorBlock(machineProps().sound(SoundType.METAL))));
}
