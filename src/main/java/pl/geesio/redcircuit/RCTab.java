package pl.geesio.redcircuit;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import pl.geesio.redcircuit.item.ModItems;

/** Zakładka trybu kreatywnego "RedCircuit". */
public final class RCTab {
    private RCTab() {}

    public static final CreativeTabs TAB = new CreativeTabs(RedCircuit.MODID) {
        @Override
        public Item getTabIconItem() {
            return ModItems.signalAlloyIngot;
        }
    };
}
