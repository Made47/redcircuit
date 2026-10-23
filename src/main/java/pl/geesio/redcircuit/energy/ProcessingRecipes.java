package pl.geesio.redcircuit.energy;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import pl.geesio.redcircuit.item.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Receptury maceratora i tartaku (w 1.21 pliki JSON typów redcircuit:macerating / redcircuit:sawing).
 * Składnik: nazwa OreDictionary albo konkretny przedmiot (metadane WILDCARD = dowolne).
 */
public final class ProcessingRecipes {
    private ProcessingRecipes() {}

    public static final class Recipe {
        public final String ore;
        public final ItemStack input;
        public final ItemStack result, bonus;
        public final float bonusChance;
        public final int time;

        Recipe(String ore, ItemStack input, ItemStack result, ItemStack bonus, float bonusChance, int time) {
            this.ore = ore;
            this.input = input;
            this.result = result;
            this.bonus = bonus;
            this.bonusChance = bonusChance;
            this.time = time;
        }

        public boolean matches(ItemStack stack) {
            if (stack == null) return false;
            if (ore != null) {
                for (ItemStack o : OreDictionary.getOres(ore)) if (OreDictionary.itemMatches(o, stack, false)) return true;
                return false;
            }
            return OreDictionary.itemMatches(input, stack, false);
        }

        /** Przedmioty pasujące do składnika (np. do NEI). */
        public List<ItemStack> inputs() {
            List<ItemStack> out = new ArrayList<>();
            if (ore != null) out.addAll(OreDictionary.getOres(ore));
            else out.add(input);
            return out;
        }
    }

    public static final List<Recipe> MACERATING = new ArrayList<>();
    public static final List<Recipe> SAWING = new ArrayList<>();

    private static void mac(Object in, ItemStack result, ItemStack bonus, float chance, int time) {
        MACERATING.add(in instanceof String s ? new Recipe(s, null, result, bonus, chance, time)
                : new Recipe(null, (ItemStack) in, result, bonus, chance, time));
    }

    private static void saw(Object in, ItemStack result, ItemStack bonus, float chance, int time) {
        SAWING.add(in instanceof String s ? new Recipe(s, null, result, bonus, chance, time)
                : new Recipe(null, (ItemStack) in, result, bonus, chance, time));
    }

    private static ItemStack st(Object o, int n) {
        if (o instanceof net.minecraft.item.Item i) return new ItemStack(i, n);
        return new ItemStack((net.minecraft.block.Block) o, n);
    }

    private static ItemStack st(Object o, int n, int meta) {
        if (o instanceof net.minecraft.item.Item i) return new ItemStack(i, n, meta);
        return new ItemStack((net.minecraft.block.Block) o, n, meta);
    }

    public static void register() {
        int W = OreDictionary.WILDCARD_VALUE;
        // Macerator: rudy x2 (+10% szansy na trzecią sztukę), sztabki -> pył, różne
        mac("oreIron", st(ModItems.ironDust, 2), st(ModItems.ironDust, 1), 0.1F, 200);
        mac("oreGold", st(ModItems.goldDust, 2), st(ModItems.goldDust, 1), 0.1F, 200);
        mac("oreCopper", st(ModItems.copperDust, 2), st(ModItems.copperDust, 1), 0.1F, 200);
        mac("oreTin", st(ModItems.tinDust, 2), st(ModItems.tinDust, 1), 0.1F, 200);
        mac("oreSilver", st(ModItems.silverDust, 2), st(ModItems.silverDust, 1), 0.1F, 200);
        mac("oreAzurite", st(ModItems.azurite, 6), st(ModItems.azurite, 1), 0.5F, 200);
        mac("oreCoal", st(Items.coal, 2), st(Items.coal, 1), 0.25F, 200);
        mac("oreLapis", st(Items.dye, 8, 4), null, 0F, 200);
        mac("oreRedstone", st(Items.redstone, 6), st(Items.redstone, 1), 0.5F, 200);
        mac("oreQuartz", st(Items.quartz, 2), st(Items.quartz, 1), 0.5F, 200);
        mac("ingotIron", st(ModItems.ironDust, 1), null, 0F, 100);
        mac("ingotGold", st(ModItems.goldDust, 1), null, 0F, 100);
        mac("ingotCopper", st(ModItems.copperDust, 1), null, 0F, 100);
        mac("ingotTin", st(ModItems.tinDust, 1), null, 0F, 100);
        mac("ingotSilver", st(ModItems.silverDust, 1), null, 0F, 100);
        mac("cobblestone", st(Blocks.gravel, 1), null, 0F, 100);
        mac(st(Blocks.stone, 1), st(Blocks.cobblestone, 1), null, 0F, 100);
        mac(st(Blocks.gravel, 1), st(Blocks.sand, 1), st(Items.flint, 1), 0.1F, 100);
        mac(st(Blocks.sandstone, 1, W), st(Blocks.sand, 4), null, 0F, 100);
        mac(st(Blocks.glowstone, 1), st(Items.glowstone_dust, 4), null, 0F, 100);
        mac(st(Items.blaze_rod, 1), st(Items.blaze_powder, 4), null, 0F, 100);
        mac(st(Items.bone, 1), st(Items.dye, 6, 15), null, 0F, 100);
        mac(st(Items.reeds, 1), st(Items.sugar, 2), null, 0F, 100);
        mac(st(Blocks.wool, 1, W), st(Items.string, 4), null, 0F, 100);

        // Tartak: kłody -> 6 desek + trociny, deski -> 3 patyki (25% trocin); kłody innych modów - przez przepis na deski
        ItemStack dust = st(ModItems.sawdust, 1);
        for (int m = 0; m < 4; m++) saw(st(Blocks.log, 1, m), st(Blocks.planks, 6, m), dust, 1.0F, 100);
        for (int m = 0; m < 2; m++) saw(st(Blocks.log2, 1, m), st(Blocks.planks, 6, 4 + m), dust, 1.0F, 100);
        saw("plankWood", st(Items.stick, 3), dust, 0.25F, 60);
    }

    public static Recipe find(List<Recipe> list, ItemStack stack) {
        if (stack == null) return null;
        for (Recipe r : list) if (r.matches(stack)) return r;
        return null;
    }

    /** Receptura tartaku - także kłody innych modów (wynik: deski z przepisu w siatce x 1,5). */
    public static Recipe findSawing(World world, ItemStack stack) {
        Recipe r = find(SAWING, stack);
        if (r != null || stack == null || world == null) return r;
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (!"logWood".equals(OreDictionary.getOreName(id))) continue;
            ItemStack planks = craftSingle(world, stack);
            if (planks == null) return null;
            ItemStack out = planks.copy();
            out.stackSize = Math.min(out.getMaxStackSize(), planks.stackSize * 3 / 2);
            return new Recipe(null, stack, out, st(ModItems.sawdust, 1), 1.0F, 100);
        }
        return null;
    }

    private static ItemStack craftSingle(World world, ItemStack stack) {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        ItemStack one = stack.copy();
        one.stackSize = 1;
        inv.setInventorySlotContents(0, one);
        return CraftingManager.getInstance().findMatchingRecipe(inv, world);
    }
}
