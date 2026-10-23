package pl.geesio.redcircuit.craft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pusty plan albo zapisany plan receptury. Zapis w NBT "Plan": 10 stosów - indeksy 0..8 = siatka 3x3, 9 = wynik.
 */
public class ItemPlan extends Item {
    public static final int RESULT_INDEX = 9;
    private final boolean blank;

    public ItemPlan(boolean blank) {
        this.blank = blank;
        String name = blank ? "blank_plan" : "plan";
        setUnlocalizedName(RedCircuit.MODID + "." + name);
        setTextureName(RedCircuit.MODID + ":" + name);
        setMaxStackSize(blank ? 64 : 1);
        if (blank) setCreativeTab(RCTab.TAB);
    }

    public boolean isBlank() {
        return blank;
    }

    public static ItemStack create(Item planItem, ItemStack[] grid, ItemStack result) {
        ItemStack plan = new ItemStack(planItem);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 10; i++) {
            ItemStack s = i < 9 ? grid[i] : result;
            NBTTagCompound e = new NBTTagCompound();
            e.setByte("Slot", (byte) i);
            if (s != null) {
                ItemStack c = s.copy();
                if (i < 9) c.stackSize = 1;
                c.writeToNBT(e);
            }
            list.appendTag(e);
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Plan", list);
        plan.setTagCompound(tag);
        return plan;
    }

    /** 10 stosów: 9 składników + wynik (null = puste). */
    public static ItemStack[] read(ItemStack plan) {
        ItemStack[] out = new ItemStack[10];
        if (plan == null || !plan.hasTagCompound()) return out;
        NBTTagList list = plan.getTagCompound().getTagList("Plan", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            int slot = e.getByte("Slot");
            if (slot >= 0 && slot < 10 && e.hasKey("id")) out[slot] = ItemStack.loadItemStackFromNBT(e);
        }
        return out;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ItemStack result = blank ? null : read(stack)[RESULT_INDEX];
        if (result == null) return super.getItemStackDisplayName(stack);
        return StatCollector.translateToLocalFormatted("item.redcircuit.plan.named", result.getDisplayName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        ItemStack[] data = read(stack);
        ItemStack result = data[RESULT_INDEX];
        if (result == null) return;
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("tooltip.redcircuit.plan.result",
                result.stackSize, result.getDisplayName()));
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < 9; i++) {
            if (data[i] == null) continue;
            counts.merge(data[i].getDisplayName(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            tooltip.add(EnumChatFormatting.DARK_GRAY + " " + e.getValue() + " x " + e.getKey());
        }
    }
}
