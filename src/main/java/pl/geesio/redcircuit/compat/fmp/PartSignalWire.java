package pl.geesio.redcircuit.compat.fmp;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.IFaceRedstonePart;
import net.minecraft.nbt.NBTTagCompound;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.WireNetwork;

/** Przewód ze stopu sygnałowego (color = -1) albo izolowany (0..15) jako część ForgeMultipart. */
public class PartSignalWire extends PartWire implements IFaceRedstonePart {
    public static final String TYPE = "redcircuit_signal_wire";

    private int color = -1;
    private int power;

    public PartSignalWire setColor(int color) {
        this.color = color;
        return this;
    }

    public int getPower() {
        return power;
    }

    public void setPowerSilently(int value) {
        power = value;
    }

    public void setPower(int value) {
        if (power == value) return;
        power = value;
        tile().markDirty();
        if (color < 0) getWriteStream().writeByte(value);   // stop sygnałowy zmienia wygląd
    }

    @Override
    public void read(MCDataInput packet) {
        power = packet.readByte();
        tile().markRender();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public BlockFaceWire hostBlock() {
        return (BlockFaceWire) (color < 0 ? ModBlocks.signalAlloyWire : ModBlocks.insulatedWire);
    }

    @Override
    public int meta() {
        return Math.max(0, color);
    }

    @Override
    public int wireType() {
        return color;
    }

    @Override
    protected int renderPower() {
        return power;
    }

    // ------------------------------------------------------------------ redstone

    @Override
    public int getFace() {
        return face;
    }

    @Override
    public int strongPowerLevel(int side) {
        return WireNetwork.isEmitting() ? power : 0;
    }

    @Override
    public int weakPowerLevel(int side) {
        return WireNetwork.isEmitting() ? power : 0;
    }

    @Override
    public boolean canConnectRedstone(int side) {
        return true;
    }

    // ------------------------------------------------------------------ zapis

    @Override
    public void save(NBTTagCompound tag) {
        super.save(tag);
        tag.setByte("Color", (byte) color);
        tag.setByte("Power", (byte) power);
    }

    @Override
    public void load(NBTTagCompound tag) {
        super.load(tag);
        color = tag.getByte("Color");
        power = tag.getByte("Power");
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        super.writeDesc(packet);
        packet.writeByte(color);
        packet.writeByte(power);
    }

    @Override
    public void readDesc(MCDataInput packet) {
        super.readDesc(packet);
        color = packet.readByte();
        power = packet.readByte();
    }
}
