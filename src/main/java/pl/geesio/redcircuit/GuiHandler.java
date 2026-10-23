package pl.geesio.redcircuit;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import pl.geesio.redcircuit.alloy.ContainerAlloyFurnace;
import pl.geesio.redcircuit.alloy.TileAlloyFurnace;
import pl.geesio.redcircuit.util.IGuiInventory;

/** Okna (GUI) bloków - numer okna przekazywany w player.openGui. */
public class GuiHandler implements IGuiHandler {
    public static final int ALLOY_FURNACE = 0;
    /** Okno jak skrzynia dla bloków z IGuiInventory (maszyny, filtry). */
    public static final int CHEST = 1;
    /** Maszyny energii (TileEnergyInv). */
    public static final int MACHINE = 2;
    public static final int BLUEPRINT_TABLE = 3;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (id == ALLOY_FURNACE && te instanceof TileAlloyFurnace furnace) {
            return new ContainerAlloyFurnace(player.inventory, furnace);
        }
        if (id == CHEST && te instanceof IGuiInventory g) {
            return new net.minecraft.inventory.ContainerChest(player.inventory, g.guiInventory());
        }
        if (id == MACHINE && te instanceof pl.geesio.redcircuit.energy.TileEnergyInv m) {
            return new pl.geesio.redcircuit.energy.ContainerEnergyMachine(player.inventory, m);
        }
        if (id == BLUEPRINT_TABLE && te instanceof pl.geesio.redcircuit.craft.TileBlueprintTable b) {
            return new pl.geesio.redcircuit.craft.ContainerBlueprintTable(player.inventory, b);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (id == ALLOY_FURNACE && te instanceof TileAlloyFurnace furnace) {
            return new pl.geesio.redcircuit.client.GuiAlloyFurnace(player.inventory, furnace);
        }
        if (id == CHEST && te instanceof IGuiInventory g) {
            return new net.minecraft.client.gui.inventory.GuiChest(player.inventory, g.guiInventory());
        }
        if (id == MACHINE && te instanceof pl.geesio.redcircuit.energy.TileEnergyInv m) {
            return new pl.geesio.redcircuit.client.GuiEnergyMachine(player.inventory, m);
        }
        if (id == BLUEPRINT_TABLE && te instanceof pl.geesio.redcircuit.craft.TileBlueprintTable b) {
            return new pl.geesio.redcircuit.client.GuiBlueprintTable(player.inventory, b);
        }
        return null;
    }
}
