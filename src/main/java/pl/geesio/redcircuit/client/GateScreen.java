package pl.geesio.redcircuit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.geesio.redcircuit.gate.GateBlock;
import pl.geesio.redcircuit.gate.GateBlockEntity;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.network.GateConfigPayload;

import java.util.List;
import java.util.Locale;

/**
 * Okno bramki (PPM na dowolnej bramce): opis działania, podgląd wejść/wyjść na żywo i ustawienia -
 * czas (zegar, sekwencer, komórka stanu), maksimum licznika, wyłączanie wejść,
 * strony zatrzasku RS i komórki stanu, dźwięk tykania.
 */
public class GateScreen extends Screen {
    private static final int W = 240;
    private static final int ROW = 22;

    private final BlockPos pos;
    private final GateType type;
    private int interval = 20;
    private int max = 4;
    private EditBox field;
    private boolean resetCount;
    private boolean muted;
    private boolean mirrored;
    private int disabled;
    private boolean quiet;

    private int h;
    private int valueRowY = -1;
    private int countRowY = -1;
    private int inputsRowY = -1;
    private List<FormattedCharSequence> desc = List.of();

    public GateScreen(BlockPos pos, GateType type) {
        super(Component.translatable("block.redcircuit." + type.getSerializedName() + "_gate"));
        this.pos = pos;
        this.type = type;
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof GateBlockEntity be) {
            interval = be.interval;
            max = be.max;
            muted = be.muted;
            mirrored = be.mirrored;
            quiet = be.quiet;
        }
        if (level != null && level.getBlockState(pos).hasProperty(GateBlock.MIRRORED)) {
            mirrored = level.getBlockState(pos).getValue(GateBlock.MIRRORED);
            disabled = GateBlock.disabledMask(level.getBlockState(pos));
        }
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
        if (field != null) field.setValue(format());
    }

    /** Tekst pola: liczba (licznik) albo sekundy z dwoma miejscami po przecinku. */
    private String format() {
        return counter() ? Integer.toString(max) : String.format(Locale.ROOT, "%.2f", interval / 20.0);
    }

    private void readField() {
        if (field == null) return;
        String t = field.getValue().trim().replace(',', '.');
        try {
            if (counter()) setValueSilently(Integer.parseInt(t));
            else setValueSilently((int) Math.round(Double.parseDouble(t) * 20.0));
        } catch (NumberFormatException ignored) {
        }
    }

    private void setValueSilently(int v) {
        if (counter()) max = Math.clamp(v, 1, GateBlockEntity.MAX_COUNT);
        else interval = Math.clamp(v, GateBlockEntity.MIN_INTERVAL, GateBlockEntity.MAX_INTERVAL);
    }

    // ------------------------------------------------------------------ układ

    @Override
    protected void init() {
        desc = font.split(Component.translatable("screen.redcircuit.gate.desc." + type.getSerializedName()), W - 70);
        int descH = Math.max(48, desc.size() * 10 + 4);

        // Wysokość okna liczona z góry, żeby je wyśrodkować
        int rows = 0;
        if (hasValue()) rows += 2;
        if (counter()) rows++;
        if (type.switchableSides() != 0) rows++;
        if (type == GateType.RS_LATCH) rows += 2;
        if (type == GateType.STATE_CELL) rows++;
        h = 26 + descH + 6 + rows * ROW + 28;

        int x = (width - W) / 2, y = (height - h) / 2;
        int yy = y + 26 + descH + 6;
        field = null;
        valueRowY = countRowY = inputsRowY = -1;

        if (hasValue()) {
            valueRowY = yy;
            field = new EditBox(font, x + 80, yy, 80, 18, Component.empty());
            field.setValue(format());
            field.setResponder(s -> readField());
            addRenderableWidget(field);
            yy += ROW;
            // Przyciski szybkiej zmiany
            int[] steps = counter() ? new int[]{-10, -1, 1, 10} : new int[]{-20, -2, 2, 20};
            String[] labels = counter() ? new String[]{"-10", "-1", "+1", "+10"} : new String[]{"-1s", "-0.1s", "+0.1s", "+1s"};
            for (int i = 0; i < 4; i++) {
                final int step = steps[i];
                addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> {
                    readField();
                    setValue(value() + step);
                }).bounds(x + 10 + i * 56, yy, 52, 18).build());
            }
            yy += ROW;
        }

        if (counter()) {
            countRowY = yy;
            addRenderableWidget(Button.builder(Component.translatable("screen.redcircuit.gate.reset"), b -> {
                resetCount = true;
                b.active = false;
            }).bounds(x + 10, yy, 110, 18).build());
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
                addRenderableWidget(Button.builder(inputLabel(name, bit), b -> {
                    int next = disabled ^ bit;
                    if (type.canDisableInputs() && (next & type.inputs()) == type.inputs()) return;   // co najmniej jedno wejście zostaje
                    disabled = next;
                    b.setMessage(inputLabel(name, bit));
                }).bounds(bx, yy, 54, 18).build());
                bx += 56;
            }
            yy += ROW;
        }

        if (type == GateType.RS_LATCH) {
            addRenderableWidget(Button.builder(rsSideLabel(), b -> {
                mirrored = !mirrored;
                b.setMessage(rsSideLabel());
            }).bounds(x + 10, yy, W - 20, 18).build());
            yy += ROW;
            addRenderableWidget(Button.builder(rsQuietLabel(), b -> {
                quiet = !quiet;
                b.setMessage(rsQuietLabel());
            }).bounds(x + 10, yy, W - 20, 18).build());
            yy += ROW;
        }

        if (type == GateType.STATE_CELL) {
            // Strona wyjścia "w trakcie odliczania": prawa / lewa
            addRenderableWidget(Button.builder(sideLabel(), b -> {
                mirrored = !mirrored;
                b.setMessage(sideLabel());
            }).bounds(x + 10, yy, W - 20, 18).build());
        }

        int bottom = y + h - 26;
        if (type.ticks()) {
            // Dźwięk tykania: włączony / wyciszony
            addRenderableWidget(Button.builder(soundLabel(), b -> {
                muted = !muted;
                b.setMessage(soundLabel());
            }).bounds(x + 10, bottom, 110, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            readField();
            PacketDistributor.sendToServer(new GateConfigPayload(pos, interval, max, resetCount, muted, mirrored,
                    disabled, quiet));
            onClose();
        }).bounds(x + W - 110, bottom, 100, 18).build());
    }

    private Component inputLabel(String side, int bit) {
        boolean off = (disabled & bit) != 0;
        return Component.translatable(off ? "screen.redcircuit.gate.input_off" : "screen.redcircuit.gate.input_on",
                Component.translatable("screen.redcircuit.gate.side." + side));
    }

    private Component rsSideLabel() {
        return Component.translatable(mirrored ? "screen.redcircuit.gate.rs_right" : "screen.redcircuit.gate.rs_left");
    }

    private Component rsQuietLabel() {
        return Component.translatable(quiet ? "screen.redcircuit.gate.rs_quiet_on" : "screen.redcircuit.gate.rs_quiet_off");
    }

    private Component sideLabel() {
        return Component.translatable(mirrored ? "screen.redcircuit.gate.side_left" : "screen.redcircuit.gate.side_right");
    }

    private Component soundLabel() {
        return Component.translatable(muted ? "screen.redcircuit.gate.sound_off" : "screen.redcircuit.gate.sound_on");
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2, y = (height - h) / 2;
        g.drawCenteredString(font, title, width / 2, y + 9, 0xFFFFFF);

        drawDiagram(g, x + 10, y + 30);
        int ty = y + 30;
        for (FormattedCharSequence line : desc) {
            g.drawString(font, line, x + 62, ty, 0xD0D0D0);
            ty += 10;
        }

        if (valueRowY >= 0) {
            Component label = counter() ? Component.translatable("screen.redcircuit.gate.max")
                    : Component.translatable("screen.redcircuit.gate.time");
            g.drawString(font, label, x + 10, valueRowY + 5, 0xE0E0E0);
            if (!counter()) {
                g.drawString(font, Component.translatable("screen.redcircuit.gate.ticks", interval), x + 166, valueRowY + 5, 0xA0A0A0);
            }
        }
        if (countRowY >= 0) {
            int count = 0;
            var level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(pos) instanceof GateBlockEntity be) count = be.count;
            g.drawString(font, Component.translatable("screen.redcircuit.gate.count", resetCount ? 0 : count, max),
                    x + 128, countRowY + 5, 0xA0A0A0);
        }
        if (inputsRowY >= 0) {
            g.drawString(font, Component.translatable(type.switchesOutputs() ? "screen.redcircuit.gate.outputs" : "screen.redcircuit.gate.inputs"), x + 10, inputsRowY + 5, 0xE0E0E0);
        }
    }

    /**
     * Schemat bramki widziany z góry (przód = góra): wypustki wejść i wyjść w kolorze aktualnego sygnału.
     * Jasnoczerwony = wyjście świeci, czerwony = wejście zasilone, ciemny = brak sygnału, szary = wejście wyłączone.
     */
    private void drawDiagram(GuiGraphics g, int bx, int by) {
        Level level = Minecraft.getInstance().level;
        BlockState state = level != null ? level.getBlockState(pos) : null;
        if (level == null || !(state.getBlock() instanceof GateBlock)) return;
        Direction f = state.getValue(GateBlock.FACING);

        g.fill(bx - 1, by - 1, bx + 41, by + 41, 0xFF202022);
        g.fill(bx, by, bx + 40, by + 40, 0xFF78787A);
        g.fill(bx + 12, by + 14, bx + 28, by + 26, 0xFF2A2A2C);

        // kolejność: przód, lewo, tył, prawo
        Direction[] sides = {f, f.getCounterClockWise(), f.getOpposite(), f.getClockWise()};
        int[] bits = {0, GateType.IN_L, GateType.IN_B, GateType.IN_R};
        for (int i = 0; i < 4; i++) {
            Direction d = sides[i];
            int out = state.getSignal(level, pos, d.getOpposite());
            boolean isInput = bits[i] != 0 && (type.inputs() & bits[i]) != 0;
            boolean isOutput = i == 0 || out > 0 || outputSide(state, i);
            if (!isInput && !isOutput) continue;
            int color;
            if (out > 0) color = 0xFFFF3A2A;
            else if (isInput && type.canDisableInputs() && (disabled & bits[i]) != 0) color = 0xFF4A4A4A;
            else if (isInput && level.getSignal(pos.relative(d), d) > 0) color = 0xFFC82020;
            else color = 0xFF5A1410;
            switch (i) {
                case 0 -> g.fill(bx + 17, by + 2, bx + 23, by + 13, color);
                case 1 -> g.fill(bx + 2, by + 17, bx + 11, by + 23, color);
                case 2 -> g.fill(bx + 17, by + 27, bx + 23, by + 38, color);
                default -> g.fill(bx + 29, by + 17, bx + 38, by + 23, color);
            }
        }
        // Strzałka wyjścia
        g.fill(bx + 15, by + 1, bx + 25, by + 2, 0xFF202022);
    }

    /** Strony, które w tej bramce bywają wyjściami (poza przodem) - rysowane także, gdy nie świecą. */
    private boolean outputSide(BlockState state, int i) {
        return switch (type) {
            case TIMER -> i == 1 || i == 3;
            case SEQUENCER -> true;
            case RS_LATCH -> i == 2 || !quiet;
            case TOGGLE -> i == 2;
            case COUNTER -> i == 2;
            case RANDOMIZER -> i == 1 || i == 3;
            case BUFFER, NOT -> (i == 1 && (disabled & GateType.IN_L) == 0) || (i == 3 && (disabled & GateType.IN_R) == 0);
            case STATE_CELL -> i == (mirrored ? 1 : 3);
            default -> false;
        };
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2, y = (height - h) / 2;
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + W, y + h, 0xFF3A3A40);
        g.fill(x + 4, y + 22, x + W - 4, y + 23, 0xFF5A5A62);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
