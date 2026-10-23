package pl.geesio.redcircuit.energy;

import java.util.ArrayList;
import java.util.List;

/** Opis okna maszyny: sloty, pasek energii, strzałka postępu, płomień. Wspólny dla wszystkich maszyn energii. */
public class GuiLayout {
    public enum Kind { NORMAL, OUTPUT, BATTERY }

    public static final class SlotDef {
        public final int index, x, y;
        public final Kind kind;

        SlotDef(int index, int x, int y, Kind kind) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.kind = kind;
        }
    }

    public final List<SlotDef> slots = new ArrayList<>();
    /** Pasek energii: x, y, szerokość, wysokość (null = brak). */
    public int[] energyBar;
    /** Strzałka postępu (lewy górny róg) albo null. */
    public int[] arrow;
    /** Płomień spalania (lewy górny róg, 14x14) albo null. */
    public int[] flame;

    public GuiLayout slot(int index, int x, int y) {
        slots.add(new SlotDef(index, x, y, Kind.NORMAL));
        return this;
    }

    public GuiLayout output(int index, int x, int y) {
        slots.add(new SlotDef(index, x, y, Kind.OUTPUT));
        return this;
    }

    public GuiLayout battery(int index, int x, int y) {
        slots.add(new SlotDef(index, x, y, Kind.BATTERY));
        return this;
    }

    public GuiLayout energy(int x, int y, int w, int h) {
        energyBar = new int[]{x, y, w, h};
        return this;
    }

    public GuiLayout arrow(int x, int y) {
        arrow = new int[]{x, y};
        return this;
    }

    public GuiLayout flame(int x, int y) {
        flame = new int[]{x, y};
        return this;
    }

    public SlotDef def(int index) {
        for (SlotDef d : slots) if (d.index == index) return d;
        return null;
    }
}
