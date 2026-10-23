package pl.geesio.redcircuit.tube;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Przedmiot podróżujący wewnątrz rury. */
public class TubeItem {
    public ItemStack stack;
    /** Strona rury, przez którą przedmiot wszedł (kierunek od środka do tej ściany). */
    public Direction from;
    /** Strona, którą wyjdzie; null dopóki nie dotrze do środka i nie zostanie wyznaczona trasa. */
    @Nullable
    public Direction to;
    /** 0 = przy ścianie wejściowej, 0.5 = środek, 1 = ściana wyjściowa. */
    public float progress;
    /** Kolor przedmiotu (0..15) lub -1 dla bezbarwnego. */
    public int color;
    /** Prędkość w blokach na tick (Akcelerator ją zwiększa, potem stopniowo spada). */
    public float speed = TubeBlockEntity.SPEED;
    /** Cel podróży (np. Requester, który zamówił przedmiot) - null = najbliższy inwentarz. */
    @Nullable
    public net.minecraft.core.BlockPos target;

    public TubeItem(ItemStack stack, Direction from, int color) {
        this.stack = stack;
        this.from = from;
        this.color = color;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", stack.save(registries));
        tag.putByte("From", (byte) from.get3DDataValue());
        tag.putByte("To", (byte) (to == null ? -1 : to.get3DDataValue()));
        tag.putFloat("Progress", progress);
        tag.putByte("Color", (byte) color);
        tag.putFloat("Speed", speed);
        if (target != null) tag.putLong("Target", target.asLong());
        return tag;
    }

    @Nullable
    public static TubeItem load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        if (stack.isEmpty()) return null;
        TubeItem item = new TubeItem(stack, Direction.from3DDataValue(tag.getByte("From")), tag.getByte("Color"));
        int to = tag.getByte("To");
        item.to = to < 0 ? null : Direction.from3DDataValue(to);
        item.progress = tag.getFloat("Progress");
        if (tag.contains("Speed")) item.speed = tag.getFloat("Speed");
        if (tag.contains("Target")) item.target = net.minecraft.core.BlockPos.of(tag.getLong("Target"));
        return item;
    }
}
