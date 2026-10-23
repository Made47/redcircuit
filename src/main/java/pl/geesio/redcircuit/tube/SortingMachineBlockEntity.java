package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/**
 * Konfiguracja maszyny sortującej: 9 kolumn x 3 rzędy.
 * <ul>
 *   <li>rząd 1 (górny) - barwnik nadający kolor przedmiotom z tej kolumny (opcjonalny),</li>
 *   <li>rzędy 2-3 - wzorce przedmiotów dla tej kolumny.</li>
 * </ul>
 * Kolumna z barwnikiem, ale bez wzorców, łapie wszystkie pozostałe przedmioty.
 */
public class SortingMachineBlockEntity extends BlockEntity {
    public static final int COLUMNS = 9;
    public final MachineContainer config = new MachineContainer(this, 27);

    public SortingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTING_MACHINE.get(), pos, state);
    }

    /** Zwraca numer kolumny pasującej do przedmiotu lub -1. */
    public int columnFor(ItemStack stack) {
        for (int c = 0; c < COLUMNS; c++) {
            for (int row = 1; row <= 2; row++) {
                ItemStack pattern = config.getItem(row * COLUMNS + c);
                if (!pattern.isEmpty() && ItemStack.isSameItem(pattern, stack)) return c;
            }
        }
        for (int c = 0; c < COLUMNS; c++) {
            boolean hasPatterns = !config.getItem(COLUMNS + c).isEmpty() || !config.getItem(2 * COLUMNS + c).isEmpty();
            if (!hasPatterns && !config.getItem(c).isEmpty()) return c;
        }
        return -1;
    }

    public int colorOf(int column) {
        if (column < 0) return -1;
        ItemStack dye = config.getItem(column);
        return dye.getItem() instanceof DyeItem d ? d.getDyeColor().getId() : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        config.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        config.load(tag, registries);
    }
}
