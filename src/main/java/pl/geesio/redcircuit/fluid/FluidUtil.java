package pl.geesio.redcircuit.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidBlock;

/** Pomocnicze operacje na płynach Forge 1.7.10 (bloki płynów w świecie, wiadra, komunikaty). */
public final class FluidUtil {
    private FluidUtil() {}

    /** Płyn bloku w świecie (woda, lawa, płyny modów) albo null. */
    public static Fluid fluidAt(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        if (b == Blocks.water || b == Blocks.flowing_water) return FluidRegistry.WATER;
        if (b == Blocks.lava || b == Blocks.flowing_lava) return FluidRegistry.LAVA;
        if (b instanceof IFluidBlock fb) return fb.getFluid();
        return FluidRegistry.lookupFluidForBlock(b);
    }

    /** Czy w (x, y, z) jest pełne źródło płynu, które da się zebrać. */
    public static boolean isSource(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        if (b instanceof BlockLiquid) return world.getBlockMetadata(x, y, z) == 0;
        if (b instanceof IFluidBlock fb) return fb.canDrain(world, x, y, z);
        return false;
    }

    /** Zbiera źródło płynu (1000 mB) ze świata; zwraca zebrany płyn albo null. */
    public static FluidStack pickup(World world, int x, int y, int z) {
        Block b = world.getBlock(x, y, z);
        if (b instanceof IFluidBlock fb) return fb.canDrain(world, x, y, z) ? fb.drain(world, x, y, z, true) : null;
        Fluid f = fluidAt(world, x, y, z);
        if (f == null || !isSource(world, x, y, z)) return null;
        world.setBlockToAir(x, y, z);
        return new FluidStack(f, FluidContainerRegistry.BUCKET_VOLUME);
    }

    /** Blok, który wylewa się w świecie z danego płynu (woda/lawa jako płynące, żeby się rozlały). */
    public static Block placeBlock(Fluid fluid) {
        if (fluid == FluidRegistry.WATER) return Blocks.flowing_water;
        if (fluid == FluidRegistry.LAVA) return Blocks.flowing_lava;
        return fluid.canBePlacedInWorld() ? fluid.getBlock() : null;
    }

    /** Nazwa płynu do komunikatu ("Pusto", gdy brak). */
    public static Object name(FluidStack fs) {
        return fs == null || fs.amount <= 0 ? new ChatComponentTranslation("message.redcircuit.pump_empty") : fs.getLocalizedName();
    }

    /**
     * PPM pojemnikiem (wiadro, butelka, pojemniki modów z rejestru FluidContainerRegistry) na zbiorniku:
     * pełny pojemnik wylewa się do zbiornika, pusty napełnia się z niego. Zwraca true, jeśli coś się stało.
     */
    public static boolean interact(EntityPlayer player, FluidTank tank) {
        ItemStack held = player.getHeldItem();
        if (held == null) return false;
        FluidStack inItem = FluidContainerRegistry.getFluidForFilledItem(held);
        if (inItem != null) {
            if (tank.fill(inItem, false) < inItem.amount) return true;
            tank.fill(inItem, true);
            if (!player.capabilities.isCreativeMode) {
                ItemStack empty = FluidContainerRegistry.drainFluidContainer(held);
                give(player, held, empty);
            }
            return true;
        }
        if (FluidContainerRegistry.isEmptyContainer(held) && tank.getFluid() != null) {
            ItemStack filled = FluidContainerRegistry.fillFluidContainer(tank.getFluid(), held);
            FluidStack taken = filled == null ? null : FluidContainerRegistry.getFluidForFilledItem(filled);
            if (taken == null) return true;
            tank.drain(taken.amount, true);
            if (!player.capabilities.isCreativeMode) give(player, held, filled);
            return true;
        }
        return false;
    }

    private static void give(EntityPlayer player, ItemStack held, ItemStack result) {
        if (--held.stackSize <= 0) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, result);
        } else if (result != null && !player.inventory.addItemStackToInventory(result)) {
            player.dropPlayerItemWithRandomChoice(result, false);
        }
        player.inventoryContainer.detectAndSendChanges();
    }
}
