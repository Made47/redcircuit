package pl.geesio.redcircuit.energy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import pl.geesio.redcircuit.config.RCConfig;

/**
 * Generator na paliwo stałe: czas spalania jak w piecu (węgiel 1600 ticków), produkcja z konfiguracji
 * (domyślnie 20 RF/t). Bufor 20 000 RF, oddaje do 400 RF/t na stronę. Sygnał redstone wstrzymuje dokładanie paliwa.
 */
public class TileFuelGenerator extends TileEnergyInv {
    public static final int CAPACITY = 20_000, OUTPUT = 400;
    private static final GuiLayout LAYOUT = new GuiLayout().slot(0, 80, 53).flame(81, 33).energy(120, 17, 16, 52);

    private int burnTime, burnTotal;

    public TileFuelGenerator() {
        super(1, CAPACITY, 0, OUTPUT, "tile.redcircuit.solid_fuel_generator.name");
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public int field(int i) {
        return switch (i) {
            case F_BURN -> burnTime;
            case F_BURN_TOTAL -> burnTotal;
            default -> super.field(i);
        };
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        int rate = RCConfig.fuelGeneratorRf;
        boolean powered = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);

        // Nowe paliwo tylko, gdy jest miejsce na energię i brak sygnału redstone
        if (burnTime <= 0 && !powered && energy.get() + rate <= CAPACITY) {
            ItemStack fuel = items[0];
            int time = TileEntityFurnace.getItemBurnTime(fuel);
            if (time > 0) {
                burnTime = burnTotal = time;
                ItemStack remainder = fuel.getItem().getContainerItem(fuel);   // np. puste wiadro po lawie
                if (--fuel.stackSize <= 0) items[0] = remainder;
                markDirty();
            }
        }

        int made = 0;
        if (burnTime > 0 && energy.get() < CAPACITY) {
            burnTime--;
            made = energy.addInternal(rate);
        }
        setLit(burnTime > 0 && made > 0);
        EnergyUtil.push(worldObj, xCoord, yCoord, zCoord, energy, OUTPUT);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return TileEntityFurnace.isItemFuel(stack);
    }

    /** Od dołu można wyjąć tylko resztki (puste wiadra). */
    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return side == 0 && !TileEntityFurnace.isItemFuel(stack);
    }

    @Override
    protected void writeExtra(NBTTagCompound tag) {
        tag.setInteger("BurnTime", burnTime);
        tag.setInteger("BurnTotal", burnTotal);
    }

    @Override
    protected void readExtra(NBTTagCompound tag) {
        burnTime = tag.getInteger("BurnTime");
        burnTotal = tag.getInteger("BurnTotal");
    }
}
