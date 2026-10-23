package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Pos;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.TileWire;
import pl.geesio.redcircuit.wire.WireLinks;

/**
 * Rozdział energii w sieci węzłów (port EnergyNodeBlockEntity z 1.21): sąsiednie węzły wyrównują
 * poziom naładowania (procentowo), a energia jest wypychana do odbiorców (maszyny, maszyny innych modów).
 */
public final class EnergyNodes {
    private EnergyNodes() {}

    public static void distribute(TileEntity self, IEnergyNode node) {
        World world = self.getWorldObj();
        int x = self.xCoord, y = self.yCoord, z = self.zCoord;
        EnergyBuffer energy = node.buffer();
        for (ForgeDirection d : Dirs.ALL) {
            if (energy.get() <= 0) return;
            int nx = x + d.offsetX, ny = y + d.offsetY, nz = z + d.offsetZ;
            if (!world.blockExists(nx, ny, nz)) continue;
            TileEntity te = world.getTileEntity(nx, ny, nz);
            if (te instanceof IEnergyNode other) {
                equalize(node, other);
            } else if (te instanceof IEnergyReceiver r && r.canConnectEnergy(d.getOpposite())) {
                int sent = r.receiveEnergy(d.getOpposite(), Math.min(energy.get(), node.rate()), false);
                if (sent > 0) energy.set(energy.get() - sent);
            }
        }

        // Przewody połączone przez narożniki zewnętrzne (po przekątnej)
        if (self instanceof TileWire wire && world.getBlock(x, y, z) instanceof BlockFaceWire block) {
            Pos pos = new Pos(x, y, z);
            for (ForgeDirection side : Dirs.ALL) {
                if (!wire.hasFace(side.ordinal())) continue;
                for (ForgeDirection dir : Dirs.ALL) {
                    if (Dirs.sameAxis(dir, side)) continue;
                    Pos n = pos.offset(dir), c = n.offset(side);
                    if (!world.blockExists(n.x, n.y, n.z) || !world.blockExists(c.x, c.y, c.z)) continue;
                    WireLinks.Target t = block.target(world, pos, side, dir);
                    if (t.link != WireLinks.Link.CORNER) continue;
                    if (world.getTileEntity(t.pos.x, t.pos.y, t.pos.z) instanceof IEnergyNode other) equalize(node, other);
                }
            }
        }
    }

    /** Przesyła energię do sąsiedniego węzła, jeśli ten jest procentowo mniej naładowany. */
    public static void equalize(IEnergyNode a, IEnergyNode b) {
        EnergyBuffer ea = a.buffer(), eb = b.buffer();
        long e1 = ea.get(), c1 = ea.capacity, e2 = eb.get(), c2 = eb.capacity;
        if (e1 * c2 <= e2 * c1) return;
        long target1 = (e1 + e2) * c1 / (c1 + c2);
        int move = (int) Math.min(e1 - target1, Math.min(a.rate(), b.rate()));
        if (move <= 0) return;
        int accepted = eb.addInternal(move);
        if (accepted > 0) ea.set((int) (e1 - accepted));
    }
}
