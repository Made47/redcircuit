package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.gate.BlockGate;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.gate.TileGate;
import pl.geesio.redcircuit.network.MessageGateConfig;
import pl.geesio.redcircuit.network.RCNetwork;
import pl.geesio.redcircuit.util.Dirs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Okno bramki (PPM na bramce; port GateScreen z 1.21): opis działania, podgląd wejść/wyjść na żywo
 * i ustawienia - czas (zegar, sekwencer, komórka stanu), maksimum licznika, wyłączanie stron,
 * strony zatrzasku RS i komórki stanu, dźwięk tykania.
 */
public class GuiGate extends GuiScreen {
    private static final int W = 240;
    private static final int ROW = 24;

    private final int gx, gy, gz;
    private final GateType type;
    private int interval = 20;
    private int max = 4;
    private GuiTextField field;
    private boolean resetCount, muted, mirrored, quiet;
    private int disabled;

    private int h;
    private int valueRowY = -1, countRowY = -1, inputsRowY = -1;
    private List<String> desc = new ArrayList<>();

    /** Przycisk z akcją (zamiast numerów w actionPerformed). */
    private static class Btn extends GuiButton {
        final Consumer<Btn> action;

        Btn(int id, int x, int y, int w, String text, Consumer<Btn> action) {
            super(id, x, y, w, 20, text);
            this.action = action;
        }
    }

    public GuiGate(int x, int y, int z, GateType type) {
        this.gx = x;
        this.gy = y;
        this.gz = z;
        this.type = type;
        TileGate t = tile();
        if (t != null) {
            interval = t.interval;
            max = t.max;
            muted = t.muted;
            mirrored = t.mirrored;
            quiet = t.quiet;
            disabled = t.disabledMask();
        }
    }

    private World world() {
        return net.minecraft.client.Minecraft.getMinecraft().theWorld;
    }

    private TileGate tile() {
        World w = world();
        return w == null ? null : BlockGate.tile(w, gx, gy, gz);
    }

