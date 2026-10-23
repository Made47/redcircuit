package pl.geesio.redcircuit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.geesio.redcircuit.network.FrequencyPayload;
import pl.geesio.redcircuit.wireless.WirelessNetwork;

/** Okno wyboru częstotliwości (1..5000) dla nadajnika, odbiornika i pilota. */
public class FrequencyScreen extends Screen {
    private static final int W = 230, H = 110;

    private final int target;
    private final BlockPos pos;
    private int frequency;
    private EditBox field;

    private FrequencyScreen(Component title, int target, BlockPos pos, int frequency) {
        super(title);
        this.target = target;
        this.pos = pos;
        this.frequency = frequency;
    }

    public static void openForBlock(BlockPos pos, int frequency, String descriptionId) {
        Minecraft.getInstance().setScreen(new FrequencyScreen(Component.translatable(descriptionId), 0, pos, frequency));
    }

    public static void openForItem(InteractionHand hand, int frequency, String descriptionId) {
        Minecraft.getInstance().setScreen(new FrequencyScreen(Component.translatable(descriptionId),
                hand == InteractionHand.MAIN_HAND ? 1 : 2, BlockPos.ZERO, frequency));
    }

    private void set(int f) {
        frequency = WirelessNetwork.clampFreq(f);
        field.setValue(Integer.toString(frequency));
    }

    private void read() {
        try {
            frequency = WirelessNetwork.clampFreq(Integer.parseInt(field.getValue().trim()));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        field = new EditBox(font, x + 95, y + 30, 60, 18, Component.empty());
        field.setMaxLength(4);
        field.setFilter(s -> s.chars().allMatch(Character::isDigit));
        field.setValue(Integer.toString(frequency));
        field.setResponder(s -> read());
        addRenderableWidget(field);

        int[] steps = {-100, -10, -1, 1, 10, 100};
        for (int i = 0; i < steps.length; i++) {
            final int step = steps[i];
            addRenderableWidget(Button.builder(Component.literal((step > 0 ? "+" : "") + step), b -> {
                read();
                set(frequency + step);
            }).bounds(x + 8 + i * 36, y + 54, 34, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            read();
            PacketDistributor.sendToServer(new FrequencyPayload(target, pos, frequency));
            onClose();
        }).bounds(x + W - 108, y + H - 26, 100, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2, y = (height - H) / 2;
        g.drawCenteredString(font, title, width / 2, y + 10, 0xFFFFFF);
        g.drawString(font, Component.translatable("screen.redcircuit.frequency"), x + 10, y + 35, 0xE0E0E0);
        g.drawString(font, "1-" + WirelessNetwork.MAX_FREQ, x + 160, y + 35, 0xA0A0A0);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2, y = (height - H) / 2;
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF000000);
        g.fill(x, y, x + W, y + H, 0xFF3A3A40);
        g.fill(x + 4, y + 24, x + W - 4, y + 25, 0xFF5A5A62);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
