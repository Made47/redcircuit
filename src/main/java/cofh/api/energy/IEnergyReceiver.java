package cofh.api.energy;

import net.minecraftforge.common.util.ForgeDirection;

/** RF API: blok-encja przyjmująca energię. */
public interface IEnergyReceiver extends IEnergyConnection {
    int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate);

    int getEnergyStored(ForgeDirection from);

    int getMaxEnergyStored(ForgeDirection from);
}
