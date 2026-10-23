package pl.geesio.redcircuit.compat.computer;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.tileentity.TileEntity;
import pl.geesio.redcircuit.gate.TileGate;
import pl.geesio.redcircuit.wireless.TileWireless;

/** Peryferium ComputerCraft dla bramki albo bloku bezprzewodowego; funkcje wykonywane w wątku serwera. */
public class CcPeripheral implements IPeripheral {
    private final TileEntity tile;
    private final String type;

    public CcPeripheral(TileEntity tile, String type) {
        this.tile = tile;
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String[] getMethodNames() {
        return ComputerApi.GATE.equals(type) ? ComputerApi.GATE_METHODS : ComputerApi.WIRELESS_METHODS;
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments)
            throws LuaException, InterruptedException {
        String[] names = getMethodNames();
        if (method < 0 || method >= names.length) throw new LuaException("No such method");
        String name = names[method];
        return context.executeMainThreadTask(new ILuaTask() {
            @Override
            public Object[] execute() throws LuaException {
                try {
                    if (tile instanceof TileGate g) return ComputerApi.callGate(g, name, arguments);
                    if (tile instanceof TileWireless w) return ComputerApi.callWireless(w, name, arguments);
                    throw new LuaException("Block is no longer available");
                } catch (IllegalArgumentException e) {
                    throw new LuaException(e.getMessage());
                }
            }
        });
    }

    @Override
    public void attach(IComputerAccess computer) {
    }

    @Override
    public void detach(IComputerAccess computer) {
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof CcPeripheral p && p.tile == tile;
    }
}
