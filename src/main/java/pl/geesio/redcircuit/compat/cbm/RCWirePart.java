package pl.geesio.redcircuit.compat.cbm;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.api.part.BaseMultipart;
import codechicken.multipart.api.part.FacePart;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.api.part.NormalOcclusionPart;
import codechicken.multipart.block.TileMultipart;
import codechicken.multipart.util.PartRayTraceResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.WireConnections;
import pl.geesio.redcircuit.wire.WireLinks;
import pl.geesio.redcircuit.wire.WireNetwork;
import pl.geesio.redcircuit.wire.WireTypes;

import java.util.List;

/** Wspólna baza przewodów-części: leży na jednej ścianie bloku (slot = ta ściana). */
public abstract class RCWirePart extends BaseMultipart implements FacePart, NormalOcclusionPart {
    /** Ściana bloku, na której leży przewód (Direction 3D data value). */
    protected int face;

    public int face() {
        return face;
    }

    /** Stan bloku-przewodu z jedną ścianą - używany do renderowania modelu części. */
    public BlockState renderState() {
        BlockState st = hostBlock().defaultBlockState();
        for (Direction d : Direction.values()) st = st.setValue(FaceWireBlock.face(d), d.get3DDataValue() == face);
        return st;
    }

    public Direction side() {
        return Direction.from3DDataValue(face);
    }

    public RCWirePart setFace(Direction side) {
        this.face = side.get3DDataValue();
        return this;
    }

    /** Blok-przewód tego samego rodzaju (wymiary, model, drop). */
    public abstract FaceWireBlock hostBlock();

    public abstract int wireType();

    /** Czy łączy się z urządzeniem w sąsiednim bloku. */
    protected boolean device(BlockGetter level, BlockPos np, BlockState ns, Direction dir) {
        return false;
    }

    // ------------------------------------------------------------------ połączenia

    public WireLinks.Target target(BlockGetter level, Direction dir) {
        BlockPos pos = pos();
        int type = wireType();
        return WireLinks.link(level, pos, type, side(), dir, f -> {
            Integer o = CbmWireHost.typeAt(level, pos, f);
            return o != null && WireTypes.compatible(type, o);
        }, true, (lvl, np, ns) -> device(lvl, np, ns, dir));
    }

    public WireConnections connections(BlockGetter level) {
        WireConnections.Builder b = new WireConnections.Builder();
        Direction side = side();
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == side.getAxis()) continue;
            b.set(side, dir, WireLinks.reach(target(level, dir).link(), side, dir));
        }
        return b.build();
    }

    // ------------------------------------------------------------------ zapis / sync

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putByte("Face", (byte) face);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        face = tag.getByte("Face");
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        packet.writeByte(face);
    }

    @Override
    public void readDesc(MCDataInput packet) {
        face = packet.readByte();
    }

    // ------------------------------------------------------------------ kształt / sloty

    @Override
    public int getSlotMask() {
        return 1 << face;
    }

    @Override
    public VoxelShape getOcclusionShape() {
        return hostBlock().faceShape(side(), WireConnections.EMPTY);
    }

    @Override
    public VoxelShape getShape(CollisionContext context) {
        return hasLevel() ? hostBlock().faceShape(side(), connections(level())) : getOcclusionShape();
    }

    @Override
    public VoxelShape getCollisionShape(CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getBlockSupportShape() {
        return Shapes.empty();
    }

    @Override
    public int redstoneConductionMap() {
        return 0xF;
    }

    // ------------------------------------------------------------------ drop / dźwięk

    @Override
    public Iterable<ItemStack> getDrops() {
        return List.of(new ItemStack(hostBlock()));
    }

    @Override
    public ItemStack getCloneStack(PartRayTraceResult hit, Player player) {
        return new ItemStack(hostBlock());
    }

    @Override
    public float getStrength(Player player, PartRayTraceResult hit) {
        return 0.25f;
    }

    @Override
    public SoundType getSound(@Nullable UseOnContext context) {
        return SoundType.METAL;
    }

    // ------------------------------------------------------------------ zdarzenia

    private boolean canStay() {
        return FaceWireBlock.canAttach(level(), pos(), side());
    }

    @Override
    public void onAdded() {
        if (!level().isClientSide) WireNetwork.onWireChanged(level(), pos(), hostBlock());
    }

    @Override
    public void onRemoved() {
        if (!level().isClientSide) {
            WireNetwork.onWireRemoved(level(), pos(), hostBlock());
            WireNetwork.update(level(), pos());   // pozostałe przewody w tym samym bloku
        }
    }

    @Override
    public void onNeighborBlockChanged(BlockPos neighbor) {
        if (level().isClientSide) {
            tile().markRender();
            return;
        }
        if (!canStay()) {
            TileMultipart.dropItem(new ItemStack(hostBlock()), level(), Vector3.fromTileCenter(tile()));
            tile().remPart(this);
            return;
        }
        WireNetwork.update(level(), pos());
    }

    @Override
    public void onPartChanged(@Nullable MultiPart part) {
        if (level().isClientSide) {
            tile().markRender();
        } else if (part != this) {
            WireNetwork.update(level(), pos());
        }
    }

    /** Wywoływane po zmianie stanu (sygnału): odśwież sąsiadów. */
    public void notifyOutput() {
        tile().notifyPartChange(this);
    }
}
