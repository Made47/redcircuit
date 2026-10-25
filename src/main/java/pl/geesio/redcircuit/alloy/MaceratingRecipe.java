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
 * Receptura maceratora: jeden przedmiot -> wynik (+ opcjonalny produkt uboczny z szansą).
 * JSON:
 * <pre>{
 *   "type": "redcircuit:macerating",
 *   "ingredient": "#c:ores/iron",
 *   "result": {"id": "redcircuit:iron_dust", "count": 2},
 *   "bonus": {"id": "minecraft:gravel"}, "bonus_chance": 0.1,
 *   "time": 200
 * }</pre>
 */
public record MaceratingRecipe(Ingredient ingredient, ItemStackTemplate output, Optional<ItemStackTemplate> bonusOutput, float bonusChance, int time)
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
    public RecipeSerializer<MaceratingRecipe> getSerializer() {
        return ModRecipes.MACERATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<MaceratingRecipe> getType() {
        return ModRecipes.MACERATING.get();
    }

    public static final class Serializer {
        static final MapCodec<MaceratingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(MaceratingRecipe::ingredient),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(MaceratingRecipe::output),
                ItemStackTemplate.CODEC.optionalFieldOf("bonus").forGetter(MaceratingRecipe::bonusOutput),
                Codec.FLOAT.optionalFieldOf("bonus_chance", 0f).forGetter(MaceratingRecipe::bonusChance),
                Codec.INT.optionalFieldOf("time", 200).forGetter(MaceratingRecipe::time)
        ).apply(i, MaceratingRecipe::new));

        static final StreamCodec<RegistryFriendlyByteBuf, MaceratingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, MaceratingRecipe::ingredient,
                ItemStackTemplate.STREAM_CODEC, MaceratingRecipe::output,
                ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), MaceratingRecipe::bonusOutput,
                ByteBufCodecs.FLOAT, MaceratingRecipe::bonusChance,
                ByteBufCodecs.VAR_INT, MaceratingRecipe::time,
                MaceratingRecipe::new);

        public static final RecipeSerializer<MaceratingRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);

        private Serializer() {}
    }
}
