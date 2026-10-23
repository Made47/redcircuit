package pl.geesio.redcircuit.machine;

import net.minecraft.block.material.Material;
import net.minecraft.world.World;

/** Przekaźnik: 9 slotów, przyjmuje przedmioty z każdej strony i sam wypycha je przodem (co 4 ticki). */
public class BlockRelay extends BlockContainerMachine {
    public BlockRelay() {
        super("relay", Material.iron, 1);
    }

    @Override
    protected boolean ticks() {
        return true;
    }

    @Override
    protected void serverTick(World world, int x, int y, int z, TileMachine te) {
        if (++te.timer < 4) return;
        te.timer = 0;
        MachineUtil.pushOne(world, x, y, z, facing(world, x, y, z), te.items, 0);
    }

    @Override
    protected void pulse(World world, int x, int y, int z, TileMachine te) {
    }
}
