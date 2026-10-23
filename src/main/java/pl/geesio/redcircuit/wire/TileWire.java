package pl.geesio.redcircuit.wire;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * Dane przewodu: ściany, do których jest przyklejony (bity 0-5 = ForgeDirection), poziom sygnału
 * oraz 16 kanałów kabla wiązkowego. Przewody nie tykają.
 */
public class TileWire extends TileEntity {
    public int faces;
    public int power;
    public final byte[] channels = new byte[16];

    @Override
    public boolean canUpdate() {
        return false;
    }

    public boolean hasFace(int side) {
        return (faces & (1 << side)) != 0;
    }

    public int faceCount() {
        return Integer.bitCount(faces & 63);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("Faces", (byte) faces);
        tag.setByte("Power", (byte) power);
        tag.setByteArray("Channels", channels.clone());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        faces = tag.getByte("Faces") & 63;
        power = tag.getByte("Power");
        byte[] ch = tag.getByteArray("Channels");
        for (int i = 0; i < 16 && i < ch.length; i++) channels[i] = ch[i];
    }

    /** Do klienta trafiają tylko ściany i sygnał (wygląd). */
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("Faces", (byte) faces);
        tag.setByte("Power", (byte) power);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.func_148857_g();
        faces = tag.getByte("Faces") & 63;
        power = tag.getByte("Power");
        if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    }
}
