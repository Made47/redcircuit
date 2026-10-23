package pl.geesio.redcircuit.fluid;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** Przedmiot zbiornika - w opisie zawartość. */
public class ItemBlockFluidTank extends ItemBlock {
    public ItemBlockFluidTank(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        FluidStack fs = stack.hasTagCompound() ? FluidStack.loadFluidStackFromNBT(stack.getTagCompound().getCompoundTag("Fluid")) : null;
        String name = fs == null ? StatCollector.translateToLocal("message.redcircuit.pump_empty") : fs.getLocalizedName();
        tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted("message.redcircuit.pipe", name,
                fs == null ? 0 : fs.amount, TileFluidTank.CAPACITY));
    }
}
