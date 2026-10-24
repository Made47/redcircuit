package pl.geesio.redcircuit.gate;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.registry.ModBlockEntities;

/** Dane bramek: ustawienia z okna (wyłączone wejścia, odwrócenie, czas...) i pamięć (licznik, synchronizator). */
public class GateBlockEntity extends BlockEntity {
    public static final int MIN_INTERVAL = 4, MAX_INTERVAL = 72_000;
    public static final int MAX_COUNT = 1024;

    /** Okres zegara / krok sekwencera / czas komórki stanu, w tickach. */
    public int interval = 20;
    /** Maksimum licznika. */
    public int max = 4;
    public int count;
    public boolean lastL, lastB, lastR;
    public boolean latchL, latchR;
    public long due;
    /** Wyciszenie dźwięku tykania (ustawiane w oknie bramki). */
    public boolean muted;
    /** Komórka stanu: wyjście "w trakcie odliczania" po lewej zamiast po prawej. */
    public boolean mirrored;
    /** Wyłączone wejścia (bity GateType.IN_L/IN_B/IN_R). */
    public int disabled;
    /** Zatrzask RS: wejścia (boki) nie świecą razem z wyjściami - świeci tylko przód albo tył. */
    public boolean quiet;

    public GateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATE.get(), pos, state);
        // NAND ma trzy wejścia; domyślnie tylne jest wyłączone (działa jak dotychczasowa 2-wejściowa)
        if (state.getBlock() instanceof GateBlock g) disabled = g.getGateType().defaultDisabled();
    }

    /** Czy ustawienia różnią się od domyślnych (wtedy śrubokręt zapisuje je w przedmiocie). */
    public boolean hasCustomSettings() {
        return interval != 20 || max != 4 || muted || mirrored || disabled != defaultDisabled() || quiet;
    }

    private int defaultDisabled() {
        return getBlockState().getBlock() instanceof GateBlock g ? g.getGateType().defaultDisabled() : 0;
    }

    public int max() {
        return Math.max(1, max);
    }

    /** Zapis + wysłanie danych do klientów (okno ustawień pokazuje aktualne wartości). */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Interval", interval);
        output.putInt("MaxValue", max);
        output.putInt("Count", count);
        output.putBoolean("L", lastL);
        output.putBoolean("B", lastB);
        output.putBoolean("R", lastR);
        output.putBoolean("LatchL", latchL);
        output.putBoolean("LatchR", latchR);
        output.putLong("Due", due);
        output.putBoolean("Muted", muted);
        output.putBoolean("Mirrored", mirrored);
        output.putInt("Disabled", disabled);
        output.putBoolean("Quiet", quiet);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (input.getInt("Interval").isPresent()) interval = input.getIntOr("Interval", 0);
        if (input.getInt("MaxValue").isPresent()) max = input.getIntOr("MaxValue", 0);
        count = input.getIntOr("Count", 0);
        lastL = input.getBooleanOr("L", false);
        lastB = input.getBooleanOr("B", false);
        lastR = input.getBooleanOr("R", false);
        latchL = input.getBooleanOr("LatchL", false);
        latchR = input.getBooleanOr("LatchR", false);
        due = input.getLongOr("Due", 0L);
        muted = input.getBooleanOr("Muted", false);
        mirrored = input.getBooleanOr("Mirrored", false);
        if (input.getInt("Disabled").isPresent()) disabled = input.getIntOr("Disabled", 0) & 7;
        quiet = input.getBooleanOr("Quiet", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
