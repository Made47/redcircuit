package pl.geesio.redcircuit.util.transfer;

import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Pomocnik transakcji: otwiera transakcję zagnieżdżoną w bieżącej (jeśli jakaś jest otwarta). */
public final class Tx {
    private Tx() {}

    public static Transaction open() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }
}
