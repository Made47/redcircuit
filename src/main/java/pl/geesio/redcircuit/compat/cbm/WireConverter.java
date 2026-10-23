package pl.geesio.redcircuit.compat.cbm;

import codechicken.multipart.api.PartConverter;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.util.MultipartPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.energy.PowerWireBlock;
import pl.geesio.redcircuit.energy.EnergyNodeBlockEntity;
import pl.geesio.redcircuit.wire.BundledCableBlock;
import pl.geesio.redcircuit.wire.BundledCableBlockEntity;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.RedWireBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Zamiana bloków-przewodów na części (gdy w ich blok wstawia się mikroblok) oraz stawianie
 * przewodów jako części w blokach CB Multipart (np. obok osłony).
 */
public class WireConverter extends PartConverter {

    @Nullable
    static RCWirePart newPart(FaceWireBlock wire, Direction side) {
        RCWirePart part;
        if (wire instanceof RedWireBlock red) part = new SignalWirePart().setColor(red.getColor());
        else if (wire instanceof BundledCableBlock) part = new BundledWirePart();
        else if (wire instanceof PowerWireBlock) part = new PowerWirePart();
        else return null;
        return part.setFace(side);
    }

    @Override
    public ConversionResult<Collection<MultiPart>> convert(LevelAccessor world, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof FaceWireBlock wire)) return emptyResultList();
        List<MultiPart> parts = new ArrayList<>();
        int sides = 0;
        for (Direction side : Direction.values()) if (FaceWireBlock.hasFace(state, side)) sides++;
        for (Direction side : Direction.values()) {
            if (!FaceWireBlock.hasFace(state, side)) continue;
            RCWirePart part = newPart(wire, side);
            if (part == null) return emptyResultList();
            if (part instanceof SignalWirePart s && state.hasProperty(RedWireBlock.POWER)) {
                s.setPowerSilently(state.getValue(RedWireBlock.POWER));
            } else if (part instanceof BundledWirePart b && world.getBlockEntity(pos) instanceof BundledCableBlockEntity be) {
                for (int ch = 0; ch < 16; ch++) b.setChannelSilently(ch, be.getChannel(ch));
            } else if (part instanceof PowerWirePart a && world.getBlockEntity(pos) instanceof EnergyNodeBlockEntity be) {
                a.getEnergy().setEnergy(be.getEnergy().getEnergyStored() / Math.max(1, sides));
            }
            parts.add(part);
        }
        return ConversionResult.<Collection<MultiPart>>success(parts);
    }

    @Override
    public ConversionResult<MultiPart> convert(MultipartPlaceContext context) {
        ItemStack held = context.getItemInHand();
        if (!(held.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof FaceWireBlock wire)) return emptyResult();
        BlockPos pos = context.getClickedPos();
        // Zwykły blok-przewód obsługuje dokładanie kolejnych ścian sam
        if (context.getLevel().getBlockState(pos).getBlock() instanceof FaceWireBlock) return emptyResult();
        Direction side = context.getClickedFace().getOpposite();
        if (!FaceWireBlock.canAttach(context.getLevel(), pos, side)) return emptyResult();
        RCWirePart part = newPart(wire, side);
        if (part == null) return emptyResult();
        return ConversionResult.<MultiPart>success(part);
    }
}
