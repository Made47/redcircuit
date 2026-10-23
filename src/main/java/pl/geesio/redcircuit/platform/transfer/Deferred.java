package pl.geesio.redcircuit.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import pl.geesio.redcircuit.RedCircuit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Operacje wykonywane dopiero po zatwierdzeniu transakcji Fabrica.
 * Inwentarze RedCircuit działają jak w NeoForge (symulacja + wykonanie), a Fabric wymaga transakcji
 * z możliwością wycofania - więc w trakcie transakcji tylko symulujemy i zapisujemy operację,
 * a wykonujemy ją po zatwierdzeniu najbardziej zewnętrznej transakcji. Wycofanie transakcji
 * (także zagnieżdżonej) usuwa zapisane w niej operacje.
 */
final class Deferred {
    record Op(Object slot, boolean insert, Object resource, long amount, int depth, Runnable action) {}

    static final class State {
        final List<Op> ops = new ArrayList<>();
        final Set<TransactionContext> watched = Collections.newSetFromMap(new IdentityHashMap<>());

        /** Suma oczekujących operacji danego rodzaju na slocie (dla danego zasobu). */
        long pending(Object slot, boolean insert, Object resource) {
            long sum = 0;
            for (Op op : ops) {
                if (Objects.equals(op.slot, slot) && op.insert == insert && Objects.equals(op.resource, resource)) sum += op.amount;
            }
            return sum;
        }

        /** Czy na slocie czeka operacja innego rodzaju albo z innym zasobem (wtedy slot pomijamy). */
        boolean conflicts(Object slot, boolean insert, Object resource) {
            for (Op op : ops) {
                if (Objects.equals(op.slot, slot) && (op.insert != insert || !Objects.equals(op.resource, resource))) return true;
            }
            return false;
        }
    }

    private final Map<TransactionContext, State> states = new IdentityHashMap<>();

    State state(TransactionContext tx) {
        TransactionContext outer = tx.getOpenTransaction(0);
        State st = states.get(outer);
        if (st == null) {
            State created = new State();
            st = created;
            states.put(outer, created);
            outer.addOuterCloseCallback(result -> {
                states.remove(outer);
                if (result.wasCommitted()) {
                    for (Op op : created.ops) {
                        try {
                            op.action.run();
                        } catch (RuntimeException e) {
                            RedCircuit.LOGGER.error("RedCircuit: deferred transfer operation failed", e);
                        }
                    }
                }
            });
        }
        if (st.watched.add(tx)) {
            State watchedState = st;
            tx.addCloseCallback((t, result) -> {
                int depth = t.nestingDepth();
                if (result.wasAborted()) {
                    watchedState.ops.removeIf(op -> op.depth >= depth);
                } else if (depth > 0) {
                    // zatwierdzona zagnieżdżona: operacje należą teraz do transakcji nadrzędnej
                    watchedState.ops.replaceAll(op -> op.depth >= depth
                            ? new Op(op.slot, op.insert, op.resource, op.amount, depth - 1, op.action) : op);
                }
                watchedState.watched.remove(t);
            });
        }
        return st;
    }

    void add(TransactionContext tx, State st, Object slot, boolean insert, Object resource, long amount, Runnable action) {
        st.ops.add(new Op(slot, insert, resource, amount, tx.nestingDepth(), action));
    }

    /** Otwiera transakcję (zagnieżdżoną, jeśli jakaś już trwa). */
    static Transaction open() {
        return Transaction.isOpen() ? Transaction.openNested(Transaction.getCurrentUnsafe()) : Transaction.openOuter();
    }
}
