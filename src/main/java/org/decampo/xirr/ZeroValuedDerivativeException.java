package org.decampo.xirr;

/**
 * Indicates that the numerical method employed encountered a zero-valued
 * derivative, terminating the algorithm unsuccessfully.
 * <p>
 * The state of the algorithm is available via the getters, to allow the caller
 * to adjust the guess and try again.
 * @author ray
 */
public class ZeroValuedDerivativeException extends OverflowException {

    ZeroValuedDerivativeException(NewtonRaphson.Calculation state) {
        super("Newton-Raphson failed due to zero-valued derivative.", state);
    }
}
