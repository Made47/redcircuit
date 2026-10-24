package pl.geesio.redcircuit.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import pl.geesio.redcircuit.energy.BatteryItem;

public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        // Rury przyjmują przedmioty z każdej strony (lejki, inne mody).
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.TUBE.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ACCELERATOR.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ACCELERATOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getAutomationHandler());

        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ALLOY_FURNACE.get(),
                (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.POWER_ALLOY_FURNACE.get(),
                (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.POWER_ALLOY_FURNACE.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.THERMOPILE.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.KINETIC_GENERATOR.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.PUMP.get(),
                (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.PUMP.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_PIPE.get(),
                (be, side) -> be.getHandler(side));

        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.REQUESTER.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.REQUESTER.get(),
                (be, side) -> be.getEnergy());

        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MANAGER.get(),
                (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.MANAGER.get(),
                (be, side) -> be.getEnergy());
        for (var type : java.util.List.of(ModBlockEntities.RELAY, ModBlockEntities.EJECTOR, ModBlockEntities.DEPLOYER,
                ModBlockEntities.ASSEMBLER, ModBlockEntities.BUFFER)) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, type.get(), (be, side) -> be.getHandler(side));
        }
        event.registerBlock(Capabilities.Item.BLOCK,
                (level, pos, state, be, side) -> pl.geesio.redcircuit.machine.ItemDetectorBlock.handler(level, pos, state, side),
                ModBlocks.ITEM_DETECTOR.get());

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_TANK.get(),
                (be, side) -> be.getTank());

        // Azurity = Forge Energy (FE), więc działa też z innymi modami.
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> BatteryItem.storage(access),
                ModItems.BATTERY.get());
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> pl.geesio.redcircuit.tool.ElectricToolItem.storage(access),
                ModItems.ELECTRIC_DRILL.get(), ModItems.ELECTRIC_CHAINSAW.get());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.CHARGER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.SOLID_FUEL_GENERATOR.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SOLID_FUEL_GENERATOR.get(), (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHARGER.get(), (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.POWER_WIRE.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.BATTERY.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BATTERY.get(),
                (be, side) -> be.getAutomationHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.SOLAR_PANEL.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.SAWMILL.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SAWMILL.get(),
                (be, side) -> be.getAutomationHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.MACERATOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MACERATOR.get(),
                (be, side) -> be.getAutomationHandler());
    }
}
