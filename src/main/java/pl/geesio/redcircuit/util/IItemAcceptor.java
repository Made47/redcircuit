package pl.geesio.redcircuit.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Blok-encja przyjmująca przedmioty "w locie" (rura, detektor przedmiotów...) - zamiast zwykłych slotów.
 * Używane przez {@link InvUtil#insert} przed IInventory.
 */
public interface IItemAcceptor {
    /**
     * @param side     ściana tego bloku, przez którą wchodzi przedmiot
     * @param simulate true = tylko sprawdź
     * @return reszta, która się nie zmieściła (null = wszystko przyjęte)
     */
    ItemStack accept(ItemStack stack, ForgeDirection side, boolean simulate);
}
