package pl.geesio.redcircuit.platform.fluids;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.world.level.material.Fluid;

/** Namiastka FluidType z NeoForge - właściwości płynu z Fabric (FluidVariantAttributes). */
public final class FluidType {
    private final Fluid fluid;

    FluidType(Fluid fluid) {
        this.fluid = fluid;
    }

    public int getLightLevel(FluidStack stack) {
        return stack.isEmpty() ? 0 : FluidVariantAttributes.getLuminance(FluidVariant.of(fluid));
    }

    public int getLightLevel() {
        return FluidVariantAttributes.getLuminance(FluidVariant.of(fluid));
    }

    public boolean isLighterThanAir() {
        return FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid));
    }
}
