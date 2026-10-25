package pl.geesio.redcircuit.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import pl.geesio.redcircuit.RedCircuit;

/** Identyfikatory kategorii REI wspólne dla serwera i klienta. */
public final class ReiIds {
    private ReiIds() {}

    public static final CategoryIdentifier<AlloyDisplay> ALLOYING = CategoryIdentifier.of(RedCircuit.MODID, "alloying");
    public static final CategoryIdentifier<ProcessingDisplay> MACERATING = CategoryIdentifier.of(RedCircuit.MODID, "macerating");
    public static final CategoryIdentifier<ProcessingDisplay> SAWING = CategoryIdentifier.of(RedCircuit.MODID, "sawing");
}
