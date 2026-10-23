package pl.geesio.redcircuit.compat.fmp;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import codechicken.multipart.TileMultipart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Pos;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.WireConnections;
import pl.geesio.redcircuit.wire.WireLinks;
import pl.geesio.redcircuit.wire.WireNetwork;
import pl.geesio.redcircuit.wire.WireTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wspólna baza przewodów-części ForgeMultipart: przewód leży na jednej ścianie bloku (slot = ta ściana),
 * dzięki czemu dzieli blok z mikroblokami (osłony, panele) i przewodami innych rodzajów.
 */
public abstract class PartWire extends TMultiPart implements TSlottedPart, JNormalOcclusion {
    /** Ściana bloku, na której leży przewód (ForgeDirection 0..5). */
    protected int face;

    public int face() {
        return face;
    }

    public ForgeDirection side() {
        return Dirs.of(face);
    }

    public PartWire setFace(ForgeDirection side) {
        this.face = side.ordinal();
        return this;
    }

    /** Blok-przewód tego samego rodzaju (wymiary, tekstura, drop). */
    public abstract BlockFaceWire hostBlock();

    /** Metadane bloku-przewodu (kolor przewodu izolowanego). */
    public int meta() {
        return 0;
    }

    public abstract int wireType();

    /** Sygnał do wyglądu (stop sygnałowy świeci). */
    protected int renderPower() {
        return 0;
    }

    public Pos pos() {
        return new Pos(x(), y(), z());
    }

    // ------------------------------------------------------------------ połączenia

    public WireLinks.Target target(IBlockAccess world, ForgeDirection dir) {
        Pos pos = pos();
        int type = wireType();
        ForgeDirection side = side();
        BlockFaceWire host = hostBlock();
        return WireLinks.link(world, pos, type, side, dir, f -> {
            Integer o = FmpWireHost.typeAt(world, pos.x, pos.y, pos.z, f);
            return o != null && WireTypes.compatible(type, o);
        }, true, (w, np, nb) -> host.connectsToDevice(w, pos, side, dir, np, nb));
    }

    public WireConnections connections(IBlockAccess world) {
        WireConnections.Builder b = new WireConnections.Builder();
        ForgeDirection side = side();
        for (ForgeDirection dir : Dirs.ALL) {
            if (Dirs.sameAxis(dir, side)) continue;
            b.set(side, dir, WireLinks.reach(target(world, dir).link, side, dir));
        }
        return b.build();
    }

    private List<float[]> boxes() {
        WireConnections conn = tile() != null && world() != null ? connections(world()) : WireConnections.EMPTY;
        return hostBlock().boxes(side(), conn);
    }

    private static Cuboid6 cuboid(float[] b) {
        return new Cuboid6(Math.max(0, b[0]) / 16.0, Math.max(0, b[1]) / 16.0, Math.max(0, b[2]) / 16.0,
                Math.min(16, b[3]) / 16.0, Math.min(16, b[4]) / 16.0, Math.min(16, b[5]) / 16.0);
    }

    // ------------------------------------------------------------------ kształt / sloty

    @Override
    public int getSlotMask() {
        return 1 << face;
    }

    @Override
    public Iterable<Cuboid6> getOcclusionBoxes() {
        return Collections.singletonList(cuboid(hostBlock().boxes(side(), WireConnections.EMPTY).get(0)));
    }

    @Override
    public boolean occlusionTest(TMultiPart npart) {
        return NormalOcclusionTest.apply(this, npart) && super.occlusionTest(npart);
    }

    @Override
    public Iterable<IndexedCuboid6> getSubParts() {
        List<IndexedCuboid6> out = new ArrayList<>();
        for (float[] b : boxes()) out.add(new IndexedCuboid6(0, cuboid(b)));
        return out;
    }

    @Override
    public Iterable<Cuboid6> getCollisionBoxes() {
        return Collections.emptyList();
    }

    @Override
    public boolean doesTick() {
        return false;
    }

    // ------------------------------------------------------------------ drop

    public ItemStack stack() {
        return new ItemStack(hostBlock(), 1, meta());
    }

    @Override
    public Iterable<ItemStack> getDrops() {
        return Collections.singletonList(stack());
    }

    @Override
    public ItemStack pickItem(MovingObjectPosition hit) {
        return stack();
    }

    @Override
    public float getStrength(MovingObjectPosition hit, EntityPlayer player) {
        return 0.5F;
    }

    // ------------------------------------------------------------------ zapis / synchronizacja

    @Override
    public void save(NBTTagCompound tag) {
        tag.setByte("Face", (byte) face);
    }

    @Override
    public void load(NBTTagCompound tag) {
        face = tag.getByte("Face");
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        packet.writeByte(face);
    }

    @Override
    public void readDesc(MCDataInput packet) {
        face = packet.readByte();
    }

    // ------------------------------------------------------------------ zdarzenia

    @Override
    public void onAdded() {
        super.onAdded();
        if (!world().isRemote) WireNetwork.onWireChanged(world(), x(), y(), z());
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (!world().isRemote) {
            WireNetwork.onWireRemoved(world(), x(), y(), z(), hostBlock());
            WireNetwork.update(world(), x(), y(), z());   // pozostałe przewody w tym samym bloku
        }
    }

    @Override
    public void onNeighborChanged() {
        if (world().isRemote) {
            tile().markRender();
            return;
        }
        if (!BlockFaceWire.canAttach(world(), x(), y(), z(), side())) {
            TileMultipart.dropItem(stack(), world(), Vector3.fromTileEntityCenter(tile()));
            tile().remPart(this);
            return;
        }
        WireNetwork.update(world(), x(), y(), z());
    }

    @Override
    public void onPartChanged(TMultiPart part) {
        if (world().isRemote) tile().markRender();
        else if (part != this) WireNetwork.update(world(), x(), y(), z());
    }

    // ------------------------------------------------------------------ wygląd

    @Override
    @SideOnly(Side.CLIENT)
    public boolean renderStatic(Vector3 pos, int pass) {
        if (pass != 0) return false;
        RenderBlocks rb = new RenderBlocks(world());
        BlockFaceWire block = hostBlock();
        rb.setOverrideBlockTexture(block.partIcon(meta(), renderPower()));
        for (float[] b : boxes()) {
            rb.setRenderBounds(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0);
            rb.renderStandardBlock(block, x(), y(), z());
        }
        rb.clearOverrideBlockTexture();
        return true;
    }
}
