package pl.geesio.redcircuit.energy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import pl.geesio.redcircuit.config.RCConfig;

/**
 * Macerator i tartak: receptury z {@link ProcessingRecipes}, energia z sieci albo z baterii.
 * Sloty: 0 = wejście, 1 = wynik, 2 = produkt uboczny, 3 = bateria.
 */
public abstract class TileProcessor extends TileEnergyInv {
    public static final int CAPACITY = 20_000, INPUT = 0, OUTPUT = 1, BONUS = 2, BATTERY = 3;
    private static final GuiLayout LAYOUT = new GuiLayout().slot(INPUT, 56, 17).output(OUTPUT, 112, 35).output(BONUS, 142, 35)
            .battery(BATTERY, 56, 53).energy(32, 17, 12, 52).arrow(80, 34);

    private int progress, maxProgress = 200;

    protected TileProcessor(String title) {
        super(4, CAPACITY, 400, 0, title);
    }

    protected abstract ProcessingRecipes.Recipe recipe(ItemStack input);

    protected abstract int cost();

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public int field(int i) {
        return switch (i) {
            case F_PROGRESS -> progress;
            case F_MAX_PROGRESS -> maxProgress;
            default -> super.field(i);
        };
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        BatteryHelper.discharge(items[BATTERY], energy, 400);
        boolean working = false;
        ProcessingRecipes.Recipe r = recipe(items[INPUT]);
        if (r != null) {
            maxProgress = Math.max(1, r.time);
            boolean fits = fits(OUTPUT, r.result) && (r.bonus == null || fits(BONUS, r.bonus));
            if (fits && energy.use(cost())) {
                working = true;
                if (++progress >= maxProgress) {
                    progress = 0;
                    decrStackSize(INPUT, 1);
                    addTo(OUTPUT, r.result);
                    if (r.bonus != null && worldObj.rand.nextFloat() < r.bonusChance) addTo(BONUS, r.bonus);
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
        if (slot == INPUT) return recipe(stack) != null;
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

    /** Macerator. */
    public static class Macerator extends TileProcessor {
        public Macerator() {
            super("tile.redcircuit.macerator.name");
        }

        @Override
        protected ProcessingRecipes.Recipe recipe(ItemStack input) {
            return ProcessingRecipes.find(ProcessingRecipes.MACERATING, input);
        }

        @Override
        protected int cost() {
            return RCConfig.maceratorRf;
        }
    }

    /** Tartak. */
    public static class Sawmill extends TileProcessor {
        public Sawmill() {
            super("tile.redcircuit.sawmill.name");
        }

        @Override
        protected ProcessingRecipes.Recipe recipe(ItemStack input) {
            return ProcessingRecipes.findSawing(worldObj, input);
        }

        @Override
        protected int cost() {
            return RCConfig.sawmillRf;
        }
    }
}
