package pl.geesio.redcircuit.machine;

import net.minecraft.block.material.Material;
import net.minecraft.world.World;

/** Wyrzutnik: 9 slotów; na impuls redstone wypycha przodem jeden stos (sloty po kolei). */
public class BlockEjector extends BlockContainerMachine {
    public BlockEjector() {
        super("ejector", Material.iron, 1);
    }

    @Override
    protected void pulse(World world, int x, int y, int z, TileMachine te) {
        int slot = MachineUtil.pushOne(world, x, y, z, facing(world, x, y, z), te.items, te.next);
        if (slot >= 0) {
            te.next = (slot + 1) % te.items.getSizeInventory();
            te.markDirty();
        }
    }
}
