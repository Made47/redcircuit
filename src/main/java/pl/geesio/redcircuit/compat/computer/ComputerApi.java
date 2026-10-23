package pl.geesio.redcircuit.compat.computer;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import pl.geesio.redcircuit.gate.BlockGate;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.gate.TileGate;
import pl.geesio.redcircuit.wireless.BlockWireless;
import pl.geesio.redcircuit.wireless.BlockWirelessTransmitter;
import pl.geesio.redcircuit.wireless.TileWireless;
import pl.geesio.redcircuit.wireless.WirelessNetwork;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Wspólne funkcje peryferiów ComputerCraft i komponentów OpenComputers: bramki ({@code redcircuit_gate}) oraz
 * nadajniki/odbiorniki bezprzewodowe ({@code redcircuit_wireless}). Wywoływać w wątku serwera.
 * Błędy zgłaszane jako {@link IllegalArgumentException} (zamieniane na błąd Lua).
 */
public final class ComputerApi {
    private ComputerApi() {}

    public static final String GATE = "redcircuit_gate", WIRELESS = "redcircuit_wireless";

    public static final String[] GATE_METHODS = {"getGateType", "getState", "getInterval", "setInterval", "getCount", "getMax",
            "setMax", "resetCount", "isMuted", "setMuted", "isMirrored", "setMirrored", "setInputEnabled", "setQuiet"};

    public static final String[] WIRELESS_METHODS = {"getFrequency", "setFrequency", "isTransmitter", "isPowered", "isChannelActive"};

    // ------------------------------------------------------------------ argumenty

    private static Object arg(Object[] args, int i) {
        if (args == null || i >= args.length) throw new IllegalArgumentException("Missing argument #" + (i + 1));
        return args[i];
    }

    static int intArg(Object[] args, int i) {
        Object o = arg(args, i);
        if (o instanceof Number n) return (int) Math.floor(n.doubleValue());
        throw new IllegalArgumentException("Argument #" + (i + 1) + ": number expected");
    }

    static boolean boolArg(Object[] args, int i) {
        Object o = arg(args, i);
        if (o instanceof Boolean b) return b;
        throw new IllegalArgumentException("Argument #" + (i + 1) + ": boolean expected");
    }

    static String strArg(Object[] args, int i) {
        Object o = arg(args, i);
        if (o instanceof String s) return s;
        if (o instanceof byte[] b) return new String(b, StandardCharsets.UTF_8);   // OpenComputers
        throw new IllegalArgumentException("Argument #" + (i + 1) + ": string expected");
    }

    private static Object[] ret(Object o) {
        return new Object[]{o};
    }

    // ------------------------------------------------------------------ bramki

    private static BlockGate gate(TileGate t) {
        World w = t.getWorldObj();
        if (w == null || t.isInvalid()) throw new IllegalArgumentException("Gate is no longer available");
        Block b = w.getBlock(t.xCoord, t.yCoord, t.zCoord);
        if (!(b instanceof BlockGate g)) throw new IllegalArgumentException("Not a gate");
        return g;
    }

    private static void apply(BlockGate g, TileGate t, int interval, int max, boolean reset, boolean muted, boolean mirrored,
                              int disabled, boolean quiet) {
        g.applySettings(t.getWorldObj(), t.xCoord, t.yCoord, t.zCoord, interval, max, reset, muted, mirrored, disabled, quiet);
    }

