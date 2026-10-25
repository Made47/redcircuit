package pl.geesio.redcircuit.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import pl.geesio.redcircuit.platform.fluids.FluidStack;
import pl.geesio.redcircuit.platform.fluids.capability.IFluidHandler;
import pl.geesio.redcircuit.wire.BundledCableBlockEntity;
import pl.geesio.redcircuit.wire.RedWireBlock;

/**
 * Miernik (multimetr): PPM na bloku wypisuje sygnał redstone (wejście/wyjście, poziom przewodu),
 * kanały kabla wiązkowego, energię (FE) i zawartość zbiorników cieczy.
 */
public class MultimeterItem extends Item {
    public MultimeterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide() || player == null) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos();
        Direction side = ctx.getClickedFace();
        BlockState state = level.getBlockState(pos);

        player.sendSystemMessage(Component.literal("— ").append(state.getBlock().getName()).append(" —").withStyle(ChatFormatting.GOLD));

        // Redstone
        if (state.hasProperty(RedWireBlock.POWER)) {
            line(player, "meter.redcircuit.wire_power", state.getValue(RedWireBlock.POWER));
        } else {
            int out = 0;
            for (Direction d : Direction.values()) out = Math.max(out, state.getSignal(level, pos, d));
            line(player, "meter.redcircuit.signal_in", level.getBestNeighborSignal(pos));
            if (state.isSignalSource()) line(player, "meter.redcircuit.signal_out", out);
        }

        // Kabel wiązkowy
        if (level.getBlockEntity(pos) instanceof BundledCableBlockEntity cable) {
            MutableComponent l = Component.translatable("meter.redcircuit.channels");
            boolean any = false;
            for (int ch = 0; ch < 16; ch++) {
                int v = cable.getChannel(ch);
                if (v <= 0) continue;
                any = true;
                l.append(Component.literal(" ■").withColor(DyeColor.byId(ch).getTextColor()))
                        .append(Component.literal(Integer.toString(v)));
            }
            player.sendSystemMessage(any ? l : Component.translatable("meter.redcircuit.channels_none"));
        }

        // Energia
        IEnergyStorage es = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.EnergyStorage.BLOCK, pos, side);
        if (es == null) es = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.EnergyStorage.BLOCK, pos, null);
        if (es != null) {
            player.sendSystemMessage(Component.translatable("meter.redcircuit.energy", es.getEnergyStored(), es.getMaxEnergyStored())
                    .withStyle(ChatFormatting.AQUA));
        }

        // Ciecze
        IFluidHandler fh = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.FluidHandler.BLOCK, pos, side);
        if (fh == null) fh = pl.geesio.redcircuit.platform.capabilities.CapLookup.block(level, Capabilities.FluidHandler.BLOCK, pos, null);
        if (fh != null) {
            for (int t = 0; t < fh.getTanks(); t++) {
                FluidStack fs = fh.getFluidInTank(t);
                Component name = fs.isEmpty() ? Component.translatable("message.redcircuit.pump_empty") : fs.getHoverName();
                player.sendSystemMessage(Component.translatable("meter.redcircuit.fluid", name, fs.getAmount(), fh.getTankCapacity(t))
                        .withStyle(ChatFormatting.BLUE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void line(Player player, String key, int value) {
        player.sendSystemMessage(Component.translatable(key, value).withStyle(value > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
    }
}
