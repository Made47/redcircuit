package pl.geesio.redcircuit.util.transfer;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/** Magazyn energii (FE) z wygodnymi metodami receive/extract, widoczny też jako {@link EnergyHandler}. */
public interface IEnergyStorage extends EnergyHandler {
    default int receiveEnergy(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        try (Transaction tx = Tx.open()) {
            int n = insert(amount, tx);
            if (!simulate) tx.commit();
            return n;
        }
    }

    default int extractEnergy(int amount, boolean simulate) {
        if (amount <= 0) return 0;
        try (Transaction tx = Tx.open()) {
            int n = extract(amount, tx);
            if (!simulate) tx.commit();
            return n;
        }
    }

    default int getEnergyStored() {
        return getAmountAsInt();
    }

    default int getMaxEnergyStored() {
        return getCapacityAsInt();
    }

    default boolean canReceive() {
        return receiveEnergy(1, true) > 0 || getEnergyStored() >= getMaxEnergyStored();
    }

    default boolean canExtract() {
        return extractEnergy(1, true) > 0 || getEnergyStored() <= 0;
    }

    @Nullable
    static IEnergyStorage wrap(@Nullable EnergyHandler handler) {
        if (handler == null) return null;
        if (handler instanceof IEnergyStorage s) return s;
        return new Wrapper(handler);
    }

    record Wrapper(EnergyHandler h) implements IEnergyStorage {
        @Override
        public long getAmountAsLong() {
            return h.getAmountAsLong();
        }

        @Override
        public long getCapacityAsLong() {
            return h.getCapacityAsLong();
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            return h.insert(amount, transaction);
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            return h.extract(amount, transaction);
        }
    }
}
