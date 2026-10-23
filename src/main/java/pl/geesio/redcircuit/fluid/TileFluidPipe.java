package pl.geesio.redcircuit.fluid;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import pl.geesio.redcircuit.util.Dirs;

/**
 * Rura do płynów: mały zbiornik (1000 mB). Rury wyrównują poziom między sobą, a płyn wypychają do podłączonych
 * zbiorników/maszyn - z wyjątkiem strony, z której płyn ostatnio przyszedł (żeby nie oddawać go do źródła).
 */
public class TileFluidPipe extends TileEntity implements IFluidHandler {
    public static final int CAPACITY = 1000;
    public static final int RATE = 250;          // mB na tick na stronę
    private static final int INPUT_COOLDOWN = 20;

    public final FluidTank tank = new FluidTank(CAPACITY);
    private final int[] inputCooldown = new int[6];

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        for (int i = 0; i < 6; i++) if (inputCooldown[i] > 0) inputCooldown[i]--;
        if (tank.getFluidAmount() <= 0) return;

        int start = (int) (worldObj.getTotalWorldTime() % 6);   // rotacja, żeby żadna strona nie była uprzywilejowana
        boolean changed = false;
        for (int k = 0; k < 6 && tank.getFluidAmount() > 0; k++) {
            ForgeDirection d = Dirs.of((start + k) % 6);
            if (!BlockFluidPipe.connects(worldObj, xCoord, yCoord, zCoord, d)) continue;
            int nx = xCoord + d.offsetX, ny = yCoord + d.offsetY, nz = zCoord + d.offsetZ;
            if (!worldObj.blockExists(nx, ny, nz)) continue;
            TileEntity te = worldObj.getTileEntity(nx, ny, nz);

            if (te instanceof TileFluidPipe other) {
                FluidStack mine = tank.getFluid(), theirs = other.tank.getFluid();
                if (theirs != null && !theirs.isFluidEqual(mine)) continue;
                int diff = tank.getFluidAmount() - other.tank.getFluidAmount();
                if (diff <= 1) continue;
                int move = Math.min(diff / 2, RATE);
                int moved = other.tank.fill(tank.drain(move, false), true);
                if (moved > 0) {
                    tank.drain(moved, true);
                    changed = true;
                }
                continue;
            }

            if (inputCooldown[d.ordinal()] > 0) continue;   // nie oddawaj do źródła
            if (!(te instanceof IFluidHandler dst)) continue;
            FluidStack offer = tank.drain(RATE, false);
            if (offer == null || !dst.canFill(d.getOpposite(), offer.getFluid())) continue;
            int moved = dst.fill(d.getOpposite(), offer, true);
            if (moved > 0) {
                tank.drain(moved, true);
                changed = true;
            }
        }
        if (changed) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tank.setFluid(null);
        tank.readFromNBT(tag.getCompoundTag("Tank"));
    }

    // ------------------------------------------------------------------ IFluidHandler (zapamiętuje, skąd przyszedł płyn)

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        int filled = tank.fill(resource, doFill);
        if (filled > 0 && doFill) {
            if (from != null && from != ForgeDirection.UNKNOWN) inputCooldown[from.ordinal()] = INPUT_COOLDOWN;
            markDirty();
        }
        return filled;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || !resource.isFluidEqual(tank.getFluid())) return null;
        return tank.drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return true;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return true;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[]{tank.getInfo()};
    }
}
