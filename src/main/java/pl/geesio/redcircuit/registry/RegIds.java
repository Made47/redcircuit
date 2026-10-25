package pl.geesio.redcircuit.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Od 1.21.2 właściwości bloków i przedmiotów muszą znać swój identyfikator ({@code setId}) już przy tworzeniu.
 * {@link #block(Supplier)} / {@link #item(Supplier)} ustawiają bieżący identyfikator na czas tworzenia obiektu,
 * a {@link #blockProps()} / {@link #itemProps()} od razu go wpisują.
 */
public final class RegIds {
    private static final ThreadLocal<ResourceKey<Block>> BLOCK = new ThreadLocal<>();
    private static final ThreadLocal<ResourceKey<Item>> ITEM = new ThreadLocal<>();

    private RegIds() {}

    public static <B extends Block> Function<Identifier, B> block(Supplier<B> factory) {
        return id -> {
            BLOCK.set(ResourceKey.create(Registries.BLOCK, id));
            try {
                return factory.get();
            } finally {
                BLOCK.remove();
            }
        };
    }

    public static <I extends Item> Function<Identifier, I> item(Supplier<I> factory) {
        return id -> {
            ITEM.set(ResourceKey.create(Registries.ITEM, id));
            try {
                return factory.get();
            } finally {
                ITEM.remove();
            }
        };
    }

    public static BlockBehaviour.Properties blockProps() {
        ResourceKey<Block> key = BLOCK.get();
        if (key == null) throw new IllegalStateException("blockProps() poza RegIds.block(...)");
        return BlockBehaviour.Properties.of().setId(key);
    }

    public static Item.Properties itemProps() {
        ResourceKey<Item> key = ITEM.get();
        if (key == null) throw new IllegalStateException("itemProps() poza RegIds.item(...)");
        return new Item.Properties().setId(key);
    }
}
