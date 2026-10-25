package pl.geesio.redcircuit.platform.capabilities;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Odpowiednik ItemCapability z NeoForge (tylko dostawcy zarejestrowani przez RedCircuit). */
public final class ItemCapability<T, C> {
    @FunctionalInterface
    public interface Provider<T, C> {
        @Nullable
        T getCapability(ItemStack stack, C context);
    }

    private final Identifier name;
    private final Map<Item, Provider<T, C>> providers = new HashMap<>();

    ItemCapability(Identifier name) {
        this.name = name;
    }

    public Identifier name() {
        return name;
    }

    void addProvider(Item item, Provider<T, C> provider) {
        providers.put(item, provider);
    }

    @Nullable
    public T getCapability(ItemStack stack, C context) {
        if (stack.isEmpty()) return null;
        Provider<T, C> p = providers.get(stack.getItem());
        return p == null ? null : p.getCapability(stack, context);
    }
}
