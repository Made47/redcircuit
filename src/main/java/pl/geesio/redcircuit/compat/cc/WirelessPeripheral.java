package pl.geesio.redcircuit.compat.cc;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.wireless.WirelessBlock;
import pl.geesio.redcircuit.wireless.WirelessBlockEntity;
import pl.geesio.redcircuit.wireless.WirelessNetwork;
import pl.geesio.redcircuit.wireless.WirelessTransmitterBlock;

/** Peryferium {@code redcircuit_wireless}: częstotliwość i stan nadajnika/odbiornika. */
public class WirelessPeripheral implements IPeripheral {
    private final WirelessBlockEntity be;

    public WirelessPeripheral(WirelessBlockEntity be) {
        this.be = be;
    }

    @Override
    public String getType() {
        return "redcircuit_wireless";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof WirelessPeripheral w && w.be == be;
    }

    @Override
    public Object getTarget() {
        return be;
    }

    private WirelessBlock block() throws LuaException {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) throw new LuaException("Block is no longer available");
        if (!(level.getBlockState(be.getBlockPos()).getBlock() instanceof WirelessBlock b)) throw new LuaException("Not a wireless block");
        return b;
    }

    @LuaFunction(mainThread = true)
    public final int getFrequency() throws LuaException {
        block();
        return be.getFrequency();
    }

    /** Ustawia częstotliwość (1-5000). */
    @LuaFunction(mainThread = true)
    public final void setFrequency(int frequency) throws LuaException {
        if (frequency < WirelessNetwork.MIN_FREQ || frequency > WirelessNetwork.MAX_FREQ) {
            throw new LuaException("Frequency must be between " + WirelessNetwork.MIN_FREQ + " and " + WirelessNetwork.MAX_FREQ);
        }
        block().applyFrequency(be.getLevel(), be.getBlockPos(), frequency);
    }

    @LuaFunction(mainThread = true)
    public final boolean isTransmitter() throws LuaException {
        return block() instanceof WirelessTransmitterBlock;
    }

    /** Nadajnik: czy nadaje; odbiornik: czy odbiera sygnał. */
    @LuaFunction(mainThread = true)
    public final boolean isPowered() throws LuaException {
        block();
        return be.getBlockState().getValue(WirelessBlock.POWERED);
    }

    /** Czy na danej częstotliwości (domyślnie własnej) nadaje jakikolwiek nadajnik. */
    @LuaFunction(mainThread = true)
    public final boolean isChannelActive(java.util.Optional<Integer> frequency) throws LuaException {
        block();
        Level level = be.getLevel();
        if (level == null || level.getServer() == null) return false;
        int f = WirelessNetwork.clampFreq(frequency.orElse(be.getFrequency()));
        return WirelessNetwork.get(level.getServer()).isActive(level.getServer(), f);
    }
}
