package pl.geesio.redcircuit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import pl.geesio.redcircuit.gate.GateType;

/** Otwiera okno ustawień bramki (wywoływane tylko po stronie klienta). */
public final class GateScreenOpener {
    private GateScreenOpener() {}

    public static void open(BlockPos pos, GateType type) {
        Minecraft.getInstance().gui.setScreen(new GateScreen(pos, type));
    }
}
