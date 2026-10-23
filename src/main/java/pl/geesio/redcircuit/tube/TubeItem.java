package pl.geesio.redcircuit.tube;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Pos;

/** Przedmiot podróżujący wewnątrz rury. */
public class TubeItem {
    public ItemStack stack;
    /** Strona rury, przez którą przedmiot wszedł (kierunek od środka do tej ściany). */
    public ForgeDirection from;
    /** Strona, którą wyjdzie; null dopóki nie dotrze do środka i nie zostanie wyznaczona trasa. */
    public ForgeDirection to;
    /** 0 = przy ścianie wejściowej, 0.5 = środek, 1 = ściana wyjściowa. */
    public float progress;
    /** Kolor przedmiotu (0..15) lub -1 dla bezbarwnego. */
    public int color;
    /** Prędkość w blokach na tick (Akcelerator ją zwiększa, potem stopniowo spada). */
    public float speed = TileTube.SPEED;
    /** Cel podróży (zamówienie Requestera) - null = najbliższy inwentarz. */
    public Pos target;

    public TubeItem(ItemStack stack, ForgeDirection from, int color) {
        this.stack = stack;
        this.from = from;
        this.color = color;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Item", stack.writeToNBT(new NBTTagCompound()));
        tag.setByte("From", (byte) from.ordinal());
        tag.setByte("To", (byte) (to == null ? -1 : to.ordinal()));
        tag.setFloat("Progress", progress);
        tag.setByte("Color", (byte) color);
        tag.setFloat("Speed", speed);
        if (target != null) tag.setIntArray("Target", new int[]{target.x, target.y, target.z});
        return tag;
    }

    public static TubeItem load(NBTTagCompound tag) {
        ItemStack stack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Item"));
        if (stack == null) return null;
        TubeItem item = new TubeItem(stack, ForgeDirection.getOrientation(tag.getByte("From")), tag.getByte("Color"));
        int to = tag.getByte("To");
        item.to = to < 0 ? null : ForgeDirection.getOrientation(to);
        item.progress = tag.getFloat("Progress");
        if (tag.hasKey("Speed")) item.speed = tag.getFloat("Speed");
        int[] t = tag.getIntArray("Target");
        if (t.length == 3) item.target = new Pos(t[0], t[1], t[2]);
        return item;
    }
}
