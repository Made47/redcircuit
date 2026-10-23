package pl.geesio.redcircuit.energy;

import net.minecraft.item.ItemStack;

/** Ładowarka: 4 sloty na przedmioty z energią (baterie, narzędzia elektryczne, przedmioty innych modów). */
public class TileCharger extends TileEnergyInv {
    public static final int SLOTS = 4, CAPACITY = 50_000, RATE = 1_000;
    private static final GuiLayout LAYOUT;

    static {
        LAYOUT = new GuiLayout().energy(120, 17, 16, 52);
        for (int i = 0; i < SLOTS; i++) LAYOUT.battery(i, 35 + i * 18, 35);
    }

    public TileCharger() {
        super(SLOTS, CAPACITY, RATE * SLOTS, 0, "tile.redcircuit.charger.name");
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        boolean charging = false;
        for (int i = 0; i < SLOTS; i++) {
            if (BatteryHelper.charge(items[i], energy, RATE) > 0) charging = true;
        }
        setLit(charging);
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    /** Lejki: wkładanie z góry i boków, od dołu wyjmowanie w pełni naładowanych. */
    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return side != 0 && BatteryHelper.isBattery(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return side == 0 && BatteryHelper.isFull(stack);
    }
}
