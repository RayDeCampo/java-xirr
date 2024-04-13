/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.decampo.xirr;

import java.util.Arrays;
import org.junit.Test;

import static org.decampo.xirr.NewtonRaphson.TOLERANCE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XirrBuilderTest {

    @Test
    public void withTransactions_1_year_no_growth() {
        // computes the xirr on 1 year growth of 0%
        final double xirr = Xirr.builder()
            .withTransactions(Arrays.asList(
                new Transaction(-1000, "2010-01-01"),
                new Transaction( 1000, "2011-01-01")
            )).xirr();
        assertEquals(0, xirr, TOLERANCE);
    }

    @Test
    public void withTransactions_1_year_growth() {
        // computes the xirr on 1 year growth of 10%
        final double xirr = Xirr.builder()
            .withTransactions(
                new Transaction(-1000, "2010-01-01"),
                new Transaction( 1100, "2011-01-01")
            ).xirr();
        assertEquals(0.10, xirr, TOLERANCE);
    }

    @Test
    public void withTransactions_1_year_decline() {
        // computes the negative xirr on 1 year decline of 10%
        final double xirr = Xirr.builder()
            .withTransactions(
                new Transaction(-1000, "2010-01-01"),
                new Transaction(  900, "2011-01-01")
            ).xirr();
        assertEquals(-0.10, xirr, TOLERANCE);
    }

    @Test
    public void withTransactions_1_year_decline_360days() {
        // computes the negative xirr on 1 year decline of 10%
        final double xirr = Xirr.builder()
                .withTransactions(
                        new Transaction(-1000, "2010-01-01"),
                        new Transaction(  900, "2011-01-01")
                )
                .withDaysInYear(360)
                .xirr();
        assertEquals(-0.0987, xirr, TOLERANCE);
    }

    @Test
    public void withNewtonRaphsonBuilder() throws Exception {
        final double expected = 1;

        final NewtonRaphson.Builder builder = setUpNewtonRaphsonBuilder();
        when(builder.findRoot(anyDouble())).thenReturn(expected);

        final double xirr = Xirr.builder()
            .withNewtonRaphsonBuilder(builder)
            .withTransactions(
                new Transaction(-1000, "2010-01-01"),
                new Transaction( 1000, "2011-01-01")
            ).xirr();

        // Correct answer is 0, but we are ensuring that Xirr is using the
        // builder we supplied
        assertEquals(expected, xirr, 0);
    }

    @Test
    public void withGuess() {
        final double expected = 1;
        final double guess = 3;

        final NewtonRaphson.Builder builder = setUpNewtonRaphsonBuilder();
        when(builder.findRoot(guess)).thenReturn(expected);

        final double xirr = Xirr.builder()
            .withGuess(guess)
            .withNewtonRaphsonBuilder(builder)
            .withTransactions(
                new Transaction(-1000, "2010-01-01"),
                new Transaction( 1000, "2011-01-01")
            ).xirr();

        // Correct answer is 0, but we are ensuring that Xirr is using the
        // builder we supplied
        assertEquals(expected, xirr, 0);
    }

    private NewtonRaphson.Builder setUpNewtonRaphsonBuilder()
    {
        final NewtonRaphson.Builder builder = mock(NewtonRaphson.Builder.class);
        when(builder.withFunction(any())).thenReturn(builder);
        when(builder.withDerivative(any())).thenReturn(builder);
        return builder;
    }

}
