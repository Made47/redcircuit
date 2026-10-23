package pl.geesio.redcircuit.util;

import net.minecraft.inventory.IInventory;

/** Blok-encja z oknem jak skrzynia (wielokrotność 9 slotów) - GuiHandler.CHEST. */
public interface IGuiInventory {
    IInventory guiInventory();
}
