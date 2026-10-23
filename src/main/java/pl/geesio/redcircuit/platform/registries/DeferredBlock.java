package pl.geesio.redcircuit.platform.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class DeferredBlock<B extends Block> extends DeferredHolder<Block, B> implements ItemLike {
    DeferredBlock(ResourceKey<Block> key, Supplier<? extends B> factory) {
        super(key, factory);
    }

    @Override
    public Item asItem() {
        return get().asItem();
    }
}
