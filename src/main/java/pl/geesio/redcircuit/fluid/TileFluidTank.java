package pl.geesio.redcircuit.fluid;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * Zbiornik na ciecz: 16 000 mB (16 wiader). Ciecz spływa do zbiornika postawionego pod spodem.
 * Zawartość zostaje w przedmiocie po rozbiciu bloku.
 */
public class TileFluidTank extends TileEntity implements IFluidHandler {
    public static final int CAPACITY = 16_000;
    private static final int FLOW_DOWN = 1_000;

    public final FluidTank tank = new FluidTank(CAPACITY);
    private int lastAmount = -1, lastLight = -1;
    private String lastFluid = "";
    /** Rozbity przez gracza w trybie kreatywnym - bez przedmiotu. */
    public boolean noDrop;

    /** Wypełnienie 0..1 (rysowanie, komparator). */
    public float fill() {
        return tank.getFluidAmount() / (float) CAPACITY;
    }

    public int lightLevel() {
        FluidStack fs = tank.getFluid();
        return fs == null || fs.getFluid() == null ? 0 : fs.getFluid().getLuminosity(fs);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        // Grawitacja: ciecz spływa do zbiornika poniżej (gazy zostają)
        if (tank.getFluidAmount() > 0 && worldObj.getTotalWorldTime() % 5 == 0 && !tank.getFluid().getFluid().isGaseous(tank.getFluid())
                && worldObj.getTileEntity(xCoord, yCoord - 1, zCoord) instanceof TileFluidTank below) {
            FluidStack moving = tank.drain(FLOW_DOWN, false);
            int accepted = below.tank.fill(moving, true);
            if (accepted > 0) tank.drain(accepted, true);
        }
        checkChanged();
    }

    /** Synchronizacja z klientem, komparatory i światło po zmianie zawartości. */
    private void checkChanged() {
        FluidStack fs = tank.getFluid();
        String name = fs == null ? "" : fs.getFluid().getName();
        int amount = tank.getFluidAmount();
        if (amount == lastAmount && name.equals(lastFluid)) return;
        lastAmount = amount;
        lastFluid = name;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
        int light = lightLevel();
        if (light != lastLight) {
            lastLight = light;
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
        }
    }

    // ------------------------------------------------------------------ zapis / synchronizacja

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

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        int light = lightLevel();
        tank.setFluid(null);
        tank.readFromNBT(pkt.func_148857_g().getCompoundTag("Tank"));
        if (light != lightLevel()) worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
    }

    // ------------------------------------------------------------------ IFluidHandler

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return tank.fill(resource, doFill);
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
