package pl.geesio.redcircuit.tube;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

public class TubeBlockEntity extends BlockEntity {
    /** Prędkość: 1/8 bloku na tick. */
    public static final float SPEED = 0.125f;
    public static final int MAX_ITEMS = 32;

    private final List<TubeItem> items = new ArrayList<>();

    /** Spadek prędkości przyspieszonego przedmiotu na każdym bloku rury. */
    public static final float SPEED_DECAY = 0.02f;

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TUBE.get(), pos, state);
    }

    protected TubeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Wywoływane, gdy przedmiot wjeżdża do tej rury (Akcelerator przyspiesza). */
    protected void onItemEnter(TubeItem item) {
    }

    public List<TubeItem> getItems() {
        return items;
    }

    public boolean canAccept(int color) {
        return items.size() < MAX_ITEMS && TubeBlock.passable(getBlockState(), color);
    }

    /** Wkłada przedmiot do rury od strony {@code from}. */
    public void insert(ItemStack stack, Direction from, int color) {
        if (stack.isEmpty()) return;
        TubeItem item = new TubeItem(stack, from, color);
        onItemEnter(item);
        items.add(item);
        sync();
    }

    /** Wkłada przedmiot zamówiony przez Requester - pojedzie do {@code target}. */
    public void insertTargeted(ItemStack stack, Direction from, BlockPos target) {
        if (stack.isEmpty()) return;
        TubeItem item = new TubeItem(stack, from, -1);
        item.target = target.immutable();
        onItemEnter(item);
        items.add(item);
        sync();
    }

    public IItemHandler getHandler(@Nullable Direction side) {
        return new InsertHandler(side == null ? Direction.UP : side);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Level level, BlockPos pos, BlockState state, TubeBlockEntity be) {
        if (level.isClientSide) {
            be.clientTick();
        } else {
            be.serverTick(level, pos);
        }
    }

    private void clientTick() {
        for (TubeItem item : items) {
            float limit = item.to == null ? 0.5f : 1.0f;
            item.progress = Math.min(limit, item.progress + item.speed);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        if (items.isEmpty()) return;
        boolean dirty = false;
        for (TubeItem item : new ArrayList<>(items)) {
            item.progress += item.speed;

            if (item.to == null && item.progress >= 0.5f) {
                item.to = TubeRouter.route(level, pos, item);
                dirty = true;
                if (item.to == null) {
                    // Nie ma dokąd iść - wyrzuć przedmiot.
                    items.remove(item);
                    spawn(level, pos, item.stack, null);
                    continue;
                }
            }

            if (item.progress >= 1.0f && item.to != null) {
                items.remove(item);
                dirty = true;
                if (!pass(level, pos, item)) {
                    // Odbij i wyznacz trasę ponownie.
                    item.from = item.to;
                    item.to = null;
                    item.progress = 0f;
                    items.add(item);
                }
            }
        }
        if (dirty) sync();
    }

    /** Próbuje przekazać przedmiot dalej. Zwraca true, jeśli przedmiot opuścił rurę. */
    private boolean pass(Level level, BlockPos pos, TubeItem item) {
        Direction out = item.to;
        BlockPos np = pos.relative(out);
        if (!level.isLoaded(np)) return false;

        if (level.getBlockEntity(np) instanceof TubeBlockEntity next) {
            if (!next.canAccept(item.color)) return false;
            item.from = out.getOpposite();
            item.to = null;
            item.progress = 0f;
            item.speed = Math.max(SPEED, item.speed - SPEED_DECAY);
            next.onItemEnter(item);
            next.items.add(item);
            next.sync();
            return true;
        }

        IItemHandler handler = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.ItemHandler.BLOCK, np, out.getOpposite());
        if (handler != null) {
            ItemStack rest = ItemHandlerHelper.insertItem(handler, item.stack, false);
            if (rest.isEmpty()) return true;
            item.stack = rest;
            return false;
        }

        // Otwarty koniec rury - wyrzuć przedmiot na zewnątrz.
        if (level.getBlockState(np).getCollisionShape(level, np).isEmpty()) {
            spawn(level, np, item.stack, out);
            return true;
        }
        return false;
    }

    public static void spawn(Level level, BlockPos pos, ItemStack stack, @Nullable Direction motion) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        if (motion != null) {
            entity.setDeltaMovement(motion.getStepX() * 0.15, motion.getStepY() * 0.15, motion.getStepZ() * 0.15);
        } else {
            entity.setDeltaMovement(0, 0, 0);
        }
        level.addFreshEntity(entity);
    }

    // ------------------------------------------------------------------ sync / save

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void writeItems(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TubeItem item : items) {
            if (!item.stack.isEmpty()) list.add(item.save(registries));
        }
        tag.put("Items", list);
    }

    private void readItems(CompoundTag tag, HolderLookup.Provider registries) {
        items.clear();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TubeItem item = TubeItem.load(list.getCompound(i), registries);
            if (item != null) items.add(item);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeItems(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readItems(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeItems(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------ capability

    /** Pozwala lejkom i innym modom wkładać przedmioty do rury. */
    private class InsertHandler implements IItemHandler {
        private final Direction side;

        InsertHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !canAccept(-1)) return stack;
            if (!simulate) insert(stack.copy(), side, -1);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }
}
