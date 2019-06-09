/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.decampo.xirr;

/**
 * Indicates the algorithm failed to converge in the allotted number of
 * iterations.
 * @author ray
 */
public class NonconvergenceException extends IllegalArgumentException {

    private final double initialGuess;
    private final long iterations;

    public NonconvergenceException(double guess, long iterations) {
        super("Newton-Raphson failed to converge within " + iterations
            + " iterations.");
        this.initialGuess = guess;
        this.iterations = iterations;
    }

    /**
     * Get the initial guess used for the algorithm.
     * @return the initial guess used for the algorithm
     */
    public double getInitialGuess() {
        return initialGuess;
    }

    /**
     * Get the number of iterations applied.
     * @return the number of iterations applied.
     */
    public long getIterations() {
        return iterations;
    }

}
