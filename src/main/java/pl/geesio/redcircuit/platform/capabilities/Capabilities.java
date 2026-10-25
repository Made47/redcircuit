package pl.geesio.redcircuit.platform.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import pl.geesio.redcircuit.platform.fluids.capability.IFluidHandler;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.transfer.FabricTransfer;

/**
 * Odpowiednik Capabilities z NeoForge. Bloki innych modów i vanilli są obsługiwane przez
 * Fabric Transfer API (przedmioty, płyny) i Team Reborn Energy (energia) - patrz {@link FabricTransfer}.
 */
public final class Capabilities {
    private Capabilities() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("redcircuit", path);
    }

    public static final class ItemHandler {
        private ItemHandler() {}

        public static final BlockCapability<IItemHandler, Direction> BLOCK = new BlockCapability<>(id("item_handler"));

        static {
            BLOCK.setFallback(FabricTransfer::findItems);
        }
    }

    public static final class EnergyStorage {
        private EnergyStorage() {}

        public static final BlockCapability<IEnergyStorage, Direction> BLOCK = new BlockCapability<>(id("energy"));
        public static final ItemCapability<IEnergyStorage, Void> ITEM = new ItemCapability<>(id("energy"));

        static {
            BLOCK.setFallback(FabricTransfer::findEnergy);
        }
    }

    public static final class FluidHandler {
        private FluidHandler() {}

        public static final BlockCapability<IFluidHandler, Direction> BLOCK = new BlockCapability<>(id("fluid_handler"));

        static {
            BLOCK.setFallback(FabricTransfer::findFluids);
        }
    }
}
