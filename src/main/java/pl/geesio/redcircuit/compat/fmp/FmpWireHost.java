package pl.geesio.redcircuit.compat.fmp;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import codechicken.multipart.TileMultipart;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.PartWires;
import pl.geesio.redcircuit.wire.TileWire;

import java.util.ArrayList;
import java.util.List;

/** Dostęp sieci przewodów do przewodów-części w blokach ForgeMultipart oraz stawianie takich przewodów. */
public class FmpWireHost implements PartWires.Host, PartWires.Placer {

    public static TileMultipart tile(IBlockAccess world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof TileMultipart t ? t : null;
    }

    /** Przewód-część leżący na ścianie {@code face}, albo null. */
    public static PartWire part(IBlockAccess world, int x, int y, int z, int face) {
        TileMultipart t = tile(world, x, y, z);
        if (t == null) return null;
        for (TMultiPart p : t.jPartList()) {
            if (p instanceof PartWire w && w.face() == face) return w;
        }
        return null;
    }

    public static Integer typeAt(IBlockAccess world, int x, int y, int z, int face) {
        PartWire w = part(world, x, y, z, face);
        return w == null ? null : w.wireType();
    }

    // ------------------------------------------------------------------ PartWires.Host

    @Override
    public Integer wireAt(IBlockAccess world, int x, int y, int z, int face) {
        return typeAt(world, x, y, z, face);
    }

    @Override
    public boolean isHost(IBlockAccess world, int x, int y, int z) {
        return tile(world, x, y, z) != null;
    }

    @Override
    public boolean slotTaken(IBlockAccess world, int x, int y, int z, int face) {
        TileMultipart t = tile(world, x, y, z);
        if (t == null) return false;
        for (TMultiPart p : t.jPartList()) {
            if (p instanceof TSlottedPart s && (s.getSlotMask() & (1 << face)) != 0) return true;
        }
        return false;
    }

    @Override
    public int getSignal(IBlockAccess world, int x, int y, int z, int face, int ch) {
        PartWire w = part(world, x, y, z, face);
        if (w instanceof PartSignalWire s) return s.getPower();
        if (w instanceof PartBundledWire b && ch >= 0) return b.getChannel(ch);
        return 0;
    }

    @Override
    public void setSignal(World world, int x, int y, int z, int face, int ch, int value) {
        PartWire w = part(world, x, y, z, face);
        if (w instanceof PartSignalWire s) s.setPower(value);
        else if (w instanceof PartBundledWire b && ch >= 0) b.setChannel(ch, value);
    }

    @Override
    public void notifyChanged(World world, int x, int y, int z, int face) {
        Block block = world.getBlock(x, y, z);
        world.notifyBlocksOfNeighborChange(x, y, z, block);
        // przewód silnie zasila sąsiednie bloki - powiadom też ich sąsiadów
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            int nx = x + d.offsetX, ny = y + d.offsetY, nz = z + d.offsetZ;
            if (!world.blockExists(nx, ny, nz)) continue;
            Block nb = world.getBlock(nx, ny, nz);
            if (nb.isNormalCube(world, nx, ny, nz)) {
                world.notifyBlocksOfNeighborChange(nx, ny, nz, block, d.getOpposite().ordinal());
            }
        }
    }

    // ------------------------------------------------------------------ PartWires.Placer

    @Override
    public boolean accepts(IBlockAccess world, int x, int y, int z) {
        if (tile(world, x, y, z) != null) return true;
        // zwykły przewód na jednej ścianie da się zamienić w część
        return world.getBlock(x, y, z) instanceof BlockFaceWire && world.getTileEntity(x, y, z) instanceof TileWire;
    }

    @Override
    public boolean place(World world, int x, int y, int z, ForgeDirection face, BlockFaceWire block, int meta) {
        PartWire part = FmpCompat.newPart(block, meta);
        if (part == null) return false;
        part.setFace(face);
        if (!BlockFaceWire.canAttach(world, x, y, z, face)) return false;
        BlockCoord pos = new BlockCoord(x, y, z);
        // Przewód-blok na kilku ścianach: konwerter FMP zwraca tylko jedną część, więc rozkładamy go ręcznie
        if (world.getBlock(x, y, z) instanceof BlockFaceWire wire && world.getTileEntity(x, y, z) instanceof TileWire t
                && t.faceCount() > 1) {
            if (t.hasFace(face.ordinal())) return false;
            if (world.isRemote) return true;
            int wmeta = world.getBlockMetadata(x, y, z) & 15;
            List<PartWire> parts = new ArrayList<>();
            boolean energy = true;   // energia przewodu trafia do pierwszej części
            for (ForgeDirection f : ForgeDirection.VALID_DIRECTIONS) {
                if (!t.hasFace(f.ordinal())) continue;
                PartWire p = WireConverter.fromBlock(wire, wmeta, t, f, energy);
                energy = false;
                if (p != null) parts.add(p);
            }
            world.setBlockToAir(x, y, z);
            for (PartWire p : parts) {
                if (TileMultipart.canPlacePart(world, pos, p)) TileMultipart.addPart(world, pos, p);
                else TileMultipart.dropItem(p.stack(), world, new codechicken.lib.vec.Vector3(x + 0.5, y + 0.5, z + 0.5));
            }
            if (!TileMultipart.canPlacePart(world, pos, part)) return false;
            TileMultipart.addPart(world, pos, part);
            return true;
        }
        if (!TileMultipart.canPlacePart(world, pos, part)) return false;
        if (!world.isRemote) TileMultipart.addPart(world, pos, part);
        return true;
    }
}
