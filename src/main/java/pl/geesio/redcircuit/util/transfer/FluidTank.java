package pl.geesio.redcircuit.util.transfer;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.function.Predicate;

/** Jednozbiornikowy magazyn płynu (transakcyjny). Nadpisuj {@link #onContentsChanged()}. */
public class FluidTank extends FluidStacksResourceHandler implements IFluidHandler {
    private final Predicate<FluidStack> validator;

    public FluidTank(int capacity) {
        this(capacity, fs -> true);
    }

    public FluidTank(int capacity, Predicate<FluidStack> validator) {
        super(1, capacity);
        this.validator = validator;
    }

    protected void onContentsChanged() {}

    @Override
    protected final void onContentsChanged(int index, FluidStack previousContents) {
        onContentsChanged();
    }

    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.isEmpty() || isFluidValid(resource.toStack(1));
    }

    public FluidStack getFluid() {
        return stacks.get(0);
    }

    public int getFluidAmount() {
        return stacks.get(0).getAmount();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getSpace() {
        return Math.max(0, capacity - getFluidAmount());
    }

    public boolean isEmpty() {
        return stacks.get(0).isEmpty();
    }

    public void setFluid(FluidStack stack) {
        FluidStack old = stacks.set(0, stack);
        onContentsChanged(0, old);
    }

    public void save(ValueOutput output) {
        serialize(output);
    }

    public void load(ValueInput input) {
        if (input.list(VALUE_IO_KEY, FluidStack.OPTIONAL_CODEC).isEmpty()) {
            // zapis z wersji 1.21.1 (FluidTank NeoForge): {Fluid: {id, amount}}
            setFluid(input.read("Fluid", FluidStack.CODEC).orElse(FluidStack.EMPTY));
            return;
        }
        deserialize(input);
    }
}
