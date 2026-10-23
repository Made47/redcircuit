package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemHandlerHelper;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.tube.MachineContainer;
import pl.geesio.redcircuit.tube.ExtractorBlock;
import pl.geesio.redcircuit.tube.TubeBlockEntity;

/**
 * Regulator: przenosi przedmioty z inwentarza z tyłu do inwentarza z przodu, aż ten będzie
 * zawierał ilości z filtra. Gdy wszystko jest uzupełnione, wysyła sygnał redstone.
 */
public class RegulatorBlockEntity extends BlockEntity {
    public static final int INTERVAL = 10;

    public final MachineContainer filter = new MachineContainer(this, 9);
    private int timer;

    public RegulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REGULATOR.get(), pos, state);
    }

    private static int count(IItemHandler h, ItemStack pattern) {
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(s, pattern)) n += s.getCount();
        }
        return n;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RegulatorBlockEntity be) {
        if (level.isClientSide || ++be.timer < INTERVAL) return;
        be.timer = 0;
        Direction facing = state.getValue(ExtractorBlock.FACING);
        BlockPos front = pos.relative(facing), back = pos.relative(facing.getOpposite());
        IItemHandler src = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.ItemHandler.BLOCK, back, facing);
        IItemHandler dst = level.getBlockEntity(front) instanceof TubeBlockEntity ? null
                : pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.ItemHandler.BLOCK, front, facing.getOpposite());

        boolean satisfied = dst != null && !be.filter.isEmpty();
        if (dst != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack p = be.filter.getItem(i);
                if (p.isEmpty()) continue;
                int need = p.getCount() - count(dst, p);
                if (need > 0 && src != null) {
                    for (int s = 0; s < src.getSlots() && need > 0; s++) {
                        ItemStack sim = src.extractItem(s, need, true);
                        if (sim.isEmpty() || !ItemStack.isSameItemSameComponents(p, sim)) continue;
                        int fits = sim.getCount() - ItemHandlerHelper.insertItem(dst, sim, true).getCount();
                        if (fits <= 0) continue;
                        ItemStack moved = src.extractItem(s, fits, false);
                        ItemStack rest = ItemHandlerHelper.insertItem(dst, moved, false);
                        if (!rest.isEmpty()) ItemHandlerHelper.insertItem(src, rest, false);
                        need -= moved.getCount() - rest.getCount();
                    }
                }
                if (need > 0) satisfied = false;
            }
        }
        if (state.getValue(RegulatorBlock.SATISFIED) != satisfied) {
            level.setBlock(pos, state.setValue(RegulatorBlock.SATISFIED, satisfied), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        filter.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filter.load(tag, registries);
    }
}
