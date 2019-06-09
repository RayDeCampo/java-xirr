package org.decampo.xirr;

import org.junit.Test;

import static org.decampo.xirr.NewtonRaphson.TOLERANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NewtonRaphsonTest {

    @Test
    public void sqrt() throws Exception {
        NewtonRaphson nr = NewtonRaphson.builder()
            .withFunction(x -> x * x)
            .withDerivative(x -> 2 * x)
            .build();
        assertEquals(2, nr.inverse(4, 4), TOLERANCE);
        assertEquals(-3, nr.inverse(9, -9), TOLERANCE);
        assertEquals(25, nr.inverse(625, 625), TOLERANCE);
    }

    @Test
    public void cubeRoot() throws Exception {
        NewtonRaphson nr = NewtonRaphson.builder()
            .withFunction(x -> x * x * x)
            .withDerivative(x -> 3 * x * x)
            .build();
        assertEquals(2, nr.inverse(8, 8), TOLERANCE);
        assertEquals(-3, nr.inverse(-27, 27), TOLERANCE);
        assertEquals(25, nr.inverse(15_625, 15_625), TOLERANCE);
    }

    @Test
    public void quadratic() throws Exception {
        NewtonRaphson nr = NewtonRaphson.builder()
            .withFunction(x -> (x - 4) * (x + 3))
            .withDerivative(x -> 2 * x - 1)
            .build();
        assertEquals(4, nr.findRoot(10), TOLERANCE);
        assertEquals(-3, nr.findRoot(-10), TOLERANCE);
        // Inflection point when derivative is zero => x = 1/2
        assertEquals(4, nr.findRoot(.51), TOLERANCE);
        assertEquals(-3, nr.findRoot(.49), TOLERANCE);
    }

    @Test(expected = ZeroValuedDerivativeException.class)
    public void failToConverge() throws Exception {
        NewtonRaphson nr = NewtonRaphson.builder()
            .withFunction(x -> (x - 4) * (x + 3))
            .withDerivative(x -> 2 * x - 1)
            .build();
        // Inflection point when derivative is zero => x = 1/2
        nr.findRoot(.5);
        fail("Expected zero-valued derivative");
    }

    @Test()
    public void failToConverge_verifyDetails() throws Exception {
        try {
            // Use nonsense functions designed to cause a zero derivative
            // after one iteration
            NewtonRaphson nr = NewtonRaphson.builder()
                .withFunction(x -> 2)
                .withDerivative(x -> x > 0 ? .25 : 0)
                .build();
            nr.findRoot(3);
            fail("Expected non-convergence");
        } catch (ZeroValuedDerivativeException zvde) {
            assertEquals(3, zvde.getInitialGuess(), TOLERANCE);
            assertEquals(1, zvde.getIteration());
            assertEquals(-5, zvde.getCandidate(), TOLERANCE);
            assertEquals(2, zvde.getValue(), TOLERANCE);
        }
    }

    @Test(expected = NonconvergenceException.class)
    public void failToConverge_iterations() throws Exception {
        NewtonRaphson nr = NewtonRaphson.builder()
            .withFunction(x -> x * x)
            .withDerivative(x -> 1) // Wrong on purpose
            .build();
        nr.findRoot(Integer.MAX_VALUE);
        fail("Expected non-convergence");
    }

    @Test
    public void failToConverge_iterations_verifyDetails() throws Exception {
        try {
            NewtonRaphson nr = NewtonRaphson.builder()
                .withFunction(x -> x * x)
                .withDerivative(x -> 1) // Wrong on purpose
                .build();
            nr.findRoot(Integer.MAX_VALUE);
            fail("Expected non-convergence");
        } catch (NonconvergenceException ne) {
            assertEquals(Integer.MAX_VALUE, ne.getInitialGuess(), TOLERANCE);
            assertEquals(10_000L, ne.getIterations());
        }
    }

    @Test
    public void tolerance() throws Exception {
        final double tolerance = TOLERANCE/1000;
        NewtonRaphson nr = NewtonRaphson.builder()
            .withFunction(x -> x * x)
            .withDerivative(x -> 2 * x)
            .withTolerance(tolerance)
            .build();
        assertEquals(4, nr.inverse(16, 16), tolerance);
        assertEquals(15, nr.inverse(225, 225), tolerance);
        assertEquals(1.414_213_562_3, nr.inverse(2, 2), tolerance);
    }

}
