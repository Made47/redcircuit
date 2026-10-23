package pl.geesio.redcircuit.platform.common.crafting;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;

/**
 * Składnik z ilością (jak SizedIngredient z NeoForge). Format JSON "płaski":
 * {"item": "minecraft:redstone", "count": 4} albo {"tag": "c:ingots/copper"} (count domyślnie 1).
 */
public record SizedIngredient(Ingredient ingredient, int count) {
    public static final Codec<SizedIngredient> FLAT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<SizedIngredient, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                int count = 1;
                T c = map.get("count");
                if (c != null) count = ops.getNumberValue(c).result().map(Number::intValue).orElse(1);
                T stripped = ops.createMap(map.entries().filter(p ->
                        !"count".equals(ops.getStringValue(p.getFirst()).result().orElse(""))));
                final int fc = Math.max(1, count);
                return Ingredient.CODEC_NONEMPTY.decode(ops, stripped)
                        .map(p -> Pair.of(new SizedIngredient(p.getFirst(), fc), input));
            });
        }

        @Override
        public <T> DataResult<T> encode(SizedIngredient s, DynamicOps<T> ops, T prefix) {
            return Ingredient.CODEC_NONEMPTY.encodeStart(ops, s.ingredient())
                    .flatMap(t -> s.count() == 1 ? DataResult.success(t)
                            : ops.mergeToMap(t, ops.createString("count"), ops.createInt(s.count())));
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
            ByteBufCodecs.VAR_INT, SizedIngredient::count,
            SizedIngredient::new);

    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    /** Przedmioty do pokazania (np. w REI) z właściwą liczbą sztuk. */
    public ItemStack[] getItems() {
        return Arrays.stream(ingredient.getItems()).map(s -> s.copyWithCount(count)).toArray(ItemStack[]::new);
    }
}
