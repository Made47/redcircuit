package pl.geesio.redcircuit.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import pl.geesio.redcircuit.gate.GateBlock;

/**
 * Przewód Signal Alloy (color = -1) lub przewód izolowany (color = 0..15), przyklejany do ścian.
 * Przewody nie tracą mocy na odległości - cała sieć ma jeden poziom sygnału
 * równy najsilniejszemu wejściu.
 */
public class RedWireBlock extends FaceWireBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    private final int color;

    public RedWireBlock(Properties properties, int color) {
        super(properties, color < 0 ? 2f : 4f, color < 0 ? 2f : 3f);
        this.color = color;
        registerDefaultState(noFaces(stateDefinition.any()).setValue(BlockStateProperties.DOWN, true).setValue(POWER, 0));
    }

    /** -1 dla Signal Alloy, 0..15 (DyeColor id) dla przewodu izolowanego. */
    public int getColor() {
        return color;
    }

    public boolean isInsulated() {
        return color >= 0;
    }

    /** Signal Alloy łączy się z każdym przewodem, izolowane tylko z tym samym kolorem. */
    public static boolean compatible(int a, int b) {
        return a < 0 || b < 0 || a == b;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    public boolean compatible(FaceWireBlock other) {
        if (other instanceof RedWireBlock w) return compatible(color, w.color);
        if (other instanceof BundledCableBlock) return isInsulated();
        return false;
    }

    @Override
    protected boolean connectsToDevice(BlockGetter level, BlockPos pos, Direction side, Direction dir, BlockPos neighborPos, BlockState neighbor) {
        if (neighbor.getBlock() instanceof GateBlock) return side == Direction.DOWN && GateBlock.connectsOn(neighbor, dir.getOpposite());
        return isRedstoneDevice(neighbor);
    }

    /** Jak niżej, ale bramki tylko od strony wyjść i włączonych wejść ({@code dir} = od przewodu do bloku). */
    public static boolean isRedstoneDevice(BlockState neighbor, Direction dir) {
        if (neighbor.getBlock() instanceof GateBlock) return GateBlock.connectsOn(neighbor, dir.getOpposite());
        return isRedstoneDevice(neighbor);
    }

    /** Bloki redstone, z którymi łączą się przewody sygnałowe (bez innych przewodów). */
    public static boolean isRedstoneDevice(BlockState neighbor) {
        Block b = neighbor.getBlock();
        if (b instanceof EncasedWireBlock) return false;
        if (b instanceof GateBlock) return true;
        if (b instanceof pl.geesio.redcircuit.light.LampBlock) return true;
        if (b instanceof pl.geesio.redcircuit.wireless.WirelessBlock) return true;
        if (neighbor.isSignalSource()) return true;
        // Maszyny RedCircuit sterowane sygnałem (ekstraktor, filtr, zapalarka, niszczarka, aplikator,
        // wyrzutnik, asembler, Requester, Manager, Regulator, detektor, sortowanie, pompa, generator na paliwo)
        if (b instanceof pl.geesio.redcircuit.tube.ExtractorBlock || b instanceof pl.geesio.redcircuit.energy.PumpBlock
                || b instanceof pl.geesio.redcircuit.energy.FuelGeneratorBlock) return true;
        return b instanceof net.minecraft.world.level.block.HopperBlock || b instanceof net.minecraft.world.level.block.PoweredRailBlock
                || b instanceof net.minecraft.world.level.block.BellBlock || b instanceof net.minecraft.world.level.block.CrafterBlock
                || b instanceof RedstoneLampBlock || b instanceof PistonBaseBlock || b instanceof DoorBlock
                || b instanceof TrapDoorBlock || b instanceof FenceGateBlock || b instanceof NoteBlock
                || b instanceof DispenserBlock || b instanceof TntBlock;
    }

    // ---------- Redstone ----------

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return WireNetwork.isEmitting() ? state.getValue(POWER) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return WireNetwork.isEmitting() ? state.getValue(POWER) : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            WireNetwork.update(level, pos);
        }
    }

    @Override
    protected void onWireChanged(Level level, BlockPos pos) {
        WireNetwork.onWireChanged(level, pos, this);
    }

    @Override
    protected void onWireRemoved(Level level, BlockPos pos) {
        WireNetwork.onWireRemoved(level, pos, this);
    }
}
