package io.jenkins.plugins.coverage.model;

import org.apache.commons.lang3.math.Fraction;

/**
 * A small wrapper for {@link Fraction} instances that avoids an arithmetic overflow by using double based operations in
 * case of an exception.
 *
 * @author Ullrich Hafner
 */
public class SafeFraction {
    private final Fraction fraction;

    /**
     * Creates a new fraction instance that wraps the specified fraction with safe operations.
     *
     * @param fraction
     *         the fraction to wrap
     */
    public SafeFraction(final Fraction fraction) {
        this.fraction = fraction;
    }

    /**
     * Multiplies the value of this fraction by another, returning the result in reduced form. Since there might be an
     * arithmetic exception due to an overflow, this method will handle this situation by calculating the multiplication
     * based on the double values of the fractions.
     *
     * @param multiplicator
     *         the fraction to multiply by
     *
     * @return a {@code Fraction} instance with the resulting values
     */
    public Fraction multiplyBy(final Fraction multiplicator) {
        try {
            return fraction.multiplyBy(multiplicator);
        }
        catch (ArithmeticException exception) {
            return Fraction.getFraction(fraction.doubleValue() * multiplicator.doubleValue());
        }
    }

    /**
     * Subtracts the value of another fraction from the value of this one, returning the result in reduced form.  Since
     * there might be an arithmetic exception due to an overflow, this method will handle this situation by calculating
     * the subtraction based on the double values of the fractions.
     *
     * @param subtrahend
     *         the fraction to subtract
     *
     * @return a {@code Fraction} instance with the resulting values
     */
    public Fraction subtract(final Fraction subtrahend) {
        try {
            return fraction.subtract(subtrahend);
        }
        catch (ArithmeticException exception) {
            return Fraction.getFraction(fraction.doubleValue() - subtrahend.doubleValue());
        }
    }

    /**
     * Adds the value of another fraction to the value of this one, returning the result in reduced form.  Since
     * there might be an arithmetic exception due to an overflow, this method will handle this situation by calculating
     * the addition based on the double values of the fractions.
     *
     * @param summand
     *         the fraction to add
     *
     * @return a {@code Fraction} instance with the resulting values
     */
    public Fraction add(final Fraction summand) {
        try {
            return fraction.add(summand);
        }
        catch (ArithmeticException exception) {
            return Fraction.getFraction(fraction.doubleValue() + summand.doubleValue());
        }
    }
}
