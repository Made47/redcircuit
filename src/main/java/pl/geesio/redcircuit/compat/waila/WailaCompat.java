package pl.geesio.redcircuit.compat.waila;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import pl.geesio.redcircuit.alloy.TileAlloyFurnace;
import pl.geesio.redcircuit.energy.TileEnergyInv;
import pl.geesio.redcircuit.energy.TileGenerator;
import pl.geesio.redcircuit.gate.BlockGate;
import pl.geesio.redcircuit.gate.GateType;
import pl.geesio.redcircuit.gate.TileGate;
import pl.geesio.redcircuit.light.BlockLamp;
import pl.geesio.redcircuit.tube.TileTube;
import pl.geesio.redcircuit.wire.BlockRedWire;
import pl.geesio.redcircuit.wire.TileWire;
import pl.geesio.redcircuit.wire.WireTypes;
import pl.geesio.redcircuit.wireless.BlockWireless;
import pl.geesio.redcircuit.wireless.TileWireless;

import java.util.List;
import java.util.Locale;

/**
 * Integracja z Waila: sygnał przewodów, kanały kabli wiązkowych, stan bramek, lamp i nadajników, produkcja
 * generatorów, przedmioty w rurach, energia, płyny i postęp maszyn. Rejestrowana przez IMC ({@link #callbackRegister}).
 */
public class WailaCompat implements IWailaDataProvider {
    private static final WailaCompat INSTANCE = new WailaCompat();
    private static final EnumChatFormatting[] DYE = {
            EnumChatFormatting.WHITE, EnumChatFormatting.GOLD, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.AQUA,
            EnumChatFormatting.YELLOW, EnumChatFormatting.GREEN, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.DARK_GRAY,
            EnumChatFormatting.GRAY, EnumChatFormatting.DARK_AQUA, EnumChatFormatting.DARK_PURPLE, EnumChatFormatting.BLUE,
            EnumChatFormatting.GOLD, EnumChatFormatting.DARK_GREEN, EnumChatFormatting.RED, EnumChatFormatting.BLACK};

    public static void callbackRegister(IWailaRegistrar reg) {
        // jedna rejestracja dla wszystkich bloków-encji (filtr ours()), żeby linie się nie dublowały
        reg.registerBodyProvider(INSTANCE, TileEntity.class);
        reg.registerNBTProvider(INSTANCE, TileEntity.class);
        reg.registerBodyProvider(INSTANCE, BlockLamp.class);
    }

    private static String tr(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    private static boolean ours(Object o) {
        return o != null && o.getClass().getName().startsWith("pl.geesio.redcircuit.");
    }

    // ------------------------------------------------------------------ dane z serwera

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
        if (te == null || !ours(te)) return tag;
        if (te instanceof TileWire w) {
            tag.setInteger("rcPower", w.power);
            tag.setByteArray("rcChannels", w.channels.clone());
        }
        if (te instanceof TileGate g) {
            tag.setInteger("rcInterval", g.interval);
            tag.setInteger("rcCount", g.count);
            tag.setInteger("rcMax", g.max());
            tag.setBoolean("rcMuted", g.muted);
            tag.setInteger("rcDisabled", g.disabledMask());
            tag.setBoolean("rcPowered", g.powered);
        }
        if (te instanceof TileWireless w) tag.setInteger("rcFreq", w.getFrequency());
        if (te instanceof TileTube t) tag.setInteger("rcItems", t.getItems().size());
        if (te instanceof TileGenerator g) tag.setInteger("rcRate", g.lastRate());
        if (te instanceof TileEnergyInv m) {
            int max = m.field(TileEnergyInv.F_MAX_PROGRESS);
            if (max > 0) tag.setInteger("rcProgress", m.field(TileEnergyInv.F_PROGRESS) * 100 / max);
            tag.setInteger("rcBurn", m.field(TileEnergyInv.F_BURN));
        }
        if (te instanceof TileAlloyFurnace f) {
            if (f.cookTime > 0) tag.setInteger("rcProgress", f.progress * 100 / f.cookTime);
            if (!f.electric) tag.setInteger("rcBurn", f.burnTime);
        }
        int stored = -1, cap = 0;
        if (te instanceof IEnergyHandler h) {
            stored = h.getEnergyStored(ForgeDirection.UNKNOWN);
            cap = h.getMaxEnergyStored(ForgeDirection.UNKNOWN);
        } else if (te instanceof IEnergyReceiver r) {
            stored = r.getEnergyStored(ForgeDirection.UNKNOWN);
            cap = r.getMaxEnergyStored(ForgeDirection.UNKNOWN);
        } else if (te instanceof IEnergyProvider p) {
            stored = p.getEnergyStored(ForgeDirection.UNKNOWN);
            cap = p.getMaxEnergyStored(ForgeDirection.UNKNOWN);
        }
        if (stored >= 0 && cap > 0) {
            tag.setInteger("rcEnergy", stored);
            tag.setInteger("rcEnergyMax", cap);
        }
        if (te instanceof IFluidHandler fh) {
            FluidTankInfo[] info = fh.getTankInfo(ForgeDirection.UNKNOWN);
            if (info != null && info.length > 0 && info[0] != null) {
                FluidStack fs = info[0].fluid;
                tag.setInteger("rcFluidCap", info[0].capacity);
                if (fs != null && fs.getFluid() != null) {
                    tag.setString("rcFluid", fs.getLocalizedName());
                    tag.setInteger("rcFluidAmount", fs.amount);
                }
            }
        }
        return tag;
    }

