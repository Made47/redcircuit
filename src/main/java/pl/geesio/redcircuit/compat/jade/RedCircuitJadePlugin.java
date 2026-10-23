package pl.geesio.redcircuit.compat.jade;

import pl.geesio.redcircuit.alloy.AlloyFurnaceBlock;
import pl.geesio.redcircuit.alloy.AlloyFurnaceBlockEntity;
import pl.geesio.redcircuit.energy.ElectricFurnaceBlock;
import pl.geesio.redcircuit.energy.ElectricFurnaceBlockEntity;
import pl.geesio.redcircuit.energy.GeneratorBlockEntity;
import pl.geesio.redcircuit.energy.GeneratorBlock;
import pl.geesio.redcircuit.gate.GateBlock;
import pl.geesio.redcircuit.gate.GateBlockEntity;
import pl.geesio.redcircuit.light.LampBlock;
import pl.geesio.redcircuit.tube.TubeBlock;
import pl.geesio.redcircuit.tube.TubeBlockEntity;
import pl.geesio.redcircuit.wire.BundledCableBlock;
import pl.geesio.redcircuit.wire.BundledCableBlockEntity;
import pl.geesio.redcircuit.wire.EncasedBundledCableBlock;
import pl.geesio.redcircuit.wire.EncasedRedWireBlock;
import pl.geesio.redcircuit.wire.RedWireBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Integracja z Jade (podpowiedź po najechaniu na blok). Ładowana tylko przez Jade.
 * Energia (FE), płyny i inwentarze są pokazywane przez Jade automatycznie (capabilities);
 * tu dochodzą informacje specyficzne dla RedCircuit.
 */
@WailaPlugin
public class RedCircuitJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Providers.BUNDLED, BundledCableBlockEntity.class);
        registration.registerBlockDataProvider(Providers.GATE, GateBlockEntity.class);
        registration.registerBlockDataProvider(Providers.WIRELESS, pl.geesio.redcircuit.wireless.WirelessBlockEntity.class);
        registration.registerBlockDataProvider(Providers.GENERATOR, GeneratorBlockEntity.class);
        registration.registerBlockDataProvider(Providers.TUBE, TubeBlockEntity.class);
        registration.registerBlockDataProvider(Providers.FURNACE, ElectricFurnaceBlockEntity.class);
        registration.registerBlockDataProvider(Providers.FURNACE, AlloyFurnaceBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Providers.SIGNAL, RedWireBlock.class);
        registration.registerBlockComponent(Providers.SIGNAL, EncasedRedWireBlock.class);
        registration.registerBlockComponent(Providers.BUNDLED, BundledCableBlock.class);
        registration.registerBlockComponent(Providers.BUNDLED, EncasedBundledCableBlock.class);
        registration.registerBlockComponent(Providers.GATE, GateBlock.class);
        registration.registerBlockComponent(Providers.LAMP, LampBlock.class);
        registration.registerBlockComponent(Providers.WIRELESS, pl.geesio.redcircuit.wireless.WirelessBlock.class);
        registration.registerBlockComponent(Providers.GENERATOR, GeneratorBlock.class);
        registration.registerBlockComponent(Providers.TUBE, TubeBlock.class);
        registration.registerBlockComponent(Providers.FURNACE, ElectricFurnaceBlock.class);
        registration.registerBlockComponent(Providers.FURNACE, AlloyFurnaceBlock.class);
    }
}
