package pl.geesio.redcircuit.energy;

import net.minecraft.item.ItemStack;

/**
 * Akumulator: 1 000 000 RF + dwa sloty na baterie - górny ładuje baterię z akumulatora, dolny rozładowuje
 * baterię do akumulatora. Węzeł sieci (jak przewody): wyrównuje poziom z sąsiednimi węzłami.
 */
public class TileBatteryBox extends TileEnergyInv implements IEnergyNode {
    public static final int CAPACITY = 1_000_000, CHARGE = 0, DISCHARGE = 1, ITEM_RATE = 1_000, RATE = 1_000;
    private static final GuiLayout LAYOUT = new GuiLayout().battery(CHARGE, 44, 17).battery(DISCHARGE, 44, 53).energy(80, 17, 16, 52);

    public TileBatteryBox() {
        super(2, CAPACITY, RATE, RATE, "tile.redcircuit.battery_box.name");
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public EnergyBuffer buffer() {
        return energy;
    }

    @Override
    public int rate() {
        return RATE;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        BatteryHelper.charge(items[CHARGE], energy, ITEM_RATE);
        BatteryHelper.discharge(items[DISCHARGE], energy, ITEM_RATE);
        EnergyNodes.distribute(this, this);
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    /** Góra: do ładowania; boki: do rozładowania; dół: wyjmowanie gotowych baterii. */
    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        if (side == 0 || !BatteryHelper.isBattery(stack)) return false;
        return slot == (side == 1 ? CHARGE : DISCHARGE);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        if (side != 0) return false;
        return slot == CHARGE ? BatteryHelper.isFull(stack) : BatteryHelper.isEmpty(stack);
    }
}
