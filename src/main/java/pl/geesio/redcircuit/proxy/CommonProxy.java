package pl.geesio.redcircuit.proxy;

import net.minecraft.world.World;
import pl.geesio.redcircuit.gate.GateType;

public class CommonProxy {
    public void preInit() {
    }

    public void init() {
    }

    public void postInit() {
    }

    /** Okno ustawień bramki (tylko klient). */
    public void openGateGui(World world, int x, int y, int z, GateType type) {
    }

    /** Okno częstotliwości (tylko klient); target 0 = blok, 1 = trzymany pilot. */
    public void openFrequencyGui(int target, int x, int y, int z, int frequency, String titleKey) {
    }
}
