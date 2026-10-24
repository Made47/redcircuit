package pl.geesio.redcircuit.alloy;

import pl.geesio.redcircuit.util.Recipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import pl.geesio.redcircuit.util.transfer.IItemHandlerModifiable;

import java.util.List;

/**
 * Receptura pieca do stopów: kilka składników z ilościami (w dowolnych slotach) -> wynik.
 * JSON:
 * <pre>{
 *   "type": "redcircuit:alloying",
 *   "ingredients": [{"ingredient": "minecraft:copper_ingot"}, {"ingredient": "minecraft:redstone", "count": 4}],
 *   "result": {"id": "redcircuit:signal_alloy_ingot"},
 *   "cooktime": 200
 * }</pre>
 */
public record AlloyRecipe(List<SizedIngredient> inputs, ItemStackTemplate output, int cookTime) implements Recipe<AlloyRecipe.Input> {

    /** Wejście receptury: zawartość 9 slotów pieca. */
    public record Input(List<ItemStack> items) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    /** Przydział: ile sztuk z którego slotu zużyć (null, jeśli składników brakuje). */
    public int[] allocate(List<ItemStack> items) {
        int[] avail = new int[items.size()];
        int[] take = new int[items.size()];
        for (int i = 0; i < items.size(); i++) avail[i] = items.get(i).getCount();
        for (SizedIngredient ing : inputs) {
            int need = ing.count();
            for (int i = 0; i < items.size() && need > 0; i++) {
                if (avail[i] <= 0 || !ing.ingredient().test(items.get(i))) continue;
                int t = Math.min(need, avail[i]);
                avail[i] -= t;
                take[i] += t;
                need -= t;
            }
            if (need > 0) return null;
        }
        return take;
    }

    /** Zużywa składniki ze slotów 0..8 podanego magazynu. */
    public void consume(IItemHandlerModifiable handler, int firstSlot, int slots) {
        List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < slots; i++) items.add(handler.getStackInSlot(firstSlot + i));
        int[] take = allocate(items);
        if (take == null) return;
        for (int i = 0; i < slots; i++) {
            if (take[i] <= 0) continue;
            ItemStack s = handler.getStackInSlot(firstSlot + i).copy();
            ItemStack remainder = pl.geesio.redcircuit.util.Recipes.remainder(s);
            s.shrink(take[i]);
            handler.setStackInSlot(firstSlot + i, s.isEmpty() ? remainder : s);
        }
    }

    /** Wynik (nowa kopia). */
    public ItemStack result() {
        return output.create();
    }

    @Override
    public boolean matches(Input input, Level level) {
        return allocate(input.items()) != null;
    }

    @Override
    public ItemStack assemble(Input input) {
        return result();
    }

    /** Składniki (dla JEI). */
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (SizedIngredient s : inputs) list.add(s.ingredient());
        return list;
    }

    /** Nie pokazuj w książce receptur (osobne urządzenie). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<AlloyRecipe> getSerializer() {
        return ModRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<AlloyRecipe> getType() {
        return ModRecipes.ALLOYING.get();
    }

    public static final class Serializer {
        static final MapCodec<AlloyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(AlloyRecipe::inputs),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlloyRecipe::output),
                Codec.INT.optionalFieldOf("cooktime", 200).forGetter(AlloyRecipe::cookTime)
        ).apply(i, AlloyRecipe::new));

        static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyRecipe::inputs,
                ItemStackTemplate.STREAM_CODEC, AlloyRecipe::output,
                ByteBufCodecs.VAR_INT, AlloyRecipe::cookTime,
                AlloyRecipe::new);

        public static final RecipeSerializer<AlloyRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {}
    }
}
