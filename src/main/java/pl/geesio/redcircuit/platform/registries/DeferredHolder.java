package pl.geesio.redcircuit.platform.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Odpowiednik DeferredHolder z NeoForge: wpis rejestru tworzony przy rejestracji
 * ({@link DeferredRegister#register()}), dostępny przez {@link #get()}.
 */
public class DeferredHolder<R, T extends R> implements Supplier<T> {
    private final ResourceKey<R> key;
    final Supplier<? extends T> factory;
    T value;

    DeferredHolder(ResourceKey<R> key, Supplier<? extends T> factory) {
        this.key = key;
        this.factory = factory;
    }

    @Override
    public T get() {
        if (value == null) throw new IllegalStateException("Registry entry not present yet: " + key.location());
        return value;
    }

    public T value() {
        return get();
    }

    public boolean isBound() {
        return value != null;
    }

    public ResourceLocation getId() {
        return key.location();
    }

    public ResourceKey<R> getKey() {
        return key;
    }
}
