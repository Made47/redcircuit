package pl.geesio.redcircuit.compat.computer;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.Pos;
import pl.geesio.redcircuit.wire.BundledIO;

/**
 * Integracja z ComputerCraft 1.7x:
 * <ul>
 *     <li>kabel wiązkowy ↔ {@code redstone.getBundledInput/setBundledOutput} komputerów i żółwi,</li>
 *     <li>peryferia {@code redcircuit_gate} i {@code redcircuit_wireless}.</li>
 * </ul>
 */
public final class CcCompat {
    private CcCompat() {}

    public static void init() {
        // Komputer czyta kabel: CC pyta o (pozycja kabla, ściana kabla zwrócona do komputera).
        ComputerCraftAPI.registerBundledRedstoneProvider((world, x, y, z, side) ->
                BundledIO.cableOutput(world, new Pos(x, y, z), ForgeDirection.getOrientation(side)));

        // Kabel czyta komputer.
        BundledIO.register(new BundledIO.Source() {
            @Override
            public int output(World world, Pos pos, ForgeDirection side) {
                if (!isComputer(world.getBlock(pos.x, pos.y, pos.z))) return -1;
                return ComputerCraftAPI.getBundledRedstoneOutput(world, pos.x, pos.y, pos.z, side.ordinal());
            }

            @Override
            public boolean isDevice(IBlockAccess world, Pos pos, Block block) {
                return isComputer(block);
            }
        });

        ComputerCraftAPI.registerPeripheralProvider((world, x, y, z, side) -> {
            TileEntity te = world.getTileEntity(x, y, z);
            String type = ComputerApi.typeOf(te);
            return type == null ? null : new CcPeripheral(te, type);
        });
        RedCircuit.LOG.info("ComputerCraft: kable wiązkowe i peryferia włączone");
    }

    /** Komputery i żółwie ComputerCraft. */
    static boolean isComputer(Block block) {
        Object id = Block.blockRegistry.getNameForObject(block);
        if (!(id instanceof String s) || !s.startsWith("ComputerCraft:")) return false;
        String name = s.substring("ComputerCraft:".length());
        return name.startsWith("CC-Computer") || name.startsWith("CC-Turtle") || name.equals("command_computer");
    }
}
