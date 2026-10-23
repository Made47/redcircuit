package pl.geesio.redcircuit.platform.energy;

/** Odpowiednik IEnergyStorage z NeoForge (Forge Energy). Na Fabricu mostkowany do Team Reborn Energy. */
public interface IEnergyStorage {
    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
