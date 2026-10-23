package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.platform.capabilities.BlockCapability;
import pl.geesio.redcircuit.platform.capabilities.Capabilities;
import pl.geesio.redcircuit.platform.energy.IEnergyStorage;
import pl.geesio.redcircuit.platform.fluids.capability.IFluidHandler;
import pl.geesio.redcircuit.platform.items.IItemHandler;

/**
 * Połączenie "capabilities" RedCircuit z API Fabrica:
 * przedmioty i płyny - Fabric Transfer API, energia - Team Reborn Energy.
 */
public final class FabricTransfer {
    private FabricTransfer() {}

    // ------------------------------------------------------------------ inne mody -> RedCircuit

    @Nullable
    public static IItemHandler findItems(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side) {
        Storage<ItemVariant> s = ItemStorage.SIDED.find(level, pos, state, be, side);
        if (s == null) return null;
        if (s instanceof ItemHandlerStorage own) return own.handler();
        return new StorageItemHandler(s);
    }

    @Nullable
    public static IFluidHandler findFluids(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side) {
        Storage<FluidVariant> s = FluidStorage.SIDED.find(level, pos, state, be, side);
        if (s == null) return null;
        if (s instanceof FluidHandlerStorage own) return own.handler();
        return new StorageFluidHandler(s);
    }

    @Nullable
    public static IEnergyStorage findEnergy(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side) {
        team.reborn.energy.api.EnergyStorage s = null;
        if (side != null) {
            s = team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, state, be, side);
        } else {
            for (Direction d : Direction.values()) {
                s = team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, state, be, d);
                if (s != null) break;
            }
        }
        if (s == null) return null;
        if (s instanceof EnergyExport own) return own.handler();
        return new EnergyImport(s);
    }

    // ------------------------------------------------------------------ RedCircuit -> inne mody

    @SuppressWarnings("unchecked")
    public static <T, C, BE extends BlockEntity> void exportBlockEntity(BlockCapability<T, C> cap, BlockEntityType<BE> type) {
        if (cap == (Object) Capabilities.ItemHandler.BLOCK) {
            BlockCapability<IItemHandler, Direction> c = (BlockCapability<IItemHandler, Direction>) (Object) cap;
            ItemStorage.SIDED.registerForBlockEntity((be, dir) -> {
                IItemHandler h = c.getOwn(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, dir);
                return h == null ? null : new ItemHandlerStorage(h);
            }, type);
        } else if (cap == (Object) Capabilities.FluidHandler.BLOCK) {
            BlockCapability<IFluidHandler, Direction> c = (BlockCapability<IFluidHandler, Direction>) (Object) cap;
            FluidStorage.SIDED.registerForBlockEntity((be, dir) -> {
                IFluidHandler h = c.getOwn(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, dir);
                return h == null ? null : new FluidHandlerStorage(h);
            }, type);
        } else if (cap == (Object) Capabilities.EnergyStorage.BLOCK) {
            BlockCapability<IEnergyStorage, Direction> c = (BlockCapability<IEnergyStorage, Direction>) (Object) cap;
            team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> {
                IEnergyStorage h = c.getOwn(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, dir);
                return h == null ? null : new EnergyExport(h);
            }, type);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, C> void exportBlocks(BlockCapability<T, C> cap, Block... blocks) {
        if (cap == (Object) Capabilities.ItemHandler.BLOCK) {
            BlockCapability<IItemHandler, Direction> c = (BlockCapability<IItemHandler, Direction>) (Object) cap;
            ItemStorage.SIDED.registerForBlocks((level, pos, state, be, dir) -> {
                IItemHandler h = c.getOwn(level, pos, state, be, dir);
                return h == null ? null : new ItemHandlerStorage(h);
            }, blocks);
        } else if (cap == (Object) Capabilities.FluidHandler.BLOCK) {
            BlockCapability<IFluidHandler, Direction> c = (BlockCapability<IFluidHandler, Direction>) (Object) cap;
            FluidStorage.SIDED.registerForBlocks((level, pos, state, be, dir) -> {
                IFluidHandler h = c.getOwn(level, pos, state, be, dir);
                return h == null ? null : new FluidHandlerStorage(h);
            }, blocks);
        } else if (cap == (Object) Capabilities.EnergyStorage.BLOCK) {
            BlockCapability<IEnergyStorage, Direction> c = (BlockCapability<IEnergyStorage, Direction>) (Object) cap;
            team.reborn.energy.api.EnergyStorage.SIDED.registerForBlocks((level, pos, state, be, dir) -> {
                IEnergyStorage h = c.getOwn(level, pos, state, be, dir);
                return h == null ? null : new EnergyExport(h);
            }, blocks);
        }
    }
}
