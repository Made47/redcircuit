package pl.geesio.redcircuit.wireless;

/** Odbiornik: co 2 ticki sprawdza, czy jego częstotliwość jest aktywna. */
public class TileWirelessReceiver extends TileWireless {
    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || (worldObj.getTotalWorldTime() & 1) != 0) return;
        if (!(getBlockType() instanceof BlockWirelessReceiver receiver)) return;
        receiver.setOutput(worldObj, xCoord, yCoord, zCoord, WirelessNetwork.get().isActive(frequency));
    }
}
