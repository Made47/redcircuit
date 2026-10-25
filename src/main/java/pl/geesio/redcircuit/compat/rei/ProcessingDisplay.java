package pl.geesio.redcircuit.compat.rei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/** Macerator / tartak w REI: składnik -> wynik (+ dodatek z szansą). */
public class ProcessingDisplay extends BasicDisplay {
    public static final MapCodec<ProcessingDisplay> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("category").forGetter(d -> d.category.getIdentifier()),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(d -> d.ingredient),
            ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(d -> d.result),
            ItemStack.OPTIONAL_CODEC.fieldOf("bonus").forGetter(d -> d.bonus),
            Codec.FLOAT.fieldOf("chance").forGetter(ProcessingDisplay::bonusChance),
            Codec.INT.fieldOf("time").forGetter(ProcessingDisplay::time)
    ).apply(i, (c, ing, r, b, ch, t) -> new ProcessingDisplay(CategoryIdentifier.of(c), ing, r, b, ch, t)));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingDisplay> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, d -> d.category.getIdentifier(),
            Ingredient.CONTENTS_STREAM_CODEC, d -> d.ingredient,
            ItemStack.OPTIONAL_STREAM_CODEC, d -> d.result,
            ItemStack.OPTIONAL_STREAM_CODEC, d -> d.bonus,
            ByteBufCodecs.FLOAT, ProcessingDisplay::bonusChance,
            ByteBufCodecs.VAR_INT, ProcessingDisplay::time,
            (c, ing, r, b, ch, t) -> new ProcessingDisplay(CategoryIdentifier.of(c), ing, r, b, ch, t));

    public static final DisplaySerializer<ProcessingDisplay> SERIALIZER = DisplaySerializer.of(CODEC, STREAM_CODEC);

    private final CategoryIdentifier<ProcessingDisplay> category;
    private final Ingredient ingredient;
    private final ItemStack result, bonus;
    private final float bonusChance;
    private final int time;

    public ProcessingDisplay(CategoryIdentifier<ProcessingDisplay> category, Ingredient ingredient, ItemStack result,
                             ItemStack bonus, float bonusChance, int time) {
        super(List.of(EntryIngredients.ofIngredient(ingredient)), outputs(result, bonus));
        this.category = category;
        this.ingredient = ingredient;
        this.result = result;
        this.bonus = bonus;
        this.bonusChance = bonusChance;
        this.time = time;
    }

    private static List<EntryIngredient> outputs(ItemStack result, ItemStack bonus) {
        List<EntryIngredient> list = new ArrayList<>();
        list.add(EntryIngredients.of(result));
        if (!bonus.isEmpty()) list.add(EntryIngredients.of(bonus));
        return list;
    }

    public float bonusChance() {
        return bonusChance;
    }

    public int time() {
        return time;
    }

    public boolean hasBonus() {
        return !bonus.isEmpty();
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }
}
