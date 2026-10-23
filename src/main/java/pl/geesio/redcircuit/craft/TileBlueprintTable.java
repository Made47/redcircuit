package pl.geesio.redcircuit.craft;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import pl.geesio.redcircuit.util.ConfigInventory;

/**
 * Stół kreślarski: trwała siatka 3x3 (przedmioty zostają po zamknięciu), 18 slotów magazynu,
 * z którego najpierw pobierane są składniki, oraz slot na plan.
 */
public class TileBlueprintTable extends TileEntity {
    public final ConfigInventory grid = new ConfigInventory(this, 9, "tile.redcircuit.blueprint_table.name");
    public final ConfigInventory storage = new ConfigInventory(this, 18, "tile.redcircuit.blueprint_table.name");
    public final ConfigInventory plan = new ConfigInventory(this, 1, "tile.redcircuit.blueprint_table.name") {
        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public boolean isItemValidForSlot(int slot, net.minecraft.item.ItemStack stack) {
            return stack != null && stack.getItem() instanceof ItemPlan;
        }
    };

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        grid.save(tag, "Grid");
        storage.save(tag, "Storage");
        plan.save(tag, "Plan");
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        grid.load(tag, "Grid");
        storage.load(tag, "Storage");
        plan.load(tag, "Plan");
    }
}
