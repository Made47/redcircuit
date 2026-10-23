package pl.geesio.redcircuit.fluid;

import cofh.api.energy.IEnergyReceiver;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidHandler;
import pl.geesio.redcircuit.config.RCConfig;
import pl.geesio.redcircuit.energy.EnergyBuffer;
import pl.geesio.redcircuit.machine.BlockFacing;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Pos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Pompa: zasysa płyn z tyłu (zbiornik innego moda albo źródła płynu w świecie, do 256 bloków połączonego jeziora)
 * i tłoczy go do przodu (zbiornik / rura; gdy z przodu jest pusto - wylewa blok płynu). Sygnał redstone ją zatrzymuje.
 */
public class TilePump extends TileEntity implements IEnergyReceiver {
    public static final int CAPACITY = 8_000;
    private static final int INTERVAL = 10, SEARCH_LIMIT = 256, BUCKET = 1000;

    public final FluidTank tank = new FluidTank(CAPACITY);
    public final EnergyBuffer energy = new EnergyBuffer(10_000, 400, 0, this::markDirty);

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || worldObj.getTotalWorldTime() % INTERVAL != 0) return;
        if (worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) return;   // redstone = stop
        ForgeDirection facing = BlockFacing.facing(getBlockMetadata());
        intake(facing);
        output(facing);
    }

    private int space() {
        return CAPACITY - tank.getFluidAmount();
    }

    // ------------------------------------------------------------------ ssanie

    private void intake(ForgeDirection facing) {
        int cost = RCConfig.pumpRfPerBucket;
        ForgeDirection back = facing.getOpposite();
        int bx = xCoord + back.offsetX, by = yCoord + back.offsetY, bz = zCoord + back.offsetZ;
        if (space() < BUCKET || energy.get() < cost || !worldObj.blockExists(bx, by, bz)) return;

        TileEntity te = worldObj.getTileEntity(bx, by, bz);
        if (te instanceof IFluidHandler src) {
            FluidStack sim = src.drain(facing, Math.min(BUCKET, space()), false);
            if (sim == null || sim.amount <= 0 || !src.canDrain(facing, sim.getFluid())) return;
            int accepted = tank.fill(sim, false);
            if (accepted <= 0) return;
            FluidStack real = src.drain(facing, accepted, true);
            tank.fill(real, true);
            energy.use(cost / 2);
            markDirty();
            return;
        }

        Fluid fluid = FluidUtil.fluidAt(worldObj, bx, by, bz);
        if (fluid == null) return;
        Pos source = findSource(worldObj, new Pos(bx, by, bz), fluid);
        if (source == null) return;
        if (tank.fill(new FluidStack(fluid, BUCKET), false) < BUCKET) return;
        FluidStack got = FluidUtil.pickup(worldObj, source.x, source.y, source.z);
        if (got == null) return;
        tank.fill(got, true);
        energy.use(cost);
        markDirty();
    }

    /** Szuka najdalszego (ostatnio odwiedzonego) źródła płynu połączonego z punktem startowym. */
    private static Pos findSource(World world, Pos start, Fluid fluid) {
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        Set<Pos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        Pos found = null;
        while (!queue.isEmpty() && seen.size() < SEARCH_LIMIT) {
            Pos p = queue.poll();
            if (FluidUtil.fluidAt(world, p.x, p.y, p.z) != fluid) continue;
            if (FluidUtil.isSource(world, p.x, p.y, p.z)) found = p;
            for (ForgeDirection d : Dirs.ALL) {
                if (d == ForgeDirection.DOWN) continue;   // nie schodź w dół - wypompowuj od góry
                Pos np = p.offset(d);
                if (np.y >= 0 && np.y < 256 && world.blockExists(np.x, np.y, np.z) && seen.add(np)) queue.add(np);
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ tłoczenie

    private void output(ForgeDirection facing) {
        if (tank.getFluidAmount() <= 0) return;
        int fx = xCoord + facing.offsetX, fy = yCoord + facing.offsetY, fz = zCoord + facing.offsetZ;
        if (!worldObj.blockExists(fx, fy, fz)) return;
        TileEntity te = worldObj.getTileEntity(fx, fy, fz);
        if (te instanceof IFluidHandler dst) {
            FluidStack sim = tank.drain(BUCKET, false);
            if (sim == null || !dst.canFill(facing.getOpposite(), sim.getFluid())) return;
            int accepted = dst.fill(facing.getOpposite(), sim, true);
            if (accepted > 0) {
                tank.drain(accepted, true);
                markDirty();
            }
            return;
        }
        // Pusto z przodu - wylej blok płynu
        Block front = worldObj.getBlock(fx, fy, fz);
        if (tank.getFluidAmount() >= BUCKET && front.isReplaceable(worldObj, fx, fy, fz)
                && FluidUtil.fluidAt(worldObj, fx, fy, fz) == null) {
            Block fb = FluidUtil.placeBlock(tank.getFluid().getFluid());
            if (fb == null) return;
            worldObj.setBlock(fx, fy, fz, fb, 0, 3);
            tank.drain(BUCKET, true);
            markDirty();
        }
    }

    // ------------------------------------------------------------------ zapis

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
        energy.save(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tank.setFluid(null);
        tank.readFromNBT(tag.getCompoundTag("Tank"));
        energy.load(tag);
    }

    // ------------------------------------------------------------------ RF

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        return energy.receive(maxReceive, simulate);
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return energy.get();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return energy.capacity;
    }
}
