package pl.geesio.redcircuit.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.geesio.redcircuit.RedCircuit;

/** Komponenty danych przedmiotów (1.21: zamiast NBT na ItemStacku). */
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RedCircuit.MODID);

    /** Energia (FE) zgromadzona w przedmiocie, np. w baterii. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.register("energy", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Zawartość zbiornika na ciecz (zachowywana po rozbiciu bloku). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent>> FLUID =
            COMPONENTS.register("fluid", () -> DataComponentType.<net.neoforged.neoforge.fluids.SimpleFluidContent>builder()
                    .persistent(net.neoforged.neoforge.fluids.SimpleFluidContent.CODEC)
                    .networkSynchronized(net.neoforged.neoforge.fluids.SimpleFluidContent.STREAM_CODEC)
                    .build());

    /** Tryb wiertarki: true = kopanie 3x3. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DRILL_AREA =
            COMPONENTS.register("drill_area", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** Częstotliwość pilota bezprzewodowego. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FREQUENCY =
            COMPONENTS.register("frequency", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
}
