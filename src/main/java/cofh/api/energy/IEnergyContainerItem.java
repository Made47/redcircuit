package cofh.api.energy;

import net.minecraft.item.ItemStack;

/** RF API: przedmiot z energią (bateria, narzędzie elektryczne). */
public interface IEnergyContainerItem {
    int receiveEnergy(ItemStack container, int maxReceive, boolean simulate);

    int extractEnergy(ItemStack container, int maxExtract, boolean simulate);

    int getEnergyStored(ItemStack container);

    int getMaxEnergyStored(ItemStack container);
}
