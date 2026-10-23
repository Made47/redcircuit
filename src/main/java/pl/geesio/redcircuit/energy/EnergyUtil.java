package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.util.Dirs;

/** Pomocnicze operacje na energii RF. */
public final class EnergyUtil {
    private EnergyUtil() {}

    /** Czy blok-encja po stronie {@code dir} od (x, y, z) przyjmuje połączenie energii od nas. */
    public static boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
        TileEntity te = world.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
        return te instanceof IEnergyConnection c && c.canConnectEnergy(dir.getOpposite());
    }

    /** Wypycha energię z bufora do sąsiednich odbiorców (do {@code rate} na stronę). */
    public static void push(World world, int x, int y, int z, EnergyBuffer buffer, int rate) {
        for (ForgeDirection d : Dirs.ALL) {
            if (buffer.get() <= 0) return;
            int nx = x + d.offsetX, ny = y + d.offsetY, nz = z + d.offsetZ;
            if (!world.blockExists(nx, ny, nz)) continue;
            TileEntity te = world.getTileEntity(nx, ny, nz);
            if (te instanceof IEnergyReceiver r && r.canConnectEnergy(d.getOpposite())) {
                int sent = r.receiveEnergy(d.getOpposite(), Math.min(buffer.get(), rate), false);
                if (sent > 0) buffer.set(buffer.get() - sent);
            }
        }
    }

    /** Komunikat "Energia: x / y RF" na pasku akcji. */
    public static void showEnergy(net.minecraft.entity.player.EntityPlayer player, int energy, int max) {
        player.addChatComponentMessage(new net.minecraft.util.ChatComponentTranslation("message.redcircuit.energy", energy, max));
    }
}
