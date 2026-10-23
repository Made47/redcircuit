package cofh.api.energy;

import net.minecraftforge.common.util.ForgeDirection;

/** RF API: blok-encja oddająca energię. */
public interface IEnergyProvider extends IEnergyConnection {
    int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate);

    int getEnergyStored(ForgeDirection from);

    int getMaxEnergyStored(ForgeDirection from);
}
