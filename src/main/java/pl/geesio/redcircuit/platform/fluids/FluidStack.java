package pl.geesio.redcircuit.platform.fluids;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Odpowiednik FluidStack z NeoForge: płyn + ilość w mB (1000 = wiadro).
 * Na granicy z Fabric Transfer API przeliczane na droplety (1 mB = 81 dropletów).
 */
public final class FluidStack {
    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);
    /** Droplety (Fabric) na 1 mB. */
    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("id").forGetter(FluidStack::getFluid),
            Codec.INT.fieldOf("amount").forGetter(FluidStack::getAmount)
    ).apply(i, FluidStack::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(net.minecraft.core.registries.Registries.FLUID), FluidStack::getFluid,
            ByteBufCodecs.VAR_INT, FluidStack::getAmount,
            FluidStack::new);

    private final Fluid fluid;
    private int amount;

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid == null ? Fluids.EMPTY : fluid;
        this.amount = amount;
    }

    public static FluidStack of(FluidVariant variant, long droplets) {
        if (variant == null || variant.isBlank() || droplets <= 0) return EMPTY;
        return new FluidStack(variant.getFluid(), (int) Math.min(Integer.MAX_VALUE, droplets / DROPLETS_PER_MB));
    }

    public FluidVariant toVariant() {
        return isEmpty() ? FluidVariant.blank() : FluidVariant.of(fluid);
    }

    public long dropletAmount() {
        return (long) amount * DROPLETS_PER_MB;
    }

    public boolean isEmpty() {
        return this == EMPTY || fluid == Fluids.EMPTY || amount <= 0;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid;
    }

    public FluidType getFluidType() {
        return new FluidType(getFluid());
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public void setAmount(int amount) {
        if (this == EMPTY) throw new IllegalStateException("Can't modify the empty stack");
        this.amount = amount;
    }

    public void grow(int amount) {
        setAmount(this.amount + amount);
    }

    public void shrink(int amount) {
        setAmount(this.amount - amount);
    }

    public FluidStack copy() {
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount);
    }

    public FluidStack copyWithAmount(int amount) {
        return isEmpty() || amount <= 0 ? EMPTY : new FluidStack(fluid, amount);
    }

    public Component getHoverName() {
        return FluidVariantAttributes.getName(toVariant());
    }

    public boolean is(Fluid other) {
        return getFluid().isSame(other);
    }

    public static boolean isSameFluid(FluidStack a, FluidStack b) {
        return a.getFluid() == b.getFluid();
    }

    public static boolean isSameFluidSameComponents(FluidStack a, FluidStack b) {
        return isSameFluid(a, b);
    }

    public static boolean matches(FluidStack a, FluidStack b) {
        return isSameFluid(a, b) && a.getAmount() == b.getAmount();
    }

    public Tag save(HolderLookup.Provider provider) {
        return CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    public static FluidStack parseOptional(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.isEmpty()) return EMPTY;
        return CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(EMPTY);
    }

    @Override
    public String toString() {
        return amount + " mB " + BuiltInRegistries.FLUID.getKey(getFluid());
    }
}
