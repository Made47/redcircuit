package pl.geesio.redcircuit.fluid;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import pl.geesio.redcircuit.util.transfer.IFluidHandler;
import pl.geesio.redcircuit.util.transfer.FluidTank;
import pl.geesio.redcircuit.registry.ModBlockEntities;
import pl.geesio.redcircuit.registry.ModDataComponents;

/**
 * Zbiornik na ciecz: 16 000 mB (16 wiader). Ciecz spływa do zbiornika postawionego pod spodem.
 * Zawartość zostaje w przedmiocie po rozbiciu bloku.
 */
public class FluidTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;
    private static final int FLOW_DOWN = 1_000;

    private int lastLight = -1;
    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            changed();
        }
    };

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    /** Wypełnienie 0..1 (do renderowania i komparatora). */
    public float fill() {
        return tank.getFluidAmount() / (float) CAPACITY;
    }

    public int lightLevel() {
        FluidStack fs = tank.getFluid();
        return fs.isEmpty() ? 0 : fs.getFluidType().getLightLevel(fs);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, s.getBlock());
            int light = lightLevel();
            if (light != lastLight) {
                lastLight = light;
                level.getLightEngine().checkBlock(worldPosition);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity be) {
        if (level.isClientSide() || be.tank.isEmpty() || (level.getGameTime() % 5) != 0) return;
        // Grawitacja: ciecz spływa do zbiornika poniżej (gazy, np. lżejsze od powietrza, zostają)
        if (be.tank.getFluid().getFluidType().isLighterThanAir()) return;
        if (level.getBlockEntity(pos.below()) instanceof FluidTankBlockEntity below) {
            FluidStack moving = be.tank.drain(FLOW_DOWN, IFluidHandler.FluidAction.SIMULATE);
            int accepted = below.tank.fill(moving, IFluidHandler.FluidAction.EXECUTE);
            if (accepted > 0) be.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // ------------------------------------------------------------------ zapis / sync / przedmiot

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output.child("Tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input.childOrEmpty("Tank"));
        if (level != null && level.isClientSide()) level.getLightEngine().checkBlock(worldPosition);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) components.set(ModDataComponents.FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        tank.setFluid(input.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY).copy());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Tank");
    }
}
