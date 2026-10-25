package pl.geesio.redcircuit.platform.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Składnik z ilością (jak SizedIngredient z NeoForge). Format JSON:
 * {"ingredient": "minecraft:redstone", "count": 4} albo {"ingredient": "#c:ingots/copper"} (count domyślnie 1).
 */
public record SizedIngredient(Ingredient ingredient, int count) {
    public static final Codec<SizedIngredient> NESTED_CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count)
    ).apply(i, SizedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
            ByteBufCodecs.VAR_INT, SizedIngredient::count,
            SizedIngredient::new);

    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    /** Przedmioty do pokazania (np. w REI) z właściwą liczbą sztuk. */
    @SuppressWarnings("deprecation")
    public ItemStack[] getItems() {
        return ingredient.items().map(h -> new ItemStack(h, count)).toArray(ItemStack[]::new);
    }
}
