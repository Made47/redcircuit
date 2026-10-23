package pl.geesio.redcircuit.tool;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.fluid.FluidUtil;
import pl.geesio.redcircuit.wire.TileWire;
import pl.geesio.redcircuit.wire.WireTypes;

/**
 * Miernik: PPM na bloku wypisuje sygnał redstone (wejście/wyjście, poziom przewodu), kanały kabla wiązkowego,
 * energię (RF) i zawartość zbiorników cieczy.
 */
public class ItemMultimeter extends Item {
    /** Kolory czatu dla 16 kanałów (kolory wełny). */
    private static final EnumChatFormatting[] CHANNEL_COLORS = {
            EnumChatFormatting.WHITE, EnumChatFormatting.GOLD, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.AQUA,
            EnumChatFormatting.YELLOW, EnumChatFormatting.GREEN, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.DARK_GRAY,
            EnumChatFormatting.GRAY, EnumChatFormatting.DARK_AQUA, EnumChatFormatting.DARK_PURPLE, EnumChatFormatting.BLUE,
            EnumChatFormatting.GOLD, EnumChatFormatting.DARK_GREEN, EnumChatFormatting.RED, EnumChatFormatting.BLACK};

    public ItemMultimeter() {
        setUnlocalizedName(RedCircuit.MODID + ".multimeter");
        setTextureName(RedCircuit.MODID + ":multimeter");
        setMaxStackSize(1);
        setCreativeTab(RCTab.TAB);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                                  float hx, float hy, float hz) {
        if (world.isRemote) return false;   // klient wysyła pakiet, serwer odpowiada w czacie
        Block block = world.getBlock(x, y, z);
        ForgeDirection face = ForgeDirection.getOrientation(side);
        String name = block.getLocalizedName();
        player.addChatComponentMessage(styled(new ChatComponentText("- " + name + " -"), EnumChatFormatting.GOLD));

        // Redstone
        TileEntity te = world.getTileEntity(x, y, z);
        Integer type = WireTypes.typeAt(world, x, y, z);
        if (WireTypes.isSignal(type) && te instanceof TileWire w) {
            line(player, "meter.redcircuit.wire_power", w.power);
        } else {
            line(player, "meter.redcircuit.signal_in", world.getStrongestIndirectPower(x, y, z));
            if (block.canProvidePower()) {
                int out = 0;
                for (int s = 0; s < 6; s++) out = Math.max(out, block.isProvidingWeakPower(world, x, y, z, s));
                line(player, "meter.redcircuit.signal_out", out);
            }
        }

        // Kabel wiązkowy
        if (type != null && type == WireTypes.BUNDLED && te instanceof TileWire cable) {
            IChatComponent l = new ChatComponentTranslation("meter.redcircuit.channels");
            boolean any = false;
            for (int ch = 0; ch < 16; ch++) {
                int v = cable.channels[ch];
                if (v <= 0) continue;
                any = true;
                l.appendSibling(styled(new ChatComponentText(" ■"), CHANNEL_COLORS[ch]));
                l.appendSibling(new ChatComponentText(Integer.toString(v)));
            }
            player.addChatComponentMessage(any ? l : new ChatComponentTranslation("meter.redcircuit.channels_none"));
        }

        // Energia
        int stored = -1, max = 0;
        if (te instanceof IEnergyReceiver r) {
            stored = r.getEnergyStored(face);
            max = r.getMaxEnergyStored(face);
        } else if (te instanceof IEnergyProvider p) {
            stored = p.getEnergyStored(face);
            max = p.getMaxEnergyStored(face);
        }
        if (stored >= 0) {
            player.addChatComponentMessage(styled(new ChatComponentTranslation("meter.redcircuit.energy", stored, max), EnumChatFormatting.AQUA));
        }

        // Ciecze
        if (te instanceof IFluidHandler fh) {
            FluidTankInfo[] tanks = fh.getTankInfo(face);
            if (tanks != null) {
                for (FluidTankInfo info : tanks) {
                    if (info == null) continue;
                    player.addChatComponentMessage(styled(new ChatComponentTranslation("meter.redcircuit.fluid",
                            FluidUtil.name(info.fluid), info.fluid == null ? 0 : info.fluid.amount, info.capacity), EnumChatFormatting.BLUE));
                }
            }
        }
        return true;
    }

    private static IChatComponent styled(IChatComponent c, EnumChatFormatting color) {
        c.setChatStyle(new ChatStyle().setColor(color));
        return c;
    }

    private static void line(EntityPlayer player, String key, int value) {
        player.addChatComponentMessage(styled(new ChatComponentTranslation(key, value),
                value > 0 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY));
    }
}
