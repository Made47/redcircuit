package pl.geesio.redcircuit.tube;

import cofh.api.energy.IEnergyReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.config.RCConfig;
import pl.geesio.redcircuit.energy.EnergyBuffer;

/** Akcelerator: odcinek rury, który za energię przyspiesza przejeżdżające przedmioty do 4x. */
public class TileAccelerator extends TileTube implements IEnergyReceiver {
    public static final float BOOST_SPEED = 0.5F;
    public static final int CAPACITY = 5_000;

    public final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 200, 0, this::markDirty);

    @Override
    protected void onItemEnter(TubeItem item) {
        if (worldObj == null || worldObj.isRemote) return;
        if (!energy.use(RCConfig.acceleratorRf)) return;
        item.speed = Math.max(item.speed, BOOST_SPEED);
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
        return CAPACITY;
    }
}