    // ------------------------------------------------------------------ podpowiedź

    @Override
    public List<String> getWailaBody(ItemStack stack, List<String> tip, IWailaDataAccessor acc, IWailaConfigHandler config) {
        Block block = acc.getBlock();
        NBTTagCompound d = acc.getNBTData();
        if (d == null) d = new NBTTagCompound();
        TileEntity te = acc.getTileEntity();
        if (block instanceof BlockLamp lamp) {
            tip.add((lamp.isLit() ? EnumChatFormatting.YELLOW : EnumChatFormatting.GRAY)
                    + tr(lamp.isLit() ? "waila.redcircuit.lamp_on" : "waila.redcircuit.lamp_off"));
            if (lamp.isInverted()) tip.add(EnumChatFormatting.DARK_GRAY + tr("waila.redcircuit.lamp_inverted"));
            return tip;
        }
        if (te == null || !ours(te)) return tip;

        if (te instanceof TileWire && d.hasKey("rcPower")) {
            Integer type = WireTypes.typeAt(acc.getWorld(), acc.getPosition().blockX, acc.getPosition().blockY, acc.getPosition().blockZ);
            if (type != null && type == WireTypes.BUNDLED) channels(tip, d.getByteArray("rcChannels"));
            else if (block instanceof BlockRedWire) {
                int p = d.getInteger("rcPower");
                tip.add((p > 0 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + tr("waila.redcircuit.signal", p));
            }
        }
        if (block instanceof BlockGate gate && d.hasKey("rcInterval")) gate(tip, gate.getGateType(), d);
        if (block instanceof BlockWireless) {
            if (d.hasKey("rcFreq")) tip.add(EnumChatFormatting.AQUA + tr("tooltip.redcircuit.frequency", d.getInteger("rcFreq")));
            boolean on = (acc.getMetadata() & 1) != 0;
            tip.add((on ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + tr(on ? "waila.redcircuit.wireless_on" : "waila.redcircuit.wireless_off"));
        }
        if (d.hasKey("rcRate")) {
            int rate = d.getInteger("rcRate");
            tip.add((rate > 0 ? EnumChatFormatting.AQUA : EnumChatFormatting.GRAY) + tr("waila.redcircuit.generating", rate));
        }
        if (d.getInteger("rcItems") > 0) tip.add(tr("waila.redcircuit.tube_items", d.getInteger("rcItems")));
        if (d.hasKey("rcEnergyMax")) tip.add(tr("meter.redcircuit.energy", d.getInteger("rcEnergy"), d.getInteger("rcEnergyMax")));
        if (d.hasKey("rcFluidCap")) {
            String fluid = d.hasKey("rcFluid") ? d.getString("rcFluid") : "-";
            tip.add(tr("meter.redcircuit.fluid", fluid, d.getInteger("rcFluidAmount"), d.getInteger("rcFluidCap")));
        }
        if (d.getInteger("rcProgress") > 0) tip.add(tr("waila.redcircuit.progress", d.getInteger("rcProgress")));
        if (d.getInteger("rcBurn") > 0) {
            tip.add(EnumChatFormatting.GOLD + tr("waila.redcircuit.burn", seconds(d.getInteger("rcBurn"))));
        }
        return tip;
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.2f", ticks / 20.0);
    }

    private static void channels(List<String> tip, byte[] ch) {
        if (ch.length < 16) return;
        StringBuilder sb = new StringBuilder(tr("waila.redcircuit.channels"));
        boolean any = false;
        for (int i = 0; i < 16; i++) {
            if (ch[i] <= 0) continue;
            any = true;
            sb.append(' ').append(DYE[i]).append('■').append(EnumChatFormatting.WHITE).append(ch[i]);
        }
        tip.add(any ? sb.toString() : EnumChatFormatting.GRAY + tr("waila.redcircuit.channels_none"));
    }

    private static void gate(List<String> tip, GateType t, NBTTagCompound d) {
        boolean on = d.getBoolean("rcPowered");
        tip.add((on ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + tr(on ? "waila.redcircuit.output_on" : "waila.redcircuit.output_off"));
        if (t.hasInterval()) tip.add(tr("waila.redcircuit.interval", seconds(d.getInteger("rcInterval"))));
        if (t == GateType.COUNTER) tip.add(tr("waila.redcircuit.count", d.getInteger("rcCount"), d.getInteger("rcMax")));
        int dis = d.getInteger("rcDisabled");
        if (dis != 0) {
            String[] names = {"left", "back", "right"};
            int[] bits = {GateType.IN_L, GateType.IN_B, GateType.IN_R};
            StringBuilder sides = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                if ((dis & bits[i]) == 0) continue;
                if (sides.length() > 0) sides.append(", ");
                sides.append(tr("screen.redcircuit.gate.side." + names[i]));
            }
            tip.add(EnumChatFormatting.GRAY + tr(t.switchesOutputs() ? "waila.redcircuit.gate_disabled_out" : "waila.redcircuit.gate_disabled", sides));
        }
        if (d.getBoolean("rcMuted")) tip.add(EnumChatFormatting.DARK_GRAY + tr("waila.redcircuit.muted"));
    }

    // ------------------------------------------------------------------ nieużywane

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor acc, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack stack, List<String> tip, IWailaDataAccessor acc, IWailaConfigHandler config) {
        return tip;
    }

    @Override
    public List<String> getWailaTail(ItemStack stack, List<String> tip, IWailaDataAccessor acc, IWailaConfigHandler config) {
        return tip;
    }
}
