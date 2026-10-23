package pl.geesio.redcircuit.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import pl.geesio.redcircuit.tube.TubeBlockEntity;

import java.util.function.Supplier;

/**
 * Deployer: 9 slotów; na impuls redstone używa przedmiotu na bloku przed sobą jak gracz PPM
 * (stawia bloki, sypie mączkę kostną, napełnia wiadra, strzyże...). Sloty po kolei.
 */
public class DeployerBlock extends ContainerMachineBlock {
    public DeployerBlock(Properties properties, Supplier<BlockEntityType<MachineBlockEntity>> type) {
        super(properties, type, 1, "container.redcircuit.deployer");
    }

    @Override
    protected void pulse(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing);
        FakePlayer player = FakePlayerFactory.getMinecraft(sl);
        Vec3 eye = Vec3.atCenterOf(pos);
        float yRot = facing.getAxis().isHorizontal() ? facing.toYRot() : 0f;
        float xRot = facing == Direction.UP ? -90f : facing == Direction.DOWN ? 90f : 0f;
        player.moveTo(eye.x, eye.y - player.getEyeHeight(), eye.z, yRot, xRot);
        player.setYHeadRot(yRot);

        int n = be.items.getContainerSize();
        for (int k = 0; k < n; k++) {
            int slot = (be.next + k) % n;
            ItemStack stack = be.items.getItem(slot);
            if (stack.isEmpty()) continue;
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            boolean done = use(sl, player, front, facing, stack);
            be.items.setItem(slot, player.getMainHandItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            // To, co trafiło do ekwipunku sztucznego gracza (np. pełne wiadro), wraca do maszyny
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack extra = player.getInventory().removeItemNoUpdate(i);
                if (extra.isEmpty()) continue;
                ItemStack rest = MachineUtil.insert(be.items, extra);
                if (!rest.isEmpty()) TubeBlockEntity.spawn(level, pos.relative(facing.getOpposite()), rest, null);
            }
            if (done) {
                be.next = (slot + 1) % n;
                be.setChanged();
                return;
            }
        }
    }

    private static boolean use(ServerLevel level, FakePlayer player, BlockPos front, Direction facing, ItemStack stack) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(front).relative(facing.getOpposite(), 0.5),
                facing.getOpposite(), front, false);
        BlockState fs = level.getBlockState(front);
        if (!fs.isAir()) {
            ItemInteractionResult r = fs.useItemOn(stack, level, player, InteractionHand.MAIN_HAND, hit);
            if (r.consumesAction()) return true;
        }
        if (stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()) return true;
        InteractionResultHolder<ItemStack> r = stack.use(level, player, InteractionHand.MAIN_HAND);
        if (r.getResult().consumesAction()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, r.getObject());
            return true;
        }
        return false;
    }
}
