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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Receptura tartaku: jeden przedmiot -> wynik (+ opcjonalny produkt uboczny z szansą).
 * JSON:
 * <pre>{
 *   "type": "redcircuit:sawing",
 *   "ingredient": {"tag": "minecraft:oak_logs"},
 *   "result": {"id": "minecraft:oak_planks", "count": 6},
 *   "bonus": {"id": "redcircuit:sawdust"}, "bonus_chance": 1.0,
 *   "time": 100
 * }</pre>
 */
public record SawingRecipe(Ingredient ingredient, ItemStack result, ItemStack bonus, float bonusChance, int time)
        implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
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
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SAWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SAWING.get();
    }

    public static class Serializer implements RecipeSerializer<SawingRecipe> {
        private static final MapCodec<SawingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SawingRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SawingRecipe::result),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("bonus", ItemStack.EMPTY).forGetter(SawingRecipe::bonus),
                Codec.FLOAT.optionalFieldOf("bonus_chance", 0f).forGetter(SawingRecipe::bonusChance),
                Codec.INT.optionalFieldOf("time", 100).forGetter(SawingRecipe::time)
        ).apply(i, SawingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SawingRecipe::ingredient,
                ItemStack.STREAM_CODEC, SawingRecipe::result,
                ItemStack.OPTIONAL_STREAM_CODEC, SawingRecipe::bonus,
                ByteBufCodecs.FLOAT, SawingRecipe::bonusChance,
                ByteBufCodecs.VAR_INT, SawingRecipe::time,
                SawingRecipe::new);

        @Override
        public MapCodec<SawingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
