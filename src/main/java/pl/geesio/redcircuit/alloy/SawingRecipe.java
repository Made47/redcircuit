package pl.geesio.redcircuit.alloy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Optional;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Receptura tartaku: jeden przedmiot -> wynik (+ opcjonalny produkt uboczny z szansą).
 * JSON:
 * <pre>{
 *   "type": "redcircuit:sawing",
 *   "ingredient": "#minecraft:oak_logs",
 *   "result": {"id": "minecraft:oak_planks", "count": 6},
 *   "bonus": {"id": "redcircuit:sawdust"}, "bonus_chance": 1.0,
 *   "time": 100
 * }</pre>
 */
public record SawingRecipe(Ingredient ingredient, ItemStackTemplate output, Optional<ItemStackTemplate> bonusOutput, float bonusChance, int time)
        implements Recipe<SingleRecipeInput> {

    /** Wynik (nowa kopia). */
    public ItemStack result() {
        return output.create();
    }

    /** Produkt uboczny (nowa kopia) albo pusty stos. */
    public ItemStack bonus() {
        return bonusOutput.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result();
    }

    /** Składniki (dla JEI). */
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(ingredient, ingredient);
    }

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
    public RecipeSerializer<SawingRecipe> getSerializer() {
        return ModRecipes.SAWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<SawingRecipe> getType() {
        return ModRecipes.SAWING.get();
    }

    public static final class Serializer {
        static final MapCodec<SawingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(SawingRecipe::ingredient),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(SawingRecipe::output),
                ItemStackTemplate.CODEC.optionalFieldOf("bonus").forGetter(SawingRecipe::bonusOutput),
                Codec.FLOAT.optionalFieldOf("bonus_chance", 0f).forGetter(SawingRecipe::bonusChance),
                Codec.INT.optionalFieldOf("time", 100).forGetter(SawingRecipe::time)
        ).apply(i, SawingRecipe::new));

        static final StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SawingRecipe::ingredient,
                ItemStackTemplate.STREAM_CODEC, SawingRecipe::output,
                ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), SawingRecipe::bonusOutput,
                ByteBufCodecs.FLOAT, SawingRecipe::bonusChance,
                ByteBufCodecs.VAR_INT, SawingRecipe::time,
                SawingRecipe::new);

        public static final RecipeSerializer<SawingRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {}
    }
}
