package cofh.api.energy;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * RF API (CoFH, licencja MIT/LGPL - publiczne API do dołączania w modach).
 * Blok-encja, do której mogą podłączać się przewody energii.
 */
public interface IEnergyConnection {
    boolean canConnectEnergy(ForgeDirection from);
}
