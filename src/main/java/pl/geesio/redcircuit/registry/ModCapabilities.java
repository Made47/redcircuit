package pl.geesio.redcircuit.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import pl.geesio.redcircuit.energy.BatteryItem;

public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        // Rury przyjmują przedmioty z każdej strony (lejki, inne mody).
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.TUBE.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ACCELERATOR.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ACCELERATOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getAutomationHandler());

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ALLOY_FURNACE.get(),
                (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.POWER_ALLOY_FURNACE.get(),
                (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.POWER_ALLOY_FURNACE.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.THERMOPILE.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.KINETIC_GENERATOR.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.PUMP.get(),
                (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PUMP.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FLUID_PIPE.get(),
                (be, side) -> be.getHandler(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.REQUESTER.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.REQUESTER.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANAGER.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.MANAGER.get(),
                (be, side) -> be.getEnergy());
        for (var type : java.util.List.of(ModBlockEntities.RELAY, ModBlockEntities.EJECTOR, ModBlockEntities.DEPLOYER,
                ModBlockEntities.ASSEMBLER, ModBlockEntities.BUFFER)) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type.get(), (be, side) -> be.getHandler(side));
        }
        event.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> pl.geesio.redcircuit.machine.ItemDetectorBlock.handler(level, pos, state, side),
                ModBlocks.ITEM_DETECTOR.get());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FLUID_TANK.get(),
                (be, side) -> be.getTank());

        // Azurity = Forge Energy (FE), więc działa też z innymi modami.
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> BatteryItem.storage(stack),
                ModItems.BATTERY.get());
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> pl.geesio.redcircuit.tool.ElectricToolItem.storage(stack),
                ModItems.ELECTRIC_DRILL.get(), ModItems.ELECTRIC_CHAINSAW.get());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CHARGER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SOLID_FUEL_GENERATOR.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SOLID_FUEL_GENERATOR.get(), (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHARGER.get(), (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.POWER_WIRE.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BATTERY.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BATTERY.get(),
                (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SOLAR_PANEL.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SAWMILL.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SAWMILL.get(),
                (be, side) -> be.getAutomationHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.MACERATOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MACERATOR.get(),
                (be, side) -> be.getAutomationHandler());
    }
}