    public static Object[] callGate(TileGate t, String method, Object[] args) {
        BlockGate g = gate(t);
        GateType type = g.getGateType();
        switch (method) {
            case "getGateType":
                return ret(type.getSerializedName());
            case "getState": {
                Map<String, Object> out = new HashMap<>();
                out.put("type", type.getSerializedName());
                out.put("left", t.lastL);
                out.put("back", t.lastB);
                out.put("right", t.lastR);
                out.put("powered", t.powered);
                out.put("state", t.state);
                out.put("muted", t.muted);
                out.put("mirrored", t.mirrored);
                Map<String, Boolean> enabled = new HashMap<>();
                int dis = t.disabledMask();
                if ((type.switchableSides() & GateType.IN_L) != 0) enabled.put("left", (dis & GateType.IN_L) == 0);
                if ((type.switchableSides() & GateType.IN_B) != 0) enabled.put("back", (dis & GateType.IN_B) == 0);
                if ((type.switchableSides() & GateType.IN_R) != 0) enabled.put("right", (dis & GateType.IN_R) == 0);
                out.put("inputs", enabled);
                if (type == GateType.RS_LATCH) out.put("quiet", t.quiet);
                if (type.hasInterval()) out.put("interval", t.interval);
                if (type == GateType.COUNTER) {
                    out.put("count", t.count);
                    out.put("max", t.max());
                }
                return ret(out);
            }
            case "getInterval":
                requireInterval(type);
                return ret(t.interval);
            case "setInterval":
                requireInterval(type);
                apply(g, t, intArg(args, 0), t.max, false, t.muted, t.mirrored, t.disabledMask(), t.quiet);
                return null;
            case "getCount":
                requireCounter(type);
                return ret(t.count);
            case "getMax":
                requireCounter(type);
                return ret(t.max());
            case "setMax":
                requireCounter(type);
                apply(g, t, t.interval, intArg(args, 0), false, t.muted, t.mirrored, t.disabledMask(), t.quiet);
                return null;
            case "resetCount":
                requireCounter(type);
                apply(g, t, t.interval, t.max, true, t.muted, t.mirrored, t.disabledMask(), t.quiet);
                return null;
            case "isMuted":
                return ret(t.muted);
            case "setMuted":
                apply(g, t, t.interval, t.max, false, boolArg(args, 0), t.mirrored, t.disabledMask(), t.quiet);
                return null;
            case "isMirrored":
                return ret(t.mirrored);
            case "setMirrored":
                if (type != GateType.STATE_CELL && type != GateType.RS_LATCH) {
                    throw new IllegalArgumentException("Only state cells and RS latches can be mirrored");
                }
                apply(g, t, t.interval, t.max, false, t.muted, boolArg(args, 0), t.disabledMask(), t.quiet);
                return null;
            case "setInputEnabled": {
                if (type.switchableSides() == 0) throw new IllegalArgumentException("This gate has no switchable sides");
                String side = strArg(args, 0);
                int bit = switch (side) {
                    case "left" -> GateType.IN_L;
                    case "back" -> GateType.IN_B;
                    case "right" -> GateType.IN_R;
                    default -> throw new IllegalArgumentException("Side must be left, back or right");
                };
                if ((type.switchableSides() & bit) == 0) throw new IllegalArgumentException("This gate has no switchable " + side + " side");
                int cur = t.disabledMask();
                int mask = boolArg(args, 1) ? cur & ~bit : cur | bit;
                if (type.canDisableInputs() && (mask & type.inputs()) == type.inputs()) {
                    throw new IllegalArgumentException("At least one input must stay enabled");
                }
                apply(g, t, t.interval, t.max, false, t.muted, t.mirrored, mask, t.quiet);
                return null;
            }
            case "setQuiet":
                if (type != GateType.RS_LATCH) throw new IllegalArgumentException("Not an RS latch");
                apply(g, t, t.interval, t.max, false, t.muted, t.mirrored, t.disabledMask(), boolArg(args, 0));
                return null;
            default:
                throw new IllegalArgumentException("No such method: " + method);
        }
    }

    private static void requireInterval(GateType t) {
        if (!t.hasInterval()) throw new IllegalArgumentException("This gate has no interval");
    }

    private static void requireCounter(GateType t) {
        if (t != GateType.COUNTER) throw new IllegalArgumentException("Not a counter");
    }

    // ------------------------------------------------------------------ redstone bezprzewodowy

    private static BlockWireless wireless(TileWireless t) {
        World w = t.getWorldObj();
        if (w == null || t.isInvalid()) throw new IllegalArgumentException("Block is no longer available");
        Block b = w.getBlock(t.xCoord, t.yCoord, t.zCoord);
        if (!(b instanceof BlockWireless bw)) throw new IllegalArgumentException("Not a wireless block");
        return bw;
    }

    public static Object[] callWireless(TileWireless t, String method, Object[] args) {
        BlockWireless b = wireless(t);
        World w = t.getWorldObj();
        switch (method) {
            case "getFrequency":
                return ret(t.getFrequency());
            case "setFrequency": {
                int f = intArg(args, 0);
                if (f < WirelessNetwork.MIN_FREQ || f > WirelessNetwork.MAX_FREQ) {
                    throw new IllegalArgumentException("Frequency must be between " + WirelessNetwork.MIN_FREQ + " and " + WirelessNetwork.MAX_FREQ);
                }
                b.applyFrequency(w, t.xCoord, t.yCoord, t.zCoord, f);
                return null;
            }
            case "isTransmitter":
                return ret(b instanceof BlockWirelessTransmitter);
            case "isPowered":
                return ret((w.getBlockMetadata(t.xCoord, t.yCoord, t.zCoord) & 1) != 0);
            case "isChannelActive": {
                int f = args != null && args.length > 0 && args[0] != null ? intArg(args, 0) : t.getFrequency();
                return ret(WirelessNetwork.get().isActive(WirelessNetwork.clampFreq(f)));
            }
            default:
                throw new IllegalArgumentException("No such method: " + method);
        }
    }

    /** Bramka albo blok bezprzewodowy pod podanym blokiem-encją (null, gdy to nic z RedCircuit). */
    public static String typeOf(TileEntity te) {
        if (te instanceof TileGate) return GATE;
        if (te instanceof TileWireless) return WIRELESS;
        return null;
    }
}
