package pl.geesio.redcircuit.platform.fluids;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Niezmienna zawartość płynu w komponencie danych przedmiotu (jak SimpleFluidContent z NeoForge). */
public final class SimpleFluidContent {
    public static final SimpleFluidContent EMPTY = new SimpleFluidContent(FluidStack.EMPTY);

    public static final Codec<SimpleFluidContent> CODEC = FluidStack.CODEC.xmap(SimpleFluidContent::new, c -> c.fluid);
    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleFluidContent> STREAM_CODEC =
            FluidStack.STREAM_CODEC.map(SimpleFluidContent::new, c -> c.fluid);

    private final FluidStack fluid;

    private SimpleFluidContent(FluidStack fluid) {
        this.fluid = fluid;
    }

    public static SimpleFluidContent copyOf(FluidStack fluid) {
        return fluid.isEmpty() ? EMPTY : new SimpleFluidContent(fluid.copy());
    }

    public FluidStack copy() {
        return fluid.copy();
    }

    public boolean isEmpty() {
        return fluid.isEmpty();
    }

    public int getAmount() {
        return fluid.getAmount();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SimpleFluidContent other && FluidStack.matches(fluid, other.fluid);
    }

    @Override
    public int hashCode() {
        return fluid.getFluid().hashCode() * 31 + fluid.getAmount();
    }
}
