package pl.geesio.redcircuit.gate;

import net.minecraft.util.StringRepresentable;

/**
 * Typy bramek. Wejścia względem kierunku wyjścia (przód):
 * L = lewo, B = tył, R = prawo.
 */
public enum GateType implements StringRepresentable {
    NOT("not"),
    BUFFER("buffer"),
    AND("and"),
    NAND("nand"),
    OR("or"),
    NOR("nor"),
    XOR("xor"),
    XNOR("xnor"),
    RS_LATCH("rs_latch"),
    TOGGLE("toggle"),
    PULSE("pulse"),
    TIMER("timer"),
    RANDOMIZER("randomizer"),
    COUNTER("counter"),
    SEQUENCER("sequencer"),
    STATE_CELL("state_cell"),
    SYNCHRONIZER("synchronizer");

    private final String name;

    GateType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Bramki przechowujące dodatkowe dane w GateBlockEntity. */
    public boolean needsData() {
        return this == COUNTER || this == STATE_CELL || this == SYNCHRONIZER || hasInterval();
    }

    /** Bramki z ustawianym czasem (PPM zmienia okres). */
    public boolean hasInterval() {
        return this == TIMER || this == SEQUENCER || this == STATE_CELL;
    }

    /** Bity wejść: lewo, tył, prawo. */
    public static final int IN_L = 1, IN_B = 2, IN_R = 4;

    /** Strony, z których bramka czyta sygnał (bity IN_L/IN_B/IN_R). */
    public int inputs() {
        return switch (this) {
            case XOR, XNOR, RS_LATCH, TOGGLE -> IN_L | IN_R;
            case COUNTER -> IN_L | IN_R;
            case AND, NAND, OR, NOR, SYNCHRONIZER -> IN_L | IN_B | IN_R;
            default -> IN_B;
        };
    }

    /** Wejścia, które można wyłączyć w oknie (bramki z co najmniej dwoma równorzędnymi wejściami). */
    public boolean canDisableInputs() {
        return switch (this) {
            case AND, NAND, OR, NOR, XOR, XNOR, TOGGLE -> true;
            default -> false;
        };
    }

    /** Wejścia wyłączone domyślnie: AND i NAND mają trzy wejścia, ale tylne startuje wyłączone (jak dawne 2-wejściowe). */
    public int defaultDisabled() {
        return this == AND || this == NAND ? IN_B : 0;
    }

    /** Bit przodu w masce stron (obok IN_L/IN_B/IN_R). */
    public static final int OUT_F = 8;

    /** Strony będące wyjściami (poza wejściami): przód zawsze, plus dodatkowe wyjścia niektórych bramek. */
    public int extraOutputs(boolean mirrored) {
        return switch (this) {
            case TIMER, BUFFER, NOT, RANDOMIZER -> OUT_F | IN_L | IN_R;
            case SEQUENCER -> OUT_F | IN_L | IN_B | IN_R;
            case STATE_CELL -> OUT_F | (mirrored ? IN_L : IN_R);
            case RS_LATCH, TOGGLE, COUNTER -> OUT_F | IN_B;
            default -> OUT_F;
        };
    }

    /** Strony, które można wyłączyć w oknie: wejścia bramek logicznych albo boczne wyjścia bufora. */
    public int switchableSides() {
        if (canDisableInputs()) return inputs();
        return switchesOutputs() ? IN_L | IN_R : 0;
    }

    /** Czy wyłączane strony to wyjścia (bufor), a nie wejścia. */
    public boolean switchesOutputs() {
        return this == BUFFER || this == NOT;
    }

    /** Wartość, jaką ma wyłączone wejście - taka, by nie wpływało na wynik (dla AND "prawda", dla reszty "fałsz"). */
    public boolean neutralInput() {
        return this == AND || this == NAND;
    }

    /** Bramki, które tykają (mają przełącznik dźwięku). */
    public boolean ticks() {
        return this == TIMER || this == SEQUENCER || this == STATE_CELL || this == COUNTER;
    }

    /** Logika kombinacyjna (bez stanu). */
    public boolean eval(boolean l, boolean b, boolean r) {
        return switch (this) {
            case NOT -> !b;
            case BUFFER -> b;
            case AND -> l && b && r;
            case NAND -> !(l && b && r);
            case OR -> l || b || r;
            case NOR -> !(l || b || r);
            case XOR -> l ^ r;
            case XNOR -> !(l ^ r);
            default -> false;
        };
    }
}
