package pl.geesio.redcircuit.tube;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    public void save(ValueOutput tag) {
        tag.store("Item", ItemStack.CODEC, stack);
        tag.putByte("From", (byte) from.get3DDataValue());
        tag.putByte("To", (byte) (to == null ? -1 : to.get3DDataValue()));
        tag.putFloat("Progress", progress);
        tag.putByte("Color", (byte) color);
        tag.putFloat("Speed", speed);
        if (target != null) tag.putLong("Target", target.asLong());
    }

    @Nullable
    public static TubeItem load(ValueInput tag) {
        ItemStack stack = tag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return null;
        TubeItem item = new TubeItem(stack, Direction.from3DDataValue(tag.getByteOr("From", (byte) 0)), tag.getByteOr("Color", (byte) -1));
        int to = tag.getByteOr("To", (byte) -1);
        item.to = to < 0 ? null : Direction.from3DDataValue(to);
        item.progress = tag.getFloatOr("Progress", 0f);
        item.speed = tag.getFloatOr("Speed", item.speed);
        tag.getLong("Target").ifPresent(t -> item.target = net.minecraft.core.BlockPos.of(t));
        return item;
    }
}
