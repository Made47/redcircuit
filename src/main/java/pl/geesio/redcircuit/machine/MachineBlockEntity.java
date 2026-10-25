package pl.geesio.redcircuit.machine;

import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import pl.geesio.redcircuit.platform.items.IItemHandler;
import pl.geesio.redcircuit.platform.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.tube.MachineContainer;
import pl.geesio.redcircuit.tube.ExtractorBlock;

/**
 * Wspólny blok-encja maszyn z inwentarzem (Relay, Ejector, Deployer, Assembler, Buffer).
 * Logika działania jest w klasach bloków; tu tylko sloty i kilka liczników.
 */
public class MachineBlockEntity extends BlockEntity {
    public final MachineContainer items;
    /** Licznik ticków (np. co ile Relay wypycha przedmioty). */
    public int timer;
    /** Kolejny slot do użycia (kolejka po slotach). */
    public int next;
    /** Ostatni stan 16 kanałów kabla wiązkowego (Assembler). */
    public int lastChannels;

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.items = new MachineContainer(this, size);
    }

    /** Dostęp dla rur i lejków: ze wszystkich stron poza przodem (wyjściem). */
    @Nullable
    public IItemHandler getHandler(@Nullable Direction side) {
        BlockState state = getBlockState();
        if (side != null && state.hasProperty(ExtractorBlock.FACING) && side == state.getValue(ExtractorBlock.FACING)) {
            return null;
        }
        return new InvWrapper(items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        items.save(output);
        output.putInt("Timer", timer);
        output.putInt("Next", next);
        output.putInt("Channels", lastChannels);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.load(input);
        timer = input.getIntOr("Timer", 0);
        next = input.getIntOr("Next", 0);
        lastChannels = input.getIntOr("Channels", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        Containers.dropContents(level, pos, this.items);
    }
}
