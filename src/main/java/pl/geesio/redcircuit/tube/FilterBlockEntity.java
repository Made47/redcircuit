package pl.geesio.redcircuit.tube;

import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/** 9 slotów wzorców. Pusty filtr przepuszcza wszystko. */
public class FilterBlockEntity extends BlockEntity {
    public final MachineContainer filter = new MachineContainer(this, 9);

    public FilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER.get(), pos, state);
    }

    public boolean matches(ItemStack stack) {
        boolean any = false;
        for (int i = 0; i < filter.getContainerSize(); i++) {
            ItemStack pattern = filter.getItem(i);
            if (pattern.isEmpty()) continue;
            any = true;
            if (ItemStack.isSameItem(pattern, stack)) return true;
        }
        return !any;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filter.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter.load(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
            Containers.dropContents(level, pos, this.filter);
    }
}
