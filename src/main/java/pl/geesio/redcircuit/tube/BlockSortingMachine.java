package pl.geesio.redcircuit.tube;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.machine.BlockContainerMachine;
import pl.geesio.redcircuit.machine.TileMachine;
import pl.geesio.redcircuit.util.Dyes;
import pl.geesio.redcircuit.util.InvUtil;

/**
 * Maszyna sortująca: na impuls redstone pobiera pasujący stos z inwentarza za sobą, nadaje mu kolor kolumny
 * i wysyła do rury. Konfiguracja 9 kolumn x 3 rzędy: górny rząd - barwnik kolumny, rzędy 2-3 - wzorce.
 * Kolumna z barwnikiem, ale bez wzorców, łapie wszystkie pozostałe przedmioty.
 */
public class BlockSortingMachine extends BlockExtractor {
    public static final int COLUMNS = 9;

    public BlockSortingMachine() {
        super("sorting_machine");
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileMachine(27, "container.redcircuit.sorting_machine", false);
    }

    /** Numer kolumny pasującej do przedmiotu lub -1. */
    public static int columnFor(IInventory config, ItemStack stack) {
        for (int c = 0; c < COLUMNS; c++) {
            for (int row = 1; row <= 2; row++) {
                ItemStack pattern = config.getStackInSlot(row * COLUMNS + c);
                if (pattern != null && InvUtil.sameItem(pattern, stack)) return c;
            }
        }
        for (int c = 0; c < COLUMNS; c++) {
            boolean hasPatterns = config.getStackInSlot(COLUMNS + c) != null || config.getStackInSlot(2 * COLUMNS + c) != null;
            if (!hasPatterns && config.getStackInSlot(c) != null) return c;
        }
        return -1;
    }

    @Override
    protected boolean accepts(World world, int x, int y, int z, ItemStack stack) {
        TileMachine te = BlockContainerMachine.tile(world, x, y, z);
        return te != null && columnFor(te.items, stack) >= 0;
    }

    @Override
    protected int colorFor(World world, int x, int y, int z, ItemStack stack) {
        TileMachine te = BlockContainerMachine.tile(world, x, y, z);
        if (te == null) return -1;
        int column = columnFor(te.items, stack);
        return column < 0 ? -1 : Dyes.colorOf(te.items.getStackInSlot(column));
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