    private static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key) : StatCollector.translateToLocalFormatted(key, args);
    }

    private boolean counter() {
        return type == GateType.COUNTER;
    }

    private boolean hasValue() {
        return counter() || type.hasInterval();
    }

    private int value() {
        return counter() ? max : interval;
    }

    private void setValue(int v) {
        setValueSilently(v);
        if (field != null) field.setText(format());
    }

    /** Tekst pola: liczba (licznik) albo sekundy z dwoma miejscami po przecinku. */
    private String format() {
        return counter() ? Integer.toString(max) : String.format(Locale.ROOT, "%.2f", interval / 20.0);
    }

    private void readField() {
        if (field == null) return;
        String t = field.getText().trim().replace(',', '.');
        try {
            if (counter()) setValueSilently(Integer.parseInt(t));
            else setValueSilently((int) Math.round(Double.parseDouble(t) * 20.0));
        } catch (NumberFormatException ignored) {
        }
    }

    private void setValueSilently(int v) {
        if (counter()) max = Math.max(1, Math.min(TileGate.MAX_COUNT, v));
        else interval = Math.max(TileGate.MIN_INTERVAL, Math.min(TileGate.MAX_INTERVAL, v));
    }

    // ------------------------------------------------------------------ układ

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        buttonList.clear();
        desc = fontRendererObj.listFormattedStringToWidth(tr("screen.redcircuit.gate.desc." + type.getSerializedName()), W - 70);
        int descH = Math.max(48, desc.size() * 10 + 4);

        int rows = 0;
        if (hasValue()) rows += 2;
        if (counter()) rows++;
        if (type.switchableSides() != 0) rows++;
        if (type == GateType.RS_LATCH) rows += 2;
        if (type == GateType.STATE_CELL) rows++;
        h = 26 + descH + 6 + rows * ROW + 30;

        int x = (width - W) / 2, y = (height - h) / 2;
        int yy = y + 26 + descH + 6;
        field = null;
        valueRowY = countRowY = inputsRowY = -1;
        int id = 0;

        if (hasValue()) {
            valueRowY = yy;
            field = new GuiTextField(fontRendererObj, x + 80, yy + 1, 80, 18);
            field.setMaxStringLength(10);
            field.setText(format());
            yy += ROW;
            int[] steps = counter() ? new int[]{-10, -1, 1, 10} : new int[]{-20, -2, 2, 20};
            String[] labels = counter() ? new String[]{"-10", "-1", "+1", "+10"} : new String[]{"-1s", "-0.1s", "+0.1s", "+1s"};
            for (int i = 0; i < 4; i++) {
                final int step = steps[i];
                buttonList.add(new Btn(id++, x + 10 + i * 56, yy, 52, labels[i], b -> {
                    readField();
                    setValue(value() + step);
                }));
            }
            yy += ROW;
        }

        if (counter()) {
            countRowY = yy;
            buttonList.add(new Btn(id++, x + 10, yy, 110, tr("screen.redcircuit.gate.reset"), b -> {
                resetCount = true;
                b.enabled = false;
            }));
            yy += ROW;
        }

        if (type.switchableSides() != 0) {
            inputsRowY = yy;
            int bx = x + 70;
            int[] bits = {GateType.IN_L, GateType.IN_B, GateType.IN_R};
            String[] names = {"left", "back", "right"};
            for (int i = 0; i < 3; i++) {
                final int bit = bits[i];
                final String name = names[i];
                if ((type.switchableSides() & bit) == 0) continue;
                buttonList.add(new Btn(id++, bx, yy, 54, inputLabel(name, bit), b -> {
                    int next = disabled ^ bit;
                    if (type.canDisableInputs() && (next & type.inputs()) == type.inputs()) return;   // co najmniej jedno wejście zostaje
                    disabled = next;
                    b.displayString = inputLabel(name, bit);
                }));
                bx += 56;
            }
            yy += ROW;
        }

        if (type == GateType.RS_LATCH) {
            buttonList.add(new Btn(id++, x + 10, yy, W - 20, rsSideLabel(), b -> {
                mirrored = !mirrored;
                b.displayString = rsSideLabel();
            }));
            yy += ROW;
            buttonList.add(new Btn(id++, x + 10, yy, W - 20, rsQuietLabel(), b -> {
                quiet = !quiet;
                b.displayString = rsQuietLabel();
            }));
            yy += ROW;
        }

        if (type == GateType.STATE_CELL) {
            buttonList.add(new Btn(id++, x + 10, yy, W - 20, sideLabel(), b -> {
                mirrored = !mirrored;
                b.displayString = sideLabel();
            }));
        }

        int bottom = y + h - 26;
        if (type.ticks()) {
            buttonList.add(new Btn(id++, x + 10, bottom, 110, soundLabel(), b -> {
                muted = !muted;
                b.displayString = soundLabel();
            }));
        }
        buttonList.add(new Btn(id, x + W - 110, bottom, 100, tr("gui.done"), b -> {
            readField();
            RCNetwork.CHANNEL.sendToServer(new MessageGateConfig(gx, gy, gz, interval, max, resetCount, muted, mirrored,
                    disabled, quiet));
            mc.displayGuiScreen(null);
        }));
    }

    private String inputLabel(String side, int bit) {
        boolean off = (disabled & bit) != 0;
        return tr(off ? "screen.redcircuit.gate.input_off" : "screen.redcircuit.gate.input_on",
                tr("screen.redcircuit.gate.side." + side));
    }

    private String rsSideLabel() {
        return tr(mirrored ? "screen.redcircuit.gate.rs_right" : "screen.redcircuit.gate.rs_left");
    }

    private String rsQuietLabel() {
        return tr(quiet ? "screen.redcircuit.gate.rs_quiet_on" : "screen.redcircuit.gate.rs_quiet_off");
    }

    private String sideLabel() {
        return tr(mirrored ? "screen.redcircuit.gate.side_left" : "screen.redcircuit.gate.side_right");
    }

    private String soundLabel() {
        return tr(muted ? "screen.redcircuit.gate.sound_off" : "screen.redcircuit.gate.sound_on");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof Btn b) b.action.accept(b);
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (field != null && field.isFocused() && key != 1) {   // 1 = Esc
            field.textboxKeyTyped(c, key);
            readField();
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        if (field != null) field.mouseClicked(mx, my, button);
    }

    @Override
    public void updateScreen() {
        if (field != null) field.updateCursorCounter();
        // Bramka zniknęła - zamknij okno
        World w = world();
        if (w == null || !(w.getBlock(gx, gy, gz) instanceof BlockGate)) mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        int x = (width - W) / 2, y = (height - h) / 2;
        drawRect(x - 1, y - 1, x + W + 1, y + h + 1, 0xFF000000);
        drawRect(x, y, x + W, y + h, 0xFF3A3A40);
        drawRect(x + 4, y + 22, x + W - 4, y + 23, 0xFF5A5A62);

        drawCenteredString(fontRendererObj, tr("tile.redcircuit." + type.getSerializedName() + "_gate.name"),
                width / 2, y + 9, 0xFFFFFF);
        drawDiagram(x + 10, y + 30);
        int ty = y + 30;
        for (String line : desc) {
            fontRendererObj.drawStringWithShadow(line, x + 62, ty, 0xD0D0D0);
            ty += 10;
        }

        if (valueRowY >= 0) {
            String label = tr(counter() ? "screen.redcircuit.gate.max" : "screen.redcircuit.gate.time");
            drawString(fontRendererObj, label, x + 10, valueRowY + 6, 0xE0E0E0);
            if (!counter()) drawString(fontRendererObj, tr("screen.redcircuit.gate.ticks", interval), x + 166, valueRowY + 6, 0xA0A0A0);
        }
        if (countRowY >= 0) {
            TileGate t = tile();
            int count = t == null ? 0 : t.count;
            drawString(fontRendererObj, tr("screen.redcircuit.gate.count", resetCount ? 0 : count, max),
                    x + 128, countRowY + 6, 0xA0A0A0);
        }
        if (inputsRowY >= 0) {
            drawString(fontRendererObj, tr(type.switchesOutputs() ? "screen.redcircuit.gate.outputs" : "screen.redcircuit.gate.inputs"),
                    x + 10, inputsRowY + 6, 0xE0E0E0);
        }
        if (field != null) field.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    /**
     * Schemat bramki z góry (przód = góra): wypustki wejść i wyjść w kolorze aktualnego sygnału.
     * Jasnoczerwony = wyjście świeci, czerwony = wejście zasilone, ciemny = brak sygnału, szary = wejście wyłączone.
     */
    private void drawDiagram(int bx, int by) {
        World w = world();
        TileGate t = tile();
        if (w == null || t == null || !(w.getBlock(gx, gy, gz) instanceof BlockGate gate)) return;
        ForgeDirection f = t.facing();

        drawRect(bx - 1, by - 1, bx + 41, by + 41, 0xFF202022);
        drawRect(bx, by, bx + 40, by + 40, 0xFF78787A);
        drawRect(bx + 12, by + 14, bx + 28, by + 26, 0xFF2A2A2C);

        // kolejność: przód, lewo, tył, prawo
        ForgeDirection[] sides = {f, Dirs.ccw(f), f.getOpposite(), Dirs.cw(f)};
        int[] bits = {0, GateType.IN_L, GateType.IN_B, GateType.IN_R};
        for (int i = 0; i < 4; i++) {
            ForgeDirection d = sides[i];
            int out = gate.signal(w, gx, gy, gz, d.getOpposite());
            boolean isInput = bits[i] != 0 && (type.inputs() & bits[i]) != 0;
            boolean isOutput = i == 0 || out > 0 || outputSide(i);
            if (!isInput && !isOutput) continue;
            int color;
            if (out > 0) color = 0xFFFF3A2A;
            else if (isInput && type.canDisableInputs() && (disabled & bits[i]) != 0) color = 0xFF4A4A4A;
            else if (isInput && w.getIndirectPowerLevelTo(gx + d.offsetX, gy, gz + d.offsetZ, d.ordinal()) > 0) color = 0xFFC82020;
            else color = 0xFF5A1410;
            switch (i) {
                case 0 -> drawRect(bx + 17, by + 2, bx + 23, by + 13, color);
                case 1 -> drawRect(bx + 2, by + 17, bx + 11, by + 23, color);
                case 2 -> drawRect(bx + 17, by + 27, bx + 23, by + 38, color);
                default -> drawRect(bx + 29, by + 17, bx + 38, by + 23, color);
            }
        }
        drawRect(bx + 15, by + 1, bx + 25, by + 2, 0xFF202022);   // strzałka wyjścia
    }

    /** Strony, które w tej bramce bywają wyjściami (poza przodem) - rysowane także, gdy nie świecą. */
    private boolean outputSide(int i) {
        return switch (type) {
            case TIMER, RANDOMIZER -> i == 1 || i == 3;
            case SEQUENCER -> true;
            case RS_LATCH -> i == 2 || !quiet;
            case TOGGLE, COUNTER -> i == 2;
            case BUFFER, NOT -> (i == 1 && (disabled & GateType.IN_L) == 0) || (i == 3 && (disabled & GateType.IN_R) == 0);
            case STATE_CELL -> i == (mirrored ? 1 : 3);
            default -> false;
        };
    }
}
