package org.decampo.xirr;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Calculates the irregular rate of return on a series of transactions.  The
 * irregular rate of return is the constant rate for which, if the transactions
 * had been applied to an investment with that rate, the same resulting returns
 * would be realized.
 * <p>
 * When creating the list of {@link Transaction} instances to feed Xirr, be
 * sure to include one transaction representing the present value of the account
 * now, as if you had cashed out the investment.
 */
public class Xirr {

    private static final double DAYS_IN_YEAR = 365;

    private final List<Investment> investments;
    private final XirrDetails details;

    /**
     * Construct an Xirr instance for the given transactions.
     * @param tx the transactions
     * @throws IllegalArgumentException if there are fewer than 2 transactions
     * @throws IllegalArgumentException if all the transactions are on the same date
     * @throws IllegalArgumentException if all the transactions negative (deposits)
     * @throws IllegalArgumentException if all the transactions non-negative (withdrawals)
     */
    public Xirr(Transaction... tx) {
        this(Arrays.asList(tx));
    }

    /**
     * Construct an Xirr instance for the given transactions.
     * @param txs the transactions
     * @throws IllegalArgumentException if there are fewer than 2 transactions
     * @throws IllegalArgumentException if all the transactions are on the same date
     * @throws IllegalArgumentException if all the transactions negative (deposits)
     * @throws IllegalArgumentException if all the transactions non-negative (withdrawals)
     */
    public Xirr(Collection<Transaction> txs) {
        if (txs.size() < 2) {
            throw new IllegalArgumentException(
                "Must have at least two transactions");
        }
        details = txs.stream().collect(XirrDetails.collector());
        details.validate();
        investments = txs.stream()
            .map(this::createInvestment)
            .collect(Collectors.toList());
    }

    private Investment createInvestment(Transaction tx) {
        // Transform the transaction into an Investment instance
        // It is much easier to calculate the present value of an Investment
        final Investment result = new Investment();
        result.amount = tx.amount;
        // Don't use YEARS.between() as it returns whole numbers
        result.years = DAYS.between(tx.when, details.end) / DAYS_IN_YEAR;
        return result;
    }

    /**
     * Calculates the present value of the investment if it had been subject to
     * the given rate of return.
     * @param rate the rate of return
     * @return the present value of the investment if it had been subject to the
     *         given rate of return
     */
    public double presentValue(final double rate) {
        return investments.stream()
            .mapToDouble(inv -> inv.presentValue(rate))
            .sum();
    }

    /**
     * The derivative of the present value under the given rate.
     * @param rate the rate of return
     * @return derivative of the present value under the given rate
     */
    public double derivative(final double rate) {
        return investments.stream()
            .mapToDouble(inv -> inv.derivative(rate))
            .sum();
    }

    /**
     * Calculates the irregular rate of return of the transactions for this
     * instance of Xirr.
     * @return the irregular rate of return of the transactions
     * @throws ArithmeticException if the derivative is 0 while executing the Newton-Raphson method
     * @throws IllegalArgumentException if the Newton-Raphson method fails to converge in the
     */
    public double xirr() {
        return NewtonRaphson.builder()
            .withFunction(this::presentValue)
            .withDerivative(this::derivative)
            .findRoot(Math.signum(details.total)/100);
    }

    /**
     * Convenience class which represents {@link Transaction} instances more
     * conveniently for our purposes.
     */
    private static class Investment {
        /** The amount of the investment. */
        double amount;
        /** The number of years for which the investment applies, including
         * fractional years. */
        double years;

        /**
         * Present value of the investment at the given rate.
         * @param rate the rate of return
         * @return present value of the investment at the given rate
         */
        private double presentValue(final double rate) {
            return amount * Math.pow(1 + rate, years);
        }

        /**
         * Derivative of the present value of the investment at the given rate.
         * @param rate the rate of return
         * @return derivative of the present value at the given rate
         */
        private double derivative(final double rate) {
            if (years == 0) {
                return 0;
            } else {
                return amount * years * Math.pow(1 + rate, years - 1);
            }
        }
    }
    
}
