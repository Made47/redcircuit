package pl.geesio.redcircuit.compat.fmp;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.energy.EnergyBuffer;
import pl.geesio.redcircuit.energy.IEnergyNode;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Pos;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.WireLinks;
import pl.geesio.redcircuit.wire.WireTypes;

/**
 * Przewód ze stopu energetycznego jako część ForgeMultipart: wyrównuje energię z sąsiednimi przewodami
 * (częściami i blokami) i akumulatorami, oddaje ją maszynom, a z generatorów sam ją pobiera
 * (blok wieloczęściowy nie przyjmuje energii RF od innych bloków).
 */
public class PartPowerWire extends PartWire implements IEnergyNode {
    public static final String TYPE = "redcircuit_power_wire";
    public static final int CAPACITY = 2_000, RATE = 500;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, RATE, RATE, () -> {
        if (tile() != null) tile().markDirty();
    });

    @Override
    public EnergyBuffer buffer() {
        return energy;
    }

    @Override
    public int rate() {
        return RATE;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public BlockFaceWire hostBlock() {
        return (BlockFaceWire) ModBlocks.powerAlloyWire;
    }

    @Override
    public int wireType() {
        return WireTypes.POWER;
    }

    @Override
    public boolean doesTick() {
        return true;
    }

    @Override
    public void update() {
        World world = world();
        if (world.isRemote) return;
        Pos pos = pos();
        ForgeDirection side = side();
        for (ForgeDirection dir : Dirs.ALL) {
            if (Dirs.sameAxis(dir, side)) continue;
            WireLinks.Target t = target(world, dir);
            if (t.pos == null) {
                if (t.link == WireLinks.Link.DEVICE) exchange(world, pos.offset(dir), dir.getOpposite());
                continue;
            }
            if (t.face >= 0) {
                if (FmpWireHost.part(world, t.pos.x, t.pos.y, t.pos.z, t.face) instanceof PartPowerWire other) {
                    equalize(energy, other.energy);
                }
            } else if (world.getTileEntity(t.pos.x, t.pos.y, t.pos.z) instanceof IEnergyNode node) {
                equalize(energy, node.buffer());
            }
        }
        // Blok, na którym leży przewód (np. maszyna albo generator)
        exchange(world, pos.offset(side), side.getOpposite());
    }

    /** Wymiana z blokiem obok: węzły - wyrównanie, generatory - pobór, odbiorcy - oddanie. */
    private void exchange(World world, Pos target, ForgeDirection from) {
        TileEntity te = world.getTileEntity(target.x, target.y, target.z);
        if (te instanceof IEnergyNode node) {
            equalize(energy, node.buffer());
            return;
        }
        if (te instanceof IEnergyProvider p && p.canConnectEnergy(from) && energy.space() > 0) {
            int got = p.extractEnergy(from, Math.min(RATE, energy.space()), false);
            if (got > 0) energy.addInternal(got);
        }
        if (te instanceof IEnergyReceiver r && r.canConnectEnergy(from) && energy.get() > 0) {
            int sent = r.receiveEnergy(from, Math.min(energy.get(), RATE), false);
            if (sent > 0) energy.set(energy.get() - sent);
        }
    }

    /** Wyrównuje procentowe naładowanie dwóch magazynów (w obie strony). */
    static void equalize(EnergyBuffer a, EnergyBuffer b) {
        long e1 = a.get(), c1 = a.capacity, e2 = b.get(), c2 = b.capacity;
        long target1 = (e1 + e2) * c1 / (c1 + c2);
        long diff = e1 - target1;
        if (diff > 0) {
            int moved = b.addInternal((int) Math.min(diff, RATE));
            a.set((int) (e1 - moved));
        } else if (diff < 0) {
            int moved = a.addInternal((int) Math.min(-diff, RATE));
            b.set((int) (e2 - moved));
        }
    }

    @Override
    public void save(NBTTagCompound tag) {
        super.save(tag);
        energy.save(tag);
    }

    @Override
    public void load(NBTTagCompound tag) {
        super.load(tag);
        energy.load(tag);
    }
}
