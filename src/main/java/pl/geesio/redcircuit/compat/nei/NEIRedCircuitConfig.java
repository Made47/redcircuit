package pl.geesio.redcircuit.compat.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.Tags;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.item.ModItems;

/**
 * Konfiguracja NEI (wyszukiwana przez NEI po nazwie klasy NEI*Config): receptury pieca do stopów, rozdrabniacza
 * i tartaku oraz ukrycie bloków technicznych (świecące lampy, plan bez receptury).
 */
public class NEIRedCircuitConfig implements IConfigureNEI {
    @Override
    public void loadConfig() {
        register(new AlloyRecipeHandler());
        register(new ProcessingRecipeHandler.Macerating());
        register(new ProcessingRecipeHandler.Sawing());

        API.hideItem(new ItemStack(ModBlocks.litLamp, 1, OreDictionary.WILDCARD_VALUE));
        API.hideItem(new ItemStack(ModBlocks.invertedLampOff, 1, OreDictionary.WILDCARD_VALUE));
        API.hideItem(new ItemStack(ModItems.plan, 1, OreDictionary.WILDCARD_VALUE));
    }

    private static void register(codechicken.nei.recipe.TemplateRecipeHandler handler) {
        API.registerRecipeHandler(handler);
        API.registerUsageHandler(handler);
    }

    @Override
    public String getName() {
        return "RedCircuit";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }

    static String id(String name) {
        return RedCircuit.MODID + "." + name;
    }
}
