package pl.geesio.redcircuit.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** Przedmiot bloku z wariantami w metadanych (nazwa: tile.redcircuit.ore.tin itp., kolor z bloku). */
public class ItemBlockMeta extends ItemBlock {
    public ItemBlockMeta(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        Block block = Block.getBlockFromItem(this);
        if (block instanceof IMetaNames names) {
            String n = names.nameFor(stack.getItemDamage());
            if (n != null) return super.getUnlocalizedName() + "." + n;
        }
        return super.getUnlocalizedName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return Block.getBlockFromItem(this).getRenderColor(stack.getItemDamage());
    }

    /** Bloki z nazwanymi wariantami. */
    public interface IMetaNames {
        /** Nazwa wariantu albo null (bez wariantów). */
        String nameFor(int meta);
    }
}
