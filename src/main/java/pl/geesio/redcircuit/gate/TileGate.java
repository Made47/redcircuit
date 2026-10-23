package pl.geesio.redcircuit.gate;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Stan i ustawienia bramki (1.7.10: zamiast stanów bloku). Kierunek wyjścia, POWERED/STATE/INPUT jak w wersji 1.21,
 * ustawienia z okna (czas, maksimum licznika, wyciszenie, strony) i pamięć bramek z licznikami.
 */
public class TileGate extends TileEntity {
    public static final int MIN_INTERVAL = 4, MAX_INTERVAL = 72_000;
    public static final int MAX_COUNT = 1024;

    /** Kierunek wyjścia (2..5 = N, S, W, E). */
    public int facing = 2;
    public boolean powered, state, input, mirrored, quiet, muted;
    /** Wyłączone strony (bity GateType.IN_L/IN_B/IN_R). */
    public int disabled = -1;

    public int interval = 20;
    public int max = 4;
    public int count;
    public boolean lastL, lastB, lastR;
    public boolean latchL, latchR;
    public long due;

    @Override
    public boolean canUpdate() {
        return false;
    }

    public ForgeDirection facing() {
        return ForgeDirection.getOrientation(facing);
    }

    public GateType type() {
        Block b = worldObj == null ? null : worldObj.getBlock(xCoord, yCoord, zCoord);
        return b instanceof BlockGate g ? g.getGateType() : GateType.AND;
    }

    /** Wyłączone strony (przy pierwszym użyciu - domyślne dla typu). */
    public int disabledMask() {
        if (disabled < 0) disabled = type().defaultDisabled();
        return disabled & type().switchableSides();
    }

    public int max() {
        return Math.max(1, max);
    }

    /** Czy ustawienia różnią się od domyślnych (wtedy śrubokręt zapisuje je w przedmiocie). */
    public boolean hasCustomSettings() {
        return interval != 20 || max != 4 || muted || mirrored || quiet || disabledMask() != type().defaultDisabled();
    }

    /** Wysyła stan do klientów (wygląd, okno ustawień). */
    public void sync() {
        markDirty();
        if (worldObj != null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    // ------------------------------------------------------------------ zapis

    /** Ustawienia z okna - zapisywane też w przedmiocie po zebraniu śrubokrętem. */
    public NBTTagCompound writeSettings(NBTTagCompound tag) {
        tag.setInteger("Interval", interval);
        tag.setInteger("MaxValue", max);
        tag.setBoolean("Muted", muted);
        tag.setBoolean("Mirrored", mirrored);
        tag.setBoolean("Quiet", quiet);
        tag.setInteger("Disabled", disabledMask());
        return tag;
    }

    public void readSettings(NBTTagCompound tag) {
        if (tag.hasKey("Interval")) interval = tag.getInteger("Interval");
        if (tag.hasKey("MaxValue")) max = tag.getInteger("MaxValue");
        muted = tag.getBoolean("Muted");
        mirrored = tag.getBoolean("Mirrored");
        quiet = tag.getBoolean("Quiet");
        if (tag.hasKey("Disabled")) disabled = tag.getInteger("Disabled") & 7;
    }

    private void writeState(NBTTagCompound tag) {
        tag.setByte("Facing", (byte) facing);
        tag.setBoolean("Powered", powered);
        tag.setBoolean("State", state);
        tag.setBoolean("Input", input);
        tag.setInteger("Count", count);
        writeSettings(tag);
    }

    private void readState(NBTTagCompound tag) {
        facing = tag.getByte("Facing");
        if (facing < 2 || facing > 5) facing = 2;
        powered = tag.getBoolean("Powered");
        state = tag.getBoolean("State");
        input = tag.getBoolean("Input");
        count = tag.getInteger("Count");
        readSettings(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeState(tag);
        tag.setBoolean("L", lastL);
        tag.setBoolean("B", lastB);
        tag.setBoolean("R", lastR);
        tag.setBoolean("LatchL", latchL);
        tag.setBoolean("LatchR", latchR);
        tag.setLong("Due", due);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readState(tag);
        lastL = tag.getBoolean("L");
        lastB = tag.getBoolean("B");
        lastR = tag.getBoolean("R");
        latchL = tag.getBoolean("LatchL");
        latchR = tag.getBoolean("LatchR");
        due = tag.getLong("Due");
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeState(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readState(pkt.func_148857_g());
        if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    }
}
