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
 * <p>
 * Example usage:
 * <code>
 *     double rate = new Xirr(
 *             new Transaction(-1000, "2016-01-15"),
 *             new Transaction(-2500, "2016-02-08"),
 *             new Transaction(-1000, "2016-04-17"),
 *             new Transaction( 5050, "2016-08-24")
 *         ).xirr();
 * </code>
 * <p>
 * Example using the builder to gain more control:
 * <code>
 *     double rate = Xirr.builder()
 *         .withNewtonRaphsonBuilder(
 *             NewtonRaphson.builder()
 *                 .withIterations(1000)
 *                 .withTolerance(0.0001))
 *         .withGuess(.20)
 *         .withTransactions(
 *             new Transaction(-1000, "2016-01-15"),
 *             new Transaction(-2500, "2016-02-08"),
 *             new Transaction(-1000, "2016-04-17"),
 *             new Transaction( 5050, "2016-08-24")
 *         ).xirr();
 * </code>
 * <p>
 * This class is not thread-safe and is designed for each instance to be used
 * once.
 */
public class Xirr {

    private static final double DAYS_IN_YEAR = 365;

    /**
     * Convenience method for getting an instance of a {@link Builder}.
     * @return new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private final List<Investment> investments;
    private final XirrDetails details;

    private NewtonRaphson.Builder builder = null;
    private Double guess = null;

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
        this(txs, null, null);
    }

    private Xirr(Collection<Transaction> txs, NewtonRaphson.Builder builder, Double guess) {
        if (txs.size() < 2) {
            throw new IllegalArgumentException(
                "Must have at least two transactions");
        }
        details = txs.stream().collect(XirrDetails.collector());
        details.validate();
        investments = txs.stream()
            .map(this::createInvestment)
            .collect(Collectors.toList());

        this.builder = builder != null ? builder : NewtonRaphson.builder();
        this.guess = guess;
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
        final double years = DAYS.between(details.start, details.end) / DAYS_IN_YEAR;
        if (details.maxAmount == 0) {
            return -1; // Total loss
        }
        guess = guess != null ? guess : (details.total / details.deposits) / years;
        return builder.withFunction(this::presentValue)
            .withDerivative(this::derivative)
            .findRoot(guess);
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
            if (-1 < rate) {
                return amount * Math.pow(1 + rate, years);
            } else if (rate < -1) {
                // Extend the function into the range where the rate is less
                // than -100%.  Even though this does not make practical sense,
                // it allows the algorithm to converge in the cases where the
                // candidate values enter this range

                // We cannot use the same formula as before, since the base of
                // the exponent (1+rate) is negative, this yields imaginary
                // values for fractional years.
                // E.g. if rate=-1.5 and years=.5, it would be (-.5)^.5,
                // i.e. the square root of negative one half.

                // Ensure the values are always negative so there can never
                // be a zero (as long as some amount is non-zero).
                // This formula also ensures that the derivative is positive
                // (when rate < -1) so that Newton's method is encouraged to
                // move the candidate values towards the proper range

                return -Math.abs(amount) * Math.pow(-1 - rate, years);
            } else if (years == 0) {
                return amount; // Resolve 0^0 as 0
            } else {
                return 0;
            }
        }

        /**
         * Derivative of the present value of the investment at the given rate.
         * @param rate the rate of return
         * @return derivative of the present value at the given rate
         */
        private double derivative(final double rate) {
            if (years == 0) {
                return 0;
            } else if (-1 < rate) {
                return amount * years * Math.pow(1 + rate, years - 1);
            } else if (rate < -1) {
                return Math.abs(amount) * years * Math.pow(-1 - rate, years - 1);
            } else {
                return 0;
            }
        }
    }

    /**
     * Builder for {@link Xirr} instances.
     */
    public static class Builder {
        private Collection<Transaction> transactions = null;
        private NewtonRaphson.Builder builder = null;
        private Double guess = null;

        public Builder() {
        }

        public Builder withTransactions(Transaction... txs) {
            return withTransactions(Arrays.asList(txs));
        }

        public Builder withTransactions(Collection<Transaction> txs) {
            this.transactions = txs;
            return this;
        }

        public Builder withNewtonRaphsonBuilder(NewtonRaphson.Builder builder) {
            this.builder = builder;
            return this;
        }

        public Builder withGuess(double guess) {
            this.guess = guess;
            return this;
        }

        public Xirr build() {
            return new Xirr(transactions, builder, guess);
        }

        /**
         * Convenience method for building the Xirr instance and invoking
         * {@link Xirr#xirr()}.  See the documentation for that method for
         * details.
         * @return the irregular rate of return of the transactions
         */
        public double xirr() {
            return build().xirr();
        }
    }

}
