package pl.geesio.redcircuit.tube;

import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Prosty kontener slotów konfiguracyjnych powiązany z blokiem. */
public class MachineContainer extends SimpleContainer {
    private final BlockEntity owner;

    public MachineContainer(BlockEntity owner, int size) {
        super(size);
        this.owner = owner;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        owner.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(owner, player);
    }

    public void save(ValueOutput output) {
        NonNullList<ItemStack> list = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) list.set(i, getItem(i));
        ContainerHelper.saveAllItems(output, list);
    }

    public void load(ValueInput input) {
        NonNullList<ItemStack> list = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, list);
        for (int i = 0; i < getContainerSize(); i++) super.setItem(i, list.get(i));
    }
}
