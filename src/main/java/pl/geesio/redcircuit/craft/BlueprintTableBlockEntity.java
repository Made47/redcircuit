package pl.geesio.redcircuit.craft;

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
import net.neoforged.neoforge.items.ItemStackHandler;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Grid", grid.serializeNBT(registries));
        tag.put("Storage", storage.serializeNBT(registries));
        tag.put("Plan", plan.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        grid.deserializeNBT(registries, tag.getCompound("Grid"));
        storage.deserializeNBT(registries, tag.getCompound("Storage"));
        plan.deserializeNBT(registries, tag.getCompound("Plan"));
    }
}
