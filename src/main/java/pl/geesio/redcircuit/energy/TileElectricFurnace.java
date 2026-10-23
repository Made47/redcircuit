package pl.geesio.redcircuit.energy;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import pl.geesio.redcircuit.config.RCConfig;

/** Piec elektryczny: przetapia jak zwykły piec, ale 2x szybciej i na energię. 0 = wejście, 1 = wynik, 2 = bateria. */
public class TileElectricFurnace extends TileEnergyInv {
    public static final int CAPACITY = 20_000, COOK_TIME = 100;
    private static final GuiLayout LAYOUT = new GuiLayout().slot(0, 56, 17).output(1, 116, 35).battery(2, 56, 53)
            .energy(32, 17, 12, 52).arrow(80, 34);

    private int progress;

    public TileElectricFurnace() {
        super(3, CAPACITY, 400, 0, "tile.redcircuit.electric_furnace.name");
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public int field(int i) {
        return switch (i) {
            case F_PROGRESS -> progress;
            case F_MAX_PROGRESS -> COOK_TIME;
            default -> super.field(i);
        };
    }

    public static ItemStack result(ItemStack input) {
        return input == null ? null : FurnaceRecipes.smelting().getSmeltingResult(input);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        BatteryHelper.discharge(items[2], energy, 400);
        boolean working = false;
        ItemStack result = result(items[0]);
        if (result != null && fits(1, result)) {
            if (energy.use(RCConfig.electricFurnaceRf)) {
                working = true;
                if (++progress >= COOK_TIME) {
                    progress = 0;
                    decrStackSize(0, 1);
                    addTo(1, result);
                }
                markDirty();
            }
        } else if (progress != 0) {
            progress = 0;
            markDirty();
        }
        setLit(working);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return result(stack) != null;
        return super.isItemValidForSlot(slot, stack);
    }

    @Override
    protected void writeExtra(NBTTagCompound tag) {
        tag.setInteger("Progress", progress);
    }

    @Override
    protected void readExtra(NBTTagCompound tag) {
        progress = tag.getInteger("Progress");
    }
}
