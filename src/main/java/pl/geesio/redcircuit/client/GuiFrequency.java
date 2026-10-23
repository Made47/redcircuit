package pl.geesio.redcircuit.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;
import pl.geesio.redcircuit.network.MessageFrequency;
import pl.geesio.redcircuit.network.RCNetwork;
import pl.geesio.redcircuit.wireless.WirelessNetwork;

/** Okno wyboru częstotliwości (1..5000) dla nadajnika, odbiornika i pilota. */
public class GuiFrequency extends GuiScreen {
    private static final int W = 230, H = 112;
    private static final int[] STEPS = {-100, -10, -1, 1, 10, 100};

    private final int target, gx, gy, gz;
    private final String titleKey;
    private int frequency;
    private GuiTextField field;

    /** @param target 0 = blok, 1 = trzymany pilot */
    public GuiFrequency(int target, int x, int y, int z, int frequency, String titleKey) {
        this.target = target;
        this.gx = x;
        this.gy = y;
        this.gz = z;
        this.frequency = frequency;
        this.titleKey = titleKey;
    }

    private void set(int f) {
        frequency = WirelessNetwork.clampFreq(f);
        field.setText(Integer.toString(frequency));
    }

    private void read() {
        try {
            frequency = WirelessNetwork.clampFreq(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        buttonList.clear();
        int x = (width - W) / 2, y = (height - H) / 2;
        field = new GuiTextField(fontRendererObj, x + 95, y + 31, 60, 18);
        field.setMaxStringLength(4);
        field.setText(Integer.toString(frequency));
        field.setFocused(true);
        for (int i = 0; i < STEPS.length; i++) {
            buttonList.add(new GuiButton(i, x + 8 + i * 36, y + 56, 34, 20, (STEPS[i] > 0 ? "+" : "") + STEPS[i]));
        }
        buttonList.add(new GuiButton(100, x + W - 108, y + H - 26, 100, 20, StatCollector.translateToLocal("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        read();
        if (b.id < STEPS.length) {
            set(frequency + STEPS[b.id]);
        } else {
            RCNetwork.CHANNEL.sendToServer(new MessageFrequency(target, gx, gy, gz, frequency));
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == 28 || key == 156) {   // Enter
            actionPerformed((GuiButton) buttonList.get(buttonList.size() - 1));
            return;
        }
        if (key != 1 && field.isFocused()) {
            if (Character.isDigit(c) || key == 14 || key == 211 || key == 203 || key == 205 || key == 199 || key == 207) {
                field.textboxKeyTyped(c, key);
                read();
            }
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        field.mouseClicked(mx, my, button);
    }

    @Override
    public void updateScreen() {
        field.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawDefaultBackground();
        int x = (width - W) / 2, y = (height - H) / 2;
        drawRect(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF000000);
        drawRect(x, y, x + W, y + H, 0xFF3A3A40);
        drawRect(x + 4, y + 24, x + W - 4, y + 25, 0xFF5A5A62);
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal(titleKey), width / 2, y + 10, 0xFFFFFF);
        drawString(fontRendererObj, StatCollector.translateToLocal("screen.redcircuit.frequency"), x + 10, y + 36, 0xE0E0E0);
        drawString(fontRendererObj, "1-" + WirelessNetwork.MAX_FREQ, x + 160, y + 36, 0xA0A0A0);
        field.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTick);
    }
}
