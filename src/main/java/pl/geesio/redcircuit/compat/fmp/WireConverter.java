package pl.geesio.redcircuit.compat.fmp;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TMultiPart;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.energy.IEnergyNode;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.TileWire;

import java.util.Arrays;

/**
 * Zamienia przewód-blok w część ForgeMultipart, gdy gracz dokłada mikroblok lub inny przewód do tego bloku.
 * Konwerter zwraca jedną część, więc obsługiwane są tylko przewody leżące na jednej ścianie.
 */
public class WireConverter implements MultiPartRegistry.IPartConverter {

    @Override
    public Iterable<Block> blockTypes() {
        return Arrays.asList(ModBlocks.signalAlloyWire, ModBlocks.insulatedWire, ModBlocks.bundledCable, ModBlocks.powerAlloyWire);
    }

    @Override
    public TMultiPart convert(World world, BlockCoord pos) {
        Block block = world.getBlock(pos.x, pos.y, pos.z);
        if (!(block instanceof BlockFaceWire wire)) return null;
        TileWire t = BlockFaceWire.tile(world, pos.x, pos.y, pos.z);
        if (t == null || t.faceCount() != 1) return null;
        return fromBlock(wire, world.getBlockMetadata(pos.x, pos.y, pos.z) & 15, t,
                ForgeDirection.getOrientation(Integer.numberOfTrailingZeros(t.faces & 63)), true);
    }

    /** Część odpowiadająca jednej ścianie przewodu-bloku, ze skopiowanym sygnałem (i energią, gdy {@code energy}). */
    static PartWire fromBlock(BlockFaceWire wire, int meta, TileWire t, ForgeDirection face, boolean energy) {
        PartWire part = FmpCompat.newPart(wire, meta);
        if (part == null) return null;
        part.setFace(face);
        if (part instanceof PartSignalWire s) s.setPowerSilently(t.power);
        else if (part instanceof PartBundledWire b) for (int ch = 0; ch < 16; ch++) b.setChannelSilently(ch, t.channels[ch]);
        else if (energy && part instanceof PartPowerWire p && t instanceof IEnergyNode node) p.buffer().set(node.buffer().get());
        return part;
    }
}
