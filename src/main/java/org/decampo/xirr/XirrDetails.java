package org.decampo.xirr;

import java.time.LocalDate;
import java.util.stream.Collector;

/**
 * Converts a stream of {@link Transaction} instances into the data needed for
 * the {@link Xirr} algorithm.
 */
class XirrDetails {
    public static Collector<Transaction, XirrDetails, XirrDetails> collector() {
        return Collector.of(
            XirrDetails::new,
            XirrDetails::accumulate,
            XirrDetails::combine,
            Collector.Characteristics.IDENTITY_FINISH,
            Collector.Characteristics.UNORDERED);
    }

    LocalDate start;
    LocalDate end;
    double minAmount = Double.POSITIVE_INFINITY;
    double maxAmount = Double.NEGATIVE_INFINITY;
    double total;
    double deposits;

    public void accumulate(final Transaction tx) {
        start = start != null && start.isBefore(tx.when) ? start : tx.when;
        end = end != null && end.isAfter(tx.when) ? end : tx.when;
        minAmount = Math.min(minAmount, tx.amount);
        maxAmount = Math.max(maxAmount, tx.amount);
        total += tx.amount;
        if (tx.amount < 0) {
            deposits -= tx.amount;
        }
    }

    public XirrDetails combine(final XirrDetails other) {
        start = start.isBefore(other.start) ? start : other.start;
        end = end.isAfter(other.end) ? end : other.end;
        minAmount = Math.min(minAmount, other.minAmount);
        maxAmount = Math.max(maxAmount, other.maxAmount);
        total += other.total;
        return this;
    }

    public void validate() {
        if (start == null) {
            throw new IllegalArgumentException("No transactions to anaylze");
        }

        if (start.equals(end)) {
            throw new IllegalArgumentException(
                "Transactions must not all be on the same day.");
        }
        if (minAmount >= 0) {
            throw new IllegalArgumentException(
                "Transactions must not all be nonnegative.");
        }
        if (maxAmount < 0) {
            throw new IllegalArgumentException(
                "Transactions must not be negative.");
        }
    }

}
