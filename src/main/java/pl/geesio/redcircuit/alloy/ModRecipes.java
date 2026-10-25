package pl.geesio.redcircuit.alloy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import pl.geesio.redcircuit.platform.registries.DeferredHolder;
import pl.geesio.redcircuit.platform.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;

public final class ModRecipes {
    private ModRecipes() {}

    /** Typ receptury (zamiast RecipeType.simple z NeoForge). */
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeType<T> simple(String name) {
        final String id = RedCircuit.MODID + ":" + name;
        return new RecipeType<>() {
            @Override
            public String toString() {
                return id;
            }
        };
    }

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, RedCircuit.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, RedCircuit.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyRecipe>> ALLOYING = TYPES.register("alloying",
            () -> simple("alloying"));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlloyRecipe>> ALLOYING_SERIALIZER =
            SERIALIZERS.register("alloying", () -> AlloyRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MaceratingRecipe>> MACERATING = TYPES.register("macerating",
            () -> simple("macerating"));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MaceratingRecipe>> MACERATING_SERIALIZER =
            SERIALIZERS.register("macerating", () -> MaceratingRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SawingRecipe>> SAWING = TYPES.register("sawing",
            () -> simple("sawing"));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SawingRecipe>> SAWING_SERIALIZER =
            SERIALIZERS.register("sawing", () -> SawingRecipe.Serializer.INSTANCE);
}
