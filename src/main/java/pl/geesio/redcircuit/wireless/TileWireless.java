package pl.geesio.redcircuit.wireless;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/** Częstotliwość nadajnika / odbiornika. */
public class TileWireless extends TileEntity {
    protected int frequency = 1;

    @Override
    public boolean canUpdate() {
        return false;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int f) {
        frequency = WirelessNetwork.clampFreq(f);
        markDirty();
        if (worldObj != null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Frequency", frequency);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        frequency = tag.hasKey("Frequency") ? WirelessNetwork.clampFreq(tag.getInteger("Frequency")) : 1;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Frequency", frequency);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        frequency = pkt.func_148857_g().getInteger("Frequency");
    }
}
