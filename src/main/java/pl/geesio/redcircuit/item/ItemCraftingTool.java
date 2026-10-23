package pl.geesio.redcircuit.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

import java.util.List;

/**
 * Narzędzie używane w siatce wytwarzania (piła ręczna, nożyce do drutu): po każdym wytworzeniu
 * zostaje w siatce i traci 1 punkt wytrzymałości. Naprawa w kowadle materiałem {@code repairOre}.
 */
public class ItemCraftingTool extends Item {
    private final String repairOre;

    public ItemCraftingTool(String name, int durability, String repairOre) {
        this.repairOre = repairOre;
        setUnlocalizedName(RedCircuit.MODID + "." + name);
        setTextureName(RedCircuit.MODID + ":" + name);
        setMaxStackSize(1);
        setMaxDamage(durability - 1);   // 1.7.10: przedmiot pęka po przekroczeniu maxDamage
        setNoRepair();
        setCreativeTab(RCTab.TAB);
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setItemDamage(copy.getItemDamage() + 1);
        copy.stackSize = 1;
        return copy.getItemDamage() > copy.getMaxDamage() ? null : copy;
    }

    @Override
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack stack) {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack tool, ItemStack material) {
        if (material == null) return false;
        for (ItemStack o : OreDictionary.getOres(repairOre)) if (OreDictionary.itemMatches(o, material, false)) return true;
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        int max = stack.getMaxDamage() + 1;
        int left = max - stack.getItemDamage();
        EnumChatFormatting c = left > max / 2 ? EnumChatFormatting.GREEN : left > max / 5 ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;
        tooltip.add(c + StatCollector.translateToLocalFormatted("tooltip.redcircuit.durability", left, max));
    }
}
