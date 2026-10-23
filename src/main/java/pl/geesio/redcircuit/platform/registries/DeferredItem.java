package pl.geesio.redcircuit.platform.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

public class DeferredItem<I extends Item> extends DeferredHolder<Item, I> implements ItemLike {
    DeferredItem(ResourceKey<Item> key, Supplier<? extends I> factory) {
        super(key, factory);
    }

    @Override
    public Item asItem() {
        return get();
    }

    public ItemStack toStack() {
        return new ItemStack(get());
    }

    public ItemStack toStack(int count) {
        return new ItemStack(get(), count);
    }
}
