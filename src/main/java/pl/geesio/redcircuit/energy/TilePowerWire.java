package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.wire.TileWire;

/** Przewód energetyczny (na ścianie albo w obudowie): węzeł sieci energii - 2000 RF, 500 RF/t na stronę. */
public class TilePowerWire extends TileWire implements IEnergyHandler, IEnergyNode {
    public static final int CAPACITY = 2_000, RATE = 500;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, RATE, RATE, this::markDirty);

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) EnergyNodes.distribute(this, this);
    }

    @Override
    public EnergyBuffer buffer() {
        return energy;
    }

    @Override
    public int rate() {
        return RATE;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        energy.save(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
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
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        return energy.extract(maxExtract, simulate);
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
