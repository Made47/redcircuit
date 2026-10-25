package pl.geesio.redcircuit.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.alloy.AlloyFurnaceBlockEntity;
import pl.geesio.redcircuit.energy.ElectricFurnaceBlockEntity;
import pl.geesio.redcircuit.energy.GeneratorBlockEntity;
import pl.geesio.redcircuit.gate.GateBlock;
import pl.geesio.redcircuit.gate.GateBlockEntity;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.light.LampBlock;
import pl.geesio.redcircuit.tube.TubeBlockEntity;
import pl.geesio.redcircuit.wire.BundledCableBlockEntity;
import pl.geesio.redcircuit.wire.RedWireBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Dostawcy informacji dla Jade. Każdy można wyłączyć w ustawieniach Jade (wtyczka RedCircuit). */
final class Providers {
    private Providers() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(RedCircuit.MODID, path);
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.2f", ticks / 20.0);
    }

    /** Przewody sygnałowe: poziom sygnału (ze stanu bloku, bez danych z serwera). */
    static final IBlockComponentProvider SIGNAL = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockState s = accessor.getBlockState();
            if (!s.hasProperty(RedWireBlock.POWER)) return;
            int p = s.getValue(RedWireBlock.POWER);
            tooltip.add(Component.translatable("jade.redcircuit.signal", p)
                    .withStyle(p > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
        }

        @Override
        public Identifier getUid() {
            return id("signal");
        }
    };

    /** Kabel wiązkowy: aktywne kanały. */
    static final Both BUNDLED = new Both("bundled") {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof BundledCableBlockEntity be) {
                byte[] ch = new byte[16];
                for (int i = 0; i < 16; i++) ch[i] = (byte) be.getChannel(i);
                data.putByteArray("Channels", ch);
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            byte[] ch = accessor.getServerData().getByteArray("Channels").orElse(new byte[0]);
            if (ch.length < 16) return;
            MutableComponent line = Component.translatable("jade.redcircuit.channels");
            boolean any = false;
            for (int i = 0; i < 16; i++) {
                if (ch[i] <= 0) continue;
                any = true;
                DyeColor c = DyeColor.byId(i);
                line.append(Component.literal(" ■").withColor(c.getTextColor()))
                        .append(Component.literal(Integer.toString(ch[i])).withStyle(ChatFormatting.WHITE));
            }
            tooltip.add(any ? line : Component.translatable("jade.redcircuit.channels_none").withStyle(ChatFormatting.GRAY));
        }
    };

    /** Bramki: stan wyjścia, czas, licznik, wyciszenie. */
    static final Both GATE = new Both("gate") {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof GateBlockEntity be) {
                data.putInt("Interval", be.interval);
                data.putInt("Count", be.count);
                data.putInt("Max", be.max());
                data.putBoolean("Muted", be.muted);
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof GateBlock gate)) return;
            BlockState s = accessor.getBlockState();
            boolean on = s.getValue(GateBlock.POWERED);
            tooltip.add(Component.translatable(on ? "jade.redcircuit.output_on" : "jade.redcircuit.output_off")
                    .withStyle(on ? ChatFormatting.RED : ChatFormatting.GRAY));
            CompoundTag d = accessor.getServerData();
            GateType t = gate.getGateType();
            if (t.hasInterval() && d.contains("Interval")) {
                tooltip.add(Component.translatable("jade.redcircuit.interval", seconds(d.getIntOr("Interval", 0))));
            }
            if (t == GateType.COUNTER && d.contains("Count")) {
                tooltip.add(Component.translatable("jade.redcircuit.count", d.getIntOr("Count", 0), d.getIntOr("Max", 0)));
            }
            int dis = GateBlock.disabledMask(s);
            if (dis != 0) {
                net.minecraft.network.chat.MutableComponent sides = Component.empty();
                String[] names = {"left", "back", "right"};
                int[] bits = {GateType.IN_L, GateType.IN_B, GateType.IN_R};
                for (int i = 0; i < 3; i++) {
                    if ((dis & bits[i]) == 0) continue;
                    if (!sides.getSiblings().isEmpty()) sides.append(", ");
                    sides.append(Component.translatable("screen.redcircuit.gate.side." + names[i]));
                }
                tooltip.add(Component.translatable(t.switchesOutputs() ? "jade.redcircuit.gate_disabled_out" : "jade.redcircuit.gate_disabled", sides).withStyle(ChatFormatting.GRAY));
            }
            if (d.getBooleanOr("Muted", false)) {
                tooltip.add(Component.translatable("jade.redcircuit.muted").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    };

    /** Lampy: świeci / zgaszona, odwrócona. */
    static final IBlockComponentProvider LAMP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof LampBlock lamp)) return;
            boolean lit = accessor.getBlockState().getValue(LampBlock.LIT);
            tooltip.add(Component.translatable(lit ? "jade.redcircuit.lamp_on" : "jade.redcircuit.lamp_off")
                    .withStyle(lit ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
            if (lamp.isInverted()) tooltip.add(Component.translatable("jade.redcircuit.lamp_inverted").withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public Identifier getUid() {
            return id("lamp");
        }
    };

    /** Generatory: bieżąca produkcja. */
    static final Both GENERATOR = new Both("generator") {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof GeneratorBlockEntity be) data.putInt("Rate", be.getLastRate());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains("Rate")) return;
            int rate = accessor.getServerData().getIntOr("Rate", 0);
            tooltip.add(Component.translatable("jade.redcircuit.generating", rate)
                    .withStyle(rate > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
    };

    /** Rury: ile stosów jedzie w środku. */
    static final Both TUBE = new Both("tube") {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof TubeBlockEntity be) data.putInt("Items", be.getItems().size());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            int n = accessor.getServerData().getIntOr("Items", 0);
            if (n > 0) tooltip.add(Component.translatable("jade.redcircuit.tube_items", n));
        }
    };

    /** Piece: postęp przetapiania / stapiania. */
    static final Both FURNACE = new Both("furnace") {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof ElectricFurnaceBlockEntity be) {
                data.putInt("Progress", be.getProgressPercent());
            } else if (accessor.getBlockEntity() instanceof AlloyFurnaceBlockEntity be) {
                data.putInt("Progress", be.getProgressPercent());
                if (!be.isElectric()) data.putInt("Burn", be.getBurnTime());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag d = accessor.getServerData();
            if (d.contains("Progress") && d.getIntOr("Progress", 0) > 0) {
                tooltip.add(Component.translatable("jade.redcircuit.progress", d.getIntOr("Progress", 0)));
            }
            if (d.getIntOr("Burn", 0) > 0) {
                tooltip.add(Component.translatable("jade.redcircuit.burn", seconds(d.getIntOr("Burn", 0))).withStyle(ChatFormatting.GOLD));
            }
        }
    };

    /** Nadajnik / odbiornik: częstotliwość i stan. */
    static final Both WIRELESS = new Both("wireless") {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof pl.geesio.redcircuit.wireless.WirelessBlockEntity be) {
                data.putInt("Freq", be.getFrequency());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getServerData().contains("Freq")) {
                tooltip.add(Component.translatable("tooltip.redcircuit.frequency", accessor.getServerData().getIntOr("Freq", 0))
                        .withStyle(ChatFormatting.AQUA));
            }
            boolean on = accessor.getBlockState().getValue(pl.geesio.redcircuit.wireless.WirelessBlock.POWERED);
            tooltip.add(Component.translatable(on ? "jade.redcircuit.wireless_on" : "jade.redcircuit.wireless_off")
                    .withStyle(on ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
    };

    /** Dostawca z danymi z serwera i podpowiedzią po stronie klienta. */
    abstract static class Both implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final Identifier uid;

        Both(String path) {
            this.uid = id(path);
        }

        @Override
        public Identifier getUid() {
            return uid;
        }
    }
}
