package pl.geesio.redcircuit.alloy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import pl.geesio.redcircuit.platform.common.crafting.SizedIngredient;
import pl.geesio.redcircuit.platform.items.IItemHandlerModifiable;

import java.util.List;

/**
 * Receptura pieca do stopów: kilka składników z ilościami (w dowolnych slotach) -> wynik.
 * JSON:
 * <pre>{
 *   "type": "redcircuit:alloying",
 *   "ingredients": [{"item": "minecraft:copper_ingot"}, {"item": "minecraft:redstone", "count": 4}],
 *   "result": {"id": "redcircuit:signal_alloy_ingot"},
 *   "cooktime": 200
 * }</pre>
 */
public record AlloyRecipe(List<SizedIngredient> inputs, ItemStack result, int cookTime) implements Recipe<AlloyRecipe.Input> {

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
            ItemStack remainder = !s.getRecipeRemainder().isEmpty() ? s.getRecipeRemainder() : ItemStack.EMPTY;
            s.shrink(take[i]);
            handler.setStackInSlot(firstSlot + i, s.isEmpty() ? remainder : s);
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return allocate(input.items()) != null;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALLOYING.get();
    }

    public static class Serializer implements RecipeSerializer<AlloyRecipe> {
        private static final MapCodec<AlloyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(AlloyRecipe::inputs),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlloyRecipe::result),
                Codec.INT.optionalFieldOf("cooktime", 200).forGetter(AlloyRecipe::cookTime)
        ).apply(i, AlloyRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyRecipe::inputs,
                ItemStack.STREAM_CODEC, AlloyRecipe::result,
                ByteBufCodecs.VAR_INT, AlloyRecipe::cookTime,
                AlloyRecipe::new);

        @Override
        public MapCodec<AlloyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
