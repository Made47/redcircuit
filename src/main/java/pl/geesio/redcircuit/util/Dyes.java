package pl.geesio.redcircuit.util;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Barwniki: kolor w numeracji wełny (0 = biały ... 15 = czarny). */
public final class Dyes {
    private Dyes() {}

    public static final String[] ORE = {"dyeWhite", "dyeOrange", "dyeMagenta", "dyeLightBlue", "dyeYellow", "dyeLime",
            "dyePink", "dyeGray", "dyeLightGray", "dyeCyan", "dyePurple", "dyeBlue", "dyeBrown", "dyeGreen", "dyeRed", "dyeBlack"};

    /** Kolor barwnika albo -1, gdy to nie barwnik. */
    public static int colorOf(ItemStack stack) {
        if (stack == null) return -1;
        if (stack.getItem() == Items.dye) return 15 - (stack.getItemDamage() & 15);
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            for (int c = 0; c < 16; c++) if (ORE[c].equals(name)) return c;
        }
        return -1;
    }
}
