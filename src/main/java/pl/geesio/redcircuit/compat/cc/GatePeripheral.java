package pl.geesio.redcircuit.compat.cc;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.gate.GateBlock;
import pl.geesio.redcircuit.gate.GateBlockEntity;
import pl.geesio.redcircuit.gate.GateType;

import java.util.HashMap;
import java.util.Map;

/**
 * Peryferium {@code redcircuit_gate}: odczyt i zmiana ustawień bramki (timer, sekwencer, komórka stanu, licznik).
 * Wszystkie funkcje działają w wątku gry ({@code mainThread = true}).
 */
public class GatePeripheral implements IPeripheral {
    private final GateBlockEntity be;

    public GatePeripheral(GateBlockEntity be) {
        this.be = be;
    }

    @Override
    public String getType() {
        return "redcircuit_gate";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof GatePeripheral g && g.be == be;
    }

    @Override
    public Object getTarget() {
        return be;
    }

    private GateBlock gate() throws LuaException {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) throw new LuaException("Gate is no longer available");
        if (!(level.getBlockState(be.getBlockPos()).getBlock() instanceof GateBlock g)) throw new LuaException("Not a gate");
        return g;
    }

    /** Rodzaj bramki, np. "timer", "counter". */
    @LuaFunction(mainThread = true)
    public final String getGateType() throws LuaException {
        return gate().getGateType().getSerializedName();
    }

    /** Stan bramki: wejścia (lewo/tył/prawo), licznik, ustawienia. */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getState() throws LuaException {
        GateBlock g = gate();
        GateType t = g.getGateType();
        BlockState state = be.getBlockState();
        Map<String, Object> out = new HashMap<>();
        out.put("type", t.getSerializedName());
        out.put("left", be.lastL);
        out.put("back", be.lastB);
        out.put("right", be.lastR);
        out.put("muted", be.muted);
        out.put("mirrored", be.mirrored);
        Map<String, Boolean> enabled = new HashMap<>();
        if ((t.switchableSides() & GateType.IN_L) != 0) enabled.put("left", (GateBlock.disabledMask(be.getBlockState()) & GateType.IN_L) == 0);
        if ((t.switchableSides() & GateType.IN_B) != 0) enabled.put("back", (GateBlock.disabledMask(be.getBlockState()) & GateType.IN_B) == 0);
        if ((t.switchableSides() & GateType.IN_R) != 0) enabled.put("right", (GateBlock.disabledMask(be.getBlockState()) & GateType.IN_R) == 0);
        out.put("inputs", enabled);
        if (t == GateType.RS_LATCH) out.put("quiet", be.quiet);
        if (t.hasInterval()) out.put("interval", be.interval);
        if (t == GateType.COUNTER) {
            out.put("count", be.count);
            out.put("max", be.max());
        }
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("powered") || prop.getName().equals("state")) {
                out.put(prop.getName(), state.getValue(prop));
            }
        }
        return out;
    }

    /** Okres w tickach (timer, sekwencer, komórka stanu). */
    @LuaFunction(mainThread = true)
    public final int getInterval() throws LuaException {
        if (!gate().getGateType().hasInterval()) throw new LuaException("This gate has no interval");
        return be.interval;
    }

    @LuaFunction(mainThread = true)
    public final void setInterval(int ticks) throws LuaException {
        GateBlock g = gate();
        if (!g.getGateType().hasInterval()) throw new LuaException("This gate has no interval");
        g.applySettings(be.getLevel(), be.getBlockPos(), ticks, be.max, false, be.muted);
    }

    @LuaFunction(mainThread = true)
    public final int getCount() throws LuaException {
        requireCounter();
        return be.count;
    }

    @LuaFunction(mainThread = true)
    public final int getMax() throws LuaException {
        requireCounter();
        return be.max();
    }

    @LuaFunction(mainThread = true)
    public final void setMax(int max) throws LuaException {
        GateBlock g = requireCounter();
        g.applySettings(be.getLevel(), be.getBlockPos(), be.interval, max, false, be.muted);
    }

    @LuaFunction(mainThread = true)
    public final void resetCount() throws LuaException {
        GateBlock g = requireCounter();
        g.applySettings(be.getLevel(), be.getBlockPos(), be.interval, be.max, true, be.muted);
    }

    @LuaFunction(mainThread = true)
    public final boolean isMuted() throws LuaException {
        gate();
        return be.muted;
    }

    @LuaFunction(mainThread = true)
    public final void setMuted(boolean muted) throws LuaException {
        GateBlock g = gate();
        if (!g.configurable()) throw new LuaException("This gate has no settings");
        g.applySettings(be.getLevel(), be.getBlockPos(), be.interval, be.max, false, muted);
    }

    /** Komórka stanu: czy wyjście "w trakcie" jest po lewej (true) czy po prawej (false). */
    @LuaFunction(mainThread = true)
    public final boolean isMirrored() throws LuaException {
        gate();
        return be.mirrored;
    }

    @LuaFunction(mainThread = true)
    public final void setMirrored(boolean mirrored) throws LuaException {
        GateBlock g = gate();
        if (g.getGateType() != GateType.STATE_CELL && g.getGateType() != GateType.RS_LATCH)
            throw new LuaException("Only state cells and RS latches can be mirrored");
        g.applySettings(be.getLevel(), be.getBlockPos(), be.interval, be.max, false, be.muted, mirrored);
    }

    /** Włączenie/wyłączenie wejścia (bufor: bocznego wyjścia): "left", "back" albo "right". */
    @LuaFunction(mainThread = true)
    public final void setInputEnabled(String side, boolean enabled) throws LuaException {
        GateBlock g = gate();
        GateType t = g.getGateType();
        if (t.switchableSides() == 0) throw new LuaException("This gate has no switchable sides");
        int bit = switch (side) {
            case "left" -> GateType.IN_L;
            case "back" -> GateType.IN_B;
            case "right" -> GateType.IN_R;
            default -> throw new LuaException("Side must be left, back or right");
        };
        if ((t.switchableSides() & bit) == 0) throw new LuaException("This gate has no switchable " + side + " side");
        int cur = GateBlock.disabledMask(be.getBlockState());
        int mask = enabled ? cur & ~bit : cur | bit;
        if (t.canDisableInputs() && (mask & t.inputs()) == t.inputs()) throw new LuaException("At least one input must stay enabled");
        apply(g, mask, be.quiet);
    }

    /** Zatrzask RS: czy boki (wejścia) nie świecą razem z wyjściami. */
    @LuaFunction(mainThread = true)
    public final void setQuiet(boolean quiet) throws LuaException {
        GateBlock g = gate();
        if (g.getGateType() != GateType.RS_LATCH) throw new LuaException("Not an RS latch");
        apply(g, GateBlock.disabledMask(be.getBlockState()), quiet);
    }

    private void apply(GateBlock g, int disabled, boolean quiet) {
        g.applySettings(be.getLevel(), be.getBlockPos(), be.interval, be.max, false, be.muted,
                be.getBlockState().getValue(GateBlock.MIRRORED), disabled, quiet);
    }

    private GateBlock requireCounter() throws LuaException {
        GateBlock g = gate();
        if (g.getGateType() != GateType.COUNTER) throw new LuaException("Not a counter");
        return g;
    }
}
