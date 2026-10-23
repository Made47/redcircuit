package pl.geesio.redcircuit.wireless;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

import java.util.List;

/** Pilot: trzymanie PPM nadaje na ustawionej częstotliwości; Shift + PPM otwiera ustawienia. */
public class ItemWirelessRemote extends Item {
    public ItemWirelessRemote() {
        setUnlocalizedName(RedCircuit.MODID + ".wireless_remote");
        setTextureName(RedCircuit.MODID + ":wireless_remote");
        setMaxStackSize(1);
        setCreativeTab(RCTab.TAB);
    }

    public static int frequency(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("Frequency") ? WirelessNetwork.clampFreq(tag.getInteger("Frequency")) : 1;
    }

    public static void setFrequency(ItemStack stack, int freq) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Frequency", WirelessNetwork.clampFreq(freq));
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            if (world.isRemote) RedCircuit.proxy.openFrequencyGui(1, 0, 0, 0, frequency(stack), getUnlocalizedName() + ".name");
            return stack;
        }
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (!player.worldObj.isRemote) WirelessNetwork.get().pulseRemote(frequency(stack), player.getUniqueID());
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72_000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.none;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted("tooltip.redcircuit.frequency", frequency(stack)));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.redcircuit.remote_hint"));
    }
}
