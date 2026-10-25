package pl.geesio.redcircuit.craft;

import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModItems;

/**
 * Stół kreślarski: trwała siatka 3x3 (przedmioty zostają po zamknięciu),
 * 18 slotów magazynu, z którego najpierw pobierane są składniki, oraz slot na plan.
 */
public class BlueprintTableBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler grid = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public final ItemStackHandler storage = new ItemStackHandler(18) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public final ItemStackHandler plan = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.BLANK_PLAN.get()) || stack.is(ModItems.PLAN.get());
        }
    };

    public BlueprintTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLUEPRINT_TABLE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.redcircuit.blueprint_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BlueprintTableMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        grid.save(output.child("Grid"));
        storage.save(output.child("Storage"));
        plan.save(output.child("Plan"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        grid.load(input.childOrEmpty("Grid"));
        storage.load(input.childOrEmpty("Storage"));
        plan.load(input.childOrEmpty("Plan"));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (var handler : java.util.List.of(this.grid, this.storage, this.plan)) {
            for (int i = 0; i < handler.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
            }
        }
    }
}
