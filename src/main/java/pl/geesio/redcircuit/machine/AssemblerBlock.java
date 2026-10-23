package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.tube.TubeBlockEntity;

import java.util.function.Supplier;

/**
 * Assembler: 16 slotów sterowanych kablem wiązkowym. Włączenie kanału N stawia przed maszyną
 * blok ze slotu N, wyłączenie - zbiera ten blok z powrotem do slotu N.
 * Zwykły impuls redstone: stawia blok z kolejnego slotu albo zbiera blok stojący przed maszyną.
 */
public class AssemblerBlock extends ContainerMachineBlock {
    public AssemblerBlock(Properties properties, Supplier<BlockEntityType<MachineBlockEntity>> type) {
        super(properties, type, 2, "container.redcircuit.assembler");
    }

    @Override
    protected boolean ticks() {
        return true;
    }

    @Override
    protected void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        int mask = MachineUtil.bundledInput(level, pos);
        int changed = mask ^ be.lastChannels;
        if (changed == 0) return;
        be.lastChannels = mask;
        be.setChanged();
        for (int ch = 0; ch < 16; ch++) {
            if ((changed & (1 << ch)) == 0) continue;
            if ((mask & (1 << ch)) != 0) place(level, pos, state, be, ch);
            else collect(level, pos, state, be, ch);
        }
    }

    @Override
    protected void pulse(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        BlockPos front = pos.relative(state.getValue(FACING));
        if (level.getBlockState(front).canBeReplaced()) {
            for (int k = 0; k < 16; k++) {
                int slot = (be.next + k) % 16;
                if (place(level, pos, state, be, slot)) {
                    be.next = (slot + 1) % 16;
                    return;
                }
            }
        } else {
            collect(level, pos, state, be, -1);
        }
    }

    private static boolean place(Level level, BlockPos pos, BlockState state, MachineBlockEntity be, int slot) {
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing);
        ItemStack stack = be.items.getItem(slot);
        if (!(stack.getItem() instanceof BlockItem bi) || !level.getBlockState(front).canBeReplaced()) return false;
        boolean ok = bi.place(new DirectionalPlaceContext(level, front, facing, stack, facing)).consumesAction();
        if (ok) be.items.setChanged();
        return ok;
    }

    /** Zbiera blok sprzed maszyny; najpierw do slotu {@code slot} (jeśli >= 0), potem do pozostałych. */
    private static void collect(Level level, BlockPos pos, BlockState state, MachineBlockEntity be, int slot) {
        if (!(level instanceof ServerLevel sl)) return;
        Direction facing = state.getValue(FACING);
        for (ItemStack drop : MachineUtil.breakBlock(sl, pos.relative(facing))) {
            if (slot >= 0) {
                ItemStack cur = be.items.getItem(slot);
                if (cur.isEmpty()) {
                    be.items.setItem(slot, drop);
                    continue;
                }
                if (ItemStack.isSameItemSameComponents(cur, drop) && cur.getCount() + drop.getCount() <= cur.getMaxStackSize()) {
                    cur.grow(drop.getCount());
                    be.items.setChanged();
                    continue;
                }
            }
            ItemStack rest = MachineUtil.insert(be.items, drop);
            if (!rest.isEmpty()) TubeBlockEntity.spawn(level, pos.relative(facing.getOpposite()), rest, null);
        }
    }
}
