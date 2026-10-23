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
 * Receptura maceratora: jeden przedmiot -> wynik (+ opcjonalny produkt uboczny z szansą).
 * JSON:
 * <pre>{
 *   "type": "redcircuit:macerating",
 *   "ingredient": {"tag": "c:ores/iron"},
 *   "result": {"id": "redcircuit:iron_dust", "count": 2},
 *   "bonus": {"id": "minecraft:gravel"}, "bonus_chance": 0.1,
 *   "time": 200
 * }</pre>
 */
public record MaceratingRecipe(Ingredient ingredient, ItemStack result, ItemStack bonus, float bonusChance, int time)
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
        return ModRecipes.MACERATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MACERATING.get();
    }

    public static class Serializer implements RecipeSerializer<MaceratingRecipe> {
        private static final MapCodec<MaceratingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MaceratingRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MaceratingRecipe::result),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("bonus", ItemStack.EMPTY).forGetter(MaceratingRecipe::bonus),
                Codec.FLOAT.optionalFieldOf("bonus_chance", 0f).forGetter(MaceratingRecipe::bonusChance),
                Codec.INT.optionalFieldOf("time", 200).forGetter(MaceratingRecipe::time)
        ).apply(i, MaceratingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MaceratingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, MaceratingRecipe::ingredient,
                ItemStack.STREAM_CODEC, MaceratingRecipe::result,
                ItemStack.OPTIONAL_STREAM_CODEC, MaceratingRecipe::bonus,
                ByteBufCodecs.FLOAT, MaceratingRecipe::bonusChance,
                ByteBufCodecs.VAR_INT, MaceratingRecipe::time,
                MaceratingRecipe::new);

        @Override
        public MapCodec<MaceratingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MaceratingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
