package pl.geesio.redcircuit.compat.cbm;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.part.redstone.FaceRedstonePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.registry.ModBlocks;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.RedWireBlock;
import pl.geesio.redcircuit.wire.WireNetwork;

/** Przewód ze stopu sygnałowego (color = -1) albo izolowany (0..15) jako część CB Multipart. */
public class SignalWirePart extends RCWirePart implements FaceRedstonePart {
    private int color = -1;
    private int power;

    public SignalWirePart setColor(int color) {
        this.color = color;
        return this;
    }

    public int getPower() {
        return power;
    }

    /** Ustawienie bez synchronizacji (część jeszcze nie jest w świecie). */
    public void setPowerSilently(int value) {
        power = value;
    }

    public void setPower(int value) {
        if (power == value) return;
        power = value;
        tile().setChanged();
        sendUpdate(p -> p.writeByte(value));
    }

    @Override
    public void readUpdate(MCDataInput packet) {
        power = packet.readByte();
        tile().markRender();
    }

    @Override
    public MultipartType<?> getType() {
        return CbmCompat.SIGNAL_WIRE.get();
    }

    @Override
    public FaceWireBlock hostBlock() {
        Block b = color < 0 ? ModBlocks.SIGNAL_ALLOY_WIRE.get() : ModBlocks.INSULATED_WIRES.get(color).get();
        return (FaceWireBlock) b;
    }

    @Override
    public BlockState renderState() {
        BlockState st = super.renderState();
        return st.hasProperty(RedWireBlock.POWER) ? st.setValue(RedWireBlock.POWER, power) : st;
    }

    @Override
    public int wireType() {
        return color;
    }

    @Override
    protected boolean device(BlockGetter level, BlockPos np, BlockState ns, Direction dir) {
        return RedWireBlock.isRedstoneDevice(ns, dir);
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
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        super.save(tag, registries);
        tag.putByte("Color", (byte) color);
        tag.putByte("Power", (byte) power);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        super.load(tag, registries);
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
