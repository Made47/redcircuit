package pl.geesio.redcircuit.platform.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Odpowiednik DeferredRegister z NeoForge dla Fabrica: zbiera wpisy, a {@link #register()}
 * rejestruje je w rejestrze gry (w kolejności dodania). Wywoływane z RedCircuit.onInitialize.
 */
public class DeferredRegister<R> {
    private final ResourceKey<? extends Registry<R>> registryKey;
    private final String modid;
    private final List<DeferredHolder<R, ? extends R>> entries = new ArrayList<>();
    private boolean registered;

    protected DeferredRegister(ResourceKey<? extends Registry<R>> registryKey, String modid) {
        this.registryKey = registryKey;
        this.modid = modid;
    }

    public static <R> DeferredRegister<R> create(ResourceKey<? extends Registry<R>> registryKey, String modid) {
        return new DeferredRegister<>(registryKey, modid);
    }

    public static Blocks createBlocks(String modid) {
        return new Blocks(modid);
    }

    public static Items createItems(String modid) {
        return new Items(modid);
    }

    protected ResourceKey<R> key(String name) {
        return ResourceKey.create(registryKey, ResourceLocation.fromNamespaceAndPath(modid, name));
    }

    protected <H extends DeferredHolder<R, ? extends R>> H add(H holder) {
        if (registered) throw new IllegalStateException("Register " + registryKey.location() + " already done");
        entries.add(holder);
        return holder;
    }

    public <T extends R> DeferredHolder<R, T> register(String name, Supplier<? extends T> factory) {
        return add(new DeferredHolder<>(key(name), factory));
    }

    public Collection<DeferredHolder<R, ? extends R>> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Rejestruje wszystkie wpisy w rejestrze gry. */
    @SuppressWarnings("unchecked")
    public void register() {
        if (registered) return;
        registered = true;
        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.get(registryKey.location());
        if (registry == null) throw new IllegalStateException("Unknown registry " + registryKey.location());
        for (DeferredHolder<R, ? extends R> holder : entries) {
            bind(registry, holder);
        }
    }

    private static <R, T extends R> void bind(Registry<R> registry, DeferredHolder<R, T> holder) {
        T value = holder.factory.get();
        Registry.register(registry, holder.getKey(), value);
        holder.value = value;
    }

    /** Bloki: {@link #register} zwraca {@link DeferredBlock}. */
    public static class Blocks extends DeferredRegister<Block> {
        protected Blocks(String modid) {
            super(Registries.BLOCK, modid);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> factory) {
            return add(new DeferredBlock<>(key(name), factory));
        }
    }

    /** Przedmioty: {@link #register} zwraca {@link DeferredItem}. */
    public static class Items extends DeferredRegister<Item> {
        protected Items(String modid) {
            super(Registries.ITEM, modid);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> factory) {
            return add(new DeferredItem<>(key(name), factory));
        }

        public DeferredItem<Item> registerSimpleItem(String name) {
            return register(name, () -> new Item(new Item.Properties()));
        }

        public DeferredItem<Item> registerSimpleItem(String name, Item.Properties properties) {
            return register(name, () -> new Item(properties));
        }
    }
}
