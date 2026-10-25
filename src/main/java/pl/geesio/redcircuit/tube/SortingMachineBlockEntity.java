package pl.geesio.redcircuit.tube;

import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
        net.minecraft.world.item.DyeColor c = dye.get(net.minecraft.core.component.DataComponents.DYE);
        return c != null ? c.getId() : -1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        config.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        config.load(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
            Containers.dropContents(level, pos, this.config);
    }
}
