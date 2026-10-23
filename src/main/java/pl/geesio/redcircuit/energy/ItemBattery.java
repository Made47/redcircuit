package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

import java.util.List;

/**
 * Bateria: przenośny magazyn energii (RF). Ładuje się i rozładowuje w akumulatorze (PPM / Shift+PPM),
 * a jako IEnergyContainerItem działa też z ładowarkami innych modów.
 */
public class ItemBattery extends Item implements IEnergyContainerItem {
    public static final int CAPACITY = 50_000, RATE = 1_000;

    public ItemBattery() {
        setUnlocalizedName(RedCircuit.MODID + ".battery");
        setTextureName(RedCircuit.MODID + ":battery");
        setMaxStackSize(1);
        setCreativeTab(RCTab.TAB);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getInteger("Energy") : 0;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Energy", Math.max(0, Math.min(CAPACITY, energy)));
    }

    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        int e = getEnergy(stack);
        int r = Math.min(Math.min(maxReceive, RATE), CAPACITY - e);
        if (r > 0 && !simulate) setEnergy(stack, e + r);
        return Math.max(0, r);
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        int e = getEnergy(stack);
        int r = Math.min(Math.min(maxExtract, RATE), e);
        if (r > 0 && !simulate) setEnergy(stack, e - r);
        return Math.max(0, r);
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        return getEnergy(stack);
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return CAPACITY;
    }

    /** Shift + PPM na akumulatorze trafia do bloku (rozładowanie baterii), a nie do przedmiotu. */
    @Override
    public boolean doesSneakBypassUse(net.minecraft.world.World world, int x, int y, int z, EntityPlayer player) {
        return world.getBlock(x, y, z) instanceof BlockBatteryBox;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - (double) getEnergy(stack) / CAPACITY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item));
        ItemStack full = new ItemStack(item);
        setEnergy(full, CAPACITY);
        list.add(full);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted("tooltip.redcircuit.battery", getEnergy(stack), CAPACITY));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.redcircuit.battery_hint"));
    }
}
