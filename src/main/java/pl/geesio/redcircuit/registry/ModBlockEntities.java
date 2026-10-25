package pl.geesio.redcircuit.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import pl.geesio.redcircuit.platform.registries.DeferredHolder;
import pl.geesio.redcircuit.platform.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.AlloyFurnaceBlockEntity;
import pl.geesio.redcircuit.craft.BlueprintTableBlockEntity;
import pl.geesio.redcircuit.energy.BatteryBlockEntity;
import pl.geesio.redcircuit.energy.PowerWireBlockEntity;
import pl.geesio.redcircuit.energy.ElectricFurnaceBlockEntity;
import pl.geesio.redcircuit.energy.GeneratorBlockEntity;
import pl.geesio.redcircuit.energy.PumpBlockEntity;
import pl.geesio.redcircuit.energy.FluidPipeBlockEntity;
import pl.geesio.redcircuit.energy.SolarPanelBlockEntity;
import pl.geesio.redcircuit.tube.FilterBlockEntity;
import pl.geesio.redcircuit.tube.RequesterBlockEntity;
import pl.geesio.redcircuit.machine.MachineBlockEntity;
import pl.geesio.redcircuit.machine.ManagerBlockEntity;
import pl.geesio.redcircuit.machine.RegulatorBlockEntity;
import pl.geesio.redcircuit.tube.SortingMachineBlockEntity;
import pl.geesio.redcircuit.tube.TubeBlockEntity;
import pl.geesio.redcircuit.wire.BundledCableBlockEntity;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RedCircuit.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.energy.ChargerBlockEntity>> CHARGER =
            BLOCK_ENTITIES.register("charger", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.energy.ChargerBlockEntity::new, ModBlocks.CHARGER.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.fluid.FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITIES.register("fluid_tank", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.fluid.FluidTankBlockEntity::new, ModBlocks.FLUID_TANK.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.wireless.WirelessBlockEntity>> WIRELESS =
            BLOCK_ENTITIES.register("wireless", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.wireless.WirelessBlockEntity::new, ModBlocks.WIRELESS_TRANSMITTER.get(), ModBlocks.WIRELESS_RECEIVER.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.gate.GateBlockEntity>> GATE =
            BLOCK_ENTITIES.register("gate", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.gate.GateBlockEntity::new, ModBlocks.GATES.values().stream()
                            .map(h -> (net.minecraft.world.level.block.Block) h.get())
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BundledCableBlockEntity>> BUNDLED_CABLE =
            BLOCK_ENTITIES.register("bundled_cable", () -> FabricBlockEntityTypeBuilder.create(BundledCableBlockEntity::new, ModBlocks.BUNDLED_CABLE.get(), ModBlocks.ENCASED_BUNDLED_CABLE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintTableBlockEntity>> BLUEPRINT_TABLE =
            BLOCK_ENTITIES.register("blueprint_table", () -> FabricBlockEntityTypeBuilder.create(BlueprintTableBlockEntity::new, ModBlocks.BLUEPRINT_TABLE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyFurnaceBlockEntity>> ALLOY_FURNACE =
            BLOCK_ENTITIES.register("alloy_furnace", () -> FabricBlockEntityTypeBuilder.create(AlloyFurnaceBlockEntity::fuel, ModBlocks.ALLOY_FURNACE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyFurnaceBlockEntity>> POWER_ALLOY_FURNACE =
            BLOCK_ENTITIES.register("power_alloy_furnace", () -> FabricBlockEntityTypeBuilder.create(AlloyFurnaceBlockEntity::electric, ModBlocks.POWER_ALLOY_FURNACE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> THERMOPILE =
            BLOCK_ENTITIES.register("thermopile", () -> FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::thermopile, ModBlocks.THERMOPILE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> KINETIC_GENERATOR =
            BLOCK_ENTITIES.register("generator", () -> FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::kinetic, ModBlocks.KINETIC_GENERATOR.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP =
            BLOCK_ENTITIES.register("pump", () -> FabricBlockEntityTypeBuilder.create(PumpBlockEntity::new, ModBlocks.PUMP.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register("fluid_pipe", () -> FabricBlockEntityTypeBuilder.create(FluidPipeBlockEntity::new, ModBlocks.FLUID_PIPE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubeBlockEntity>> TUBE =
            BLOCK_ENTITIES.register("tube", () -> FabricBlockEntityTypeBuilder.create(TubeBlockEntity::new, ModBlocks.TUBE.get(), ModBlocks.RESTRICTION_TUBE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.tube.AcceleratorBlockEntity>> ACCELERATOR =
            BLOCK_ENTITIES.register("accelerator", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.tube.AcceleratorBlockEntity::new, ModBlocks.ACCELERATOR.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManagerBlockEntity>> MANAGER =
            BLOCK_ENTITIES.register("manager", () -> FabricBlockEntityTypeBuilder.create(ManagerBlockEntity::new, ModBlocks.MANAGER.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RegulatorBlockEntity>> REGULATOR =
            BLOCK_ENTITIES.register("regulator", () -> FabricBlockEntityTypeBuilder.create(RegulatorBlockEntity::new, ModBlocks.REGULATOR.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> RELAY = machine("relay", 9, ModBlocks.RELAY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> EJECTOR = machine("ejector", 9, ModBlocks.EJECTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> DEPLOYER = machine("deployer", 9, ModBlocks.DEPLOYER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> ASSEMBLER = machine("assembler", 18, ModBlocks.ASSEMBLER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> BUFFER = machine("buffer", 18, ModBlocks.BUFFER);

    /** Typ dla maszyn z samym inwentarzem - wspólna klasa MachineBlockEntity. */
    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> machine(
            String name, int size, java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block) {
        java.util.concurrent.atomic.AtomicReference<BlockEntityType<MachineBlockEntity>> self = new java.util.concurrent.atomic.AtomicReference<>();
        return BLOCK_ENTITIES.register(name, () -> {
            BlockEntityType<MachineBlockEntity> type = FabricBlockEntityTypeBuilder.<MachineBlockEntity>create(
                    (pos, state) -> new MachineBlockEntity(self.get(), pos, state, size), block.get()).build();
            self.set(type);
            return type;
        });
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RequesterBlockEntity>> REQUESTER =
            BLOCK_ENTITIES.register("requester", () -> FabricBlockEntityTypeBuilder.create(RequesterBlockEntity::new, ModBlocks.REQUESTER.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilterBlockEntity>> FILTER =
            BLOCK_ENTITIES.register("filter", () -> FabricBlockEntityTypeBuilder.create(FilterBlockEntity::new, ModBlocks.FILTER.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SortingMachineBlockEntity>> SORTING_MACHINE =
            BLOCK_ENTITIES.register("sorting_machine", () -> FabricBlockEntityTypeBuilder.create(SortingMachineBlockEntity::new, ModBlocks.SORTING_MACHINE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerWireBlockEntity>> POWER_WIRE =
            BLOCK_ENTITIES.register("power_alloy_wire", () -> FabricBlockEntityTypeBuilder.create(PowerWireBlockEntity::new, ModBlocks.POWER_ALLOY_WIRE.get(), ModBlocks.ENCASED_POWER_ALLOY_WIRE.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY =
            BLOCK_ENTITIES.register("battery_box", () -> FabricBlockEntityTypeBuilder.create(BatteryBlockEntity::new, ModBlocks.BATTERY_BOX.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel", () -> FabricBlockEntityTypeBuilder.create(SolarPanelBlockEntity::new, ModBlocks.SOLAR_PANEL.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.energy.FuelGeneratorBlockEntity>> SOLID_FUEL_GENERATOR =
            BLOCK_ENTITIES.register("solid_fuel_generator", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.energy.FuelGeneratorBlockEntity::new, ModBlocks.SOLID_FUEL_GENERATOR.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.energy.SawmillBlockEntity>> SAWMILL =
            BLOCK_ENTITIES.register("sawmill", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.energy.SawmillBlockEntity::new, ModBlocks.SAWMILL.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<pl.geesio.redcircuit.energy.MaceratorBlockEntity>> MACERATOR =
            BLOCK_ENTITIES.register("macerator", () -> FabricBlockEntityTypeBuilder.create(pl.geesio.redcircuit.energy.MaceratorBlockEntity::new, ModBlocks.MACERATOR.get()).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("electric_furnace", () -> FabricBlockEntityTypeBuilder.create(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE.get()).build());
}
