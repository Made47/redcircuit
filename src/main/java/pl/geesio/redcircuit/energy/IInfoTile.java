package pl.geesio.redcircuit.energy;

import net.minecraft.entity.player.EntityPlayer;

/** Blok-encja bez okna: PPM pokazuje graczowi stan (produkcja, energia). */
public interface IInfoTile {
    void showInfo(EntityPlayer player);
}
