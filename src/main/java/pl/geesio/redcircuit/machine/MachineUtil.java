package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.tube.ExtractorBlock;
import pl.geesio.redcircuit.wire.BundledCableBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class MachineUtil {
    private MachineUtil() {}

    /** Wypycha jeden stos (pierwszy niepusty od slotu {@code start}) przodem maszyny. Zwraca indeks slotu albo -1. */
    public static int pushOne(Level level, BlockPos pos, Direction facing, Container items, int start) {
        int n = items.getContainerSize();
        for (int k = 0; k < n; k++) {
            int slot = (start + k) % n;
            ItemStack stack = items.getItem(slot);
            if (stack.isEmpty()) continue;
            int count = ExtractorBlock.deliverableCount(level, pos, facing, stack, -1);
            if (count <= 0) continue;
            ItemStack out = items.removeItem(slot, count);
            ExtractorBlock.deliver(level, pos, facing, out, -1);
            return slot;
        }
        return -1;
    }

    /** Wkłada stos do kontenera; zwraca resztę. */
    public static ItemStack insert(Container items, ItemStack stack) {
        for (int i = 0; i < items.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack cur = items.getItem(i);
            if (cur.isEmpty()) {
                items.setItem(i, stack.copy());
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(cur, stack) && cur.getCount() < cur.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), cur.getMaxStackSize() - cur.getCount());
                cur.grow(move);
                stack.shrink(move);
                items.setChanged();
            }
        }
        return stack;
    }

    /** Czy blok przed maszyną da się rozbić (nie powietrze, nie płyn, nie bedrock). */
    public static boolean breakable(Level level, BlockPos target) {
        BlockState s = level.getBlockState(target);
        return !s.isAir() && s.getFluidState().isEmpty() && s.getDestroySpeed(level, target) >= 0;
    }

    /** Rozbija blok jak diamentowy kilof i zwraca jego dropy. */
    public static List<ItemStack> breakBlock(ServerLevel level, BlockPos target) {
        BlockState s = level.getBlockState(target);
        if (!breakable(level, target)) return List.of();
        BlockEntity be = level.getBlockEntity(target);
        List<ItemStack> drops = new ArrayList<>(net.minecraft.world.level.block.Block.getDrops(
                s, level, target, be, null, new ItemStack(Items.DIAMOND_PICKAXE)));
        level.destroyBlock(target, false);
        return drops;
    }

    /** Odczyt 16 kanałów z kabli wiązkowych (przyklejonych lub w obudowie) dookoła - maska bitowa. */
    public static int bundledInput(Level level, BlockPos pos) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(pos.relative(d)) instanceof BundledCableBlockEntity cable) {
                for (int ch = 0; ch < 16; ch++) if (cable.getChannel(ch) > 0) mask |= 1 << ch;
            }
        }
        return mask;
    }
}
