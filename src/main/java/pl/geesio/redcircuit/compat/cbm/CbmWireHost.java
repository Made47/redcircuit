package pl.geesio.redcircuit.compat.cbm;

import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.block.TileMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.wire.PartWires;

/** Dostęp sieci przewodów RedCircuit do przewodów-części w blokach CB Multipart. */
public class CbmWireHost implements PartWires.Host {

    @Nullable
    static RCWirePart part(BlockGetter level, BlockPos pos, int face) {
        if (!(level.getBlockEntity(pos) instanceof TileMultipart tile)) return null;
        for (MultiPart p : tile.getPartList()) {
            if (p instanceof RCWirePart w && w.face() == face) return w;
        }
        return null;
    }

    @Nullable
    static Integer typeAt(BlockGetter level, BlockPos pos, int face) {
        RCWirePart w = part(level, pos, face);
        return w == null ? null : w.wireType();
    }

    @Nullable
    @Override
    public Integer wireAt(BlockGetter level, BlockPos pos, int face) {
        return typeAt(level, pos, face);
    }

    @Override
    public boolean isHost(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileMultipart;
    }

    @Override
    public int getSignal(BlockGetter level, BlockPos pos, int face, int ch) {
        RCWirePart w = part(level, pos, face);
        if (w instanceof SignalWirePart s && ch < 0) return s.getPower();
        if (w instanceof BundledWirePart b && ch >= 0) return b.getChannel(ch);
        return 0;
    }

    @Override
    public void setSignal(Level level, BlockPos pos, int face, int ch, int value) {
        RCWirePart w = part(level, pos, face);
        if (w instanceof SignalWirePart s && ch < 0) s.setPower(value);
        else if (w instanceof BundledWirePart b && ch >= 0) b.setChannel(ch, value);
    }

    @Override
    public void notifyChanged(Level level, BlockPos pos, int face) {
        RCWirePart w = part(level, pos, face);
        if (w instanceof SignalWirePart) w.notifyOutput();
    }
}
