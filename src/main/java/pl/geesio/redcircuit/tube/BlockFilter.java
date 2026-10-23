package pl.geesio.redcircuit.tube;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.machine.BlockContainerMachine;
import pl.geesio.redcircuit.machine.TileMachine;
import pl.geesio.redcircuit.util.InvUtil;

/** Filtr: jak ekstraktor, ale pobiera tylko przedmioty pasujące do wzorców (PPM otwiera 9 slotów; pusty = wszystko). */
public class BlockFilter extends BlockExtractor {
    public BlockFilter() {
        super("filter");
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileMachine(9, "container.redcircuit.filter", false);
    }

    @Override
    protected boolean accepts(World world, int x, int y, int z, ItemStack stack) {
        TileMachine te = BlockContainerMachine.tile(world, x, y, z);
        if (te == null) return false;
        boolean any = false;
        for (int i = 0; i < te.items.getSizeInventory(); i++) {
            ItemStack pattern = te.items.getStackInSlot(i);
            if (pattern == null) continue;
            any = true;
            if (InvUtil.sameItem(pattern, stack)) return true;
        }
        return !any;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (!world.isRemote) player.openGui(RedCircuit.instance, GuiHandler.CHEST, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileMachine te = BlockContainerMachine.tile(world, x, y, z);
        if (te != null) InvUtil.dropAll(world, x, y, z, te.items);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
