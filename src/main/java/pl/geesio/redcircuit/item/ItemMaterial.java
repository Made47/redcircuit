package pl.geesio.redcircuit.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

/**
 * Prosty materiał (sztabka, bryłka, pył, zębatka). Ikona może być teksturą z gry
 * (np. "iron_ingot") przebarwioną na kolor metalu - jak w wersji 1.21.
 */
public class ItemMaterial extends Item {
    private final String icon;
    private final int color;

    /**
     * @param name  nazwa rejestru (redcircuit:name) i klucz tłumaczenia
     * @param icon  tekstura: "redcircuit:..." albo nazwa tekstury z gry ("iron_ingot")
     * @param color kolor przebarwienia RGB (0xFFFFFF = bez zmian)
     */
    public ItemMaterial(String name, String icon, int color) {
        this.icon = icon;
        this.color = color;
        setUnlocalizedName(RedCircuit.MODID + "." + name);
        setCreativeTab(RCTab.TAB);
    }

    public ItemMaterial(String name) {
        this(name, RedCircuit.MODID + ":" + name, 0xFFFFFF);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(icon);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return color;
    }
}
