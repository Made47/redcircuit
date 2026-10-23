package pl.geesio.redcircuit.compat.computer;

import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Visibility;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.gate.TileGate;
import pl.geesio.redcircuit.wireless.TileWireless;

/**
 * Integracja z OpenComputers: komponenty {@code redcircuit_gate} i {@code redcircuit_wireless} (przez adapter).
 * Funkcje nie są "direct", więc OpenComputers wykonuje je w wątku serwera.
 */
public final class OcCompat {
    private OcCompat() {}

    public static void init() {
        Driver.add(new DriverSidedTileEntity() {
            @Override
            public Class<?> getTileEntityClass() {
                return TileGate.class;
            }

            @Override
            public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
                return world.getTileEntity(x, y, z) instanceof TileGate t ? new GateEnv(t) : null;
            }
        });
        Driver.add(new DriverSidedTileEntity() {
            @Override
            public Class<?> getTileEntityClass() {
                return TileWireless.class;
            }

            @Override
            public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
                return world.getTileEntity(x, y, z) instanceof TileWireless t ? new WirelessEnv(t) : null;
            }
        });
        RedCircuit.LOG.info("OpenComputers: komponenty bramek i redstone bezprzewodowego włączone");
    }

    /** Środowisko komponentu związane z blokiem-encją. */
    public abstract static class TileEnv<T extends TileEntity> extends li.cil.oc.api.prefab.ManagedEnvironment {
        protected final T tileEntity;

        protected TileEnv(T tile, String name) {
            this.tileEntity = tile;
            setNode(Network.newNode(this, Visibility.Network).withComponent(name).create());
        }
    }

    public static class GateEnv extends TileEnv<TileGate> {
        public GateEnv(TileGate tile) {
            super(tile, ComputerApi.GATE);
        }

        private Object[] call(String m, Arguments a) {
            return ComputerApi.callGate(tileEntity, m, a.toArray());
        }

        @Callback(doc = "function():string -- gate type, e.g. \"timer\"")
        public Object[] getGateType(Context c, Arguments a) {
            return call("getGateType", a);
        }

        @Callback(doc = "function():table -- inputs, output and settings")
        public Object[] getState(Context c, Arguments a) {
            return call("getState", a);
        }

        @Callback(doc = "function():number -- interval in ticks")
        public Object[] getInterval(Context c, Arguments a) {
            return call("getInterval", a);
        }

        @Callback(doc = "function(ticks:number) -- set interval")
        public Object[] setInterval(Context c, Arguments a) {
            return call("setInterval", a);
        }

        @Callback(doc = "function():number -- counter value")
        public Object[] getCount(Context c, Arguments a) {
            return call("getCount", a);
        }

        @Callback(doc = "function():number -- counter maximum")
        public Object[] getMax(Context c, Arguments a) {
            return call("getMax", a);
        }

        @Callback(doc = "function(max:number) -- set counter maximum")
        public Object[] setMax(Context c, Arguments a) {
            return call("setMax", a);
        }

        @Callback(doc = "function() -- reset counter")
        public Object[] resetCount(Context c, Arguments a) {
            return call("resetCount", a);
        }

        @Callback(doc = "function():boolean")
        public Object[] isMuted(Context c, Arguments a) {
            return call("isMuted", a);
        }

        @Callback(doc = "function(muted:boolean)")
        public Object[] setMuted(Context c, Arguments a) {
            return call("setMuted", a);
        }

        @Callback(doc = "function():boolean")
        public Object[] isMirrored(Context c, Arguments a) {
            return call("isMirrored", a);
        }

        @Callback(doc = "function(mirrored:boolean) -- state cell / RS latch")
        public Object[] setMirrored(Context c, Arguments a) {
            return call("setMirrored", a);
        }

        @Callback(doc = "function(side:string, enabled:boolean) -- side: left, back, right")
        public Object[] setInputEnabled(Context c, Arguments a) {
            return call("setInputEnabled", a);
        }

        @Callback(doc = "function(quiet:boolean) -- RS latch")
        public Object[] setQuiet(Context c, Arguments a) {
            return call("setQuiet", a);
        }
    }

    public static class WirelessEnv extends TileEnv<TileWireless> {
        public WirelessEnv(TileWireless tile) {
            super(tile, ComputerApi.WIRELESS);
        }

        private Object[] call(String m, Arguments a) {
            return ComputerApi.callWireless(tileEntity, m, a.toArray());
        }

        @Callback(doc = "function():number")
        public Object[] getFrequency(Context c, Arguments a) {
            return call("getFrequency", a);
        }

        @Callback(doc = "function(frequency:number) -- 1-5000")
        public Object[] setFrequency(Context c, Arguments a) {
            return call("setFrequency", a);
        }

        @Callback(doc = "function():boolean")
        public Object[] isTransmitter(Context c, Arguments a) {
            return call("isTransmitter", a);
        }

        @Callback(doc = "function():boolean -- transmitting / receiving")
        public Object[] isPowered(Context c, Arguments a) {
            return call("isPowered", a);
        }

        @Callback(doc = "function([frequency:number]):boolean")
        public Object[] isChannelActive(Context c, Arguments a) {
            return call("isChannelActive", a);
        }
    }
}
