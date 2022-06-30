package io.jenkins.plugins.coverage.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.math.Fraction;

/**
 * Represents a coverage percentage value which can be used in order to show and serialize coverage values. The class
 * can also be used for transforming a coverage fraction into its percentage representation. The percentage is
 * represented by a numerator and a denominator.
 *
 * @author Florian Orendi
 */
public final class CoveragePercentage implements Serializable {

    private static final long serialVersionUID = 3324942976687883481L;

    static final String DENOMINATOR_ZERO_MESSAGE = "The denominator must not be zero";
    private static final Fraction HUNDRED = Fraction.getFraction("100.0");

    private final int numerator;
    private final int denominator;

    /**
     * Creates an instance of {@link CoveragePercentage}.
     *
     * @param numerator
     *         The numerator of the fraction which represents the percentage
     * @param denominator
     *         The denominator of the fraction which represents the percentage
     */
    private CoveragePercentage(final int numerator, final int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Creates an instance of {@link CoveragePercentage} from a {@link Fraction fraction} within the range [0,1].
     *
     * @param fraction
     *         The coverage as fraction
     *
     * @return the created instance
     */
    public static CoveragePercentage valueOf(final Fraction fraction) {
        Fraction percentage = new SafeFraction(fraction).multiplyBy(HUNDRED);
        return new CoveragePercentage(percentage.getNumerator(), percentage.getDenominator());
    }

    /**
     * Creates an instance of {@link CoveragePercentage} from a coverage percentage value.
     *
     * @param percentage
     *         The value which represents a coverage percentage
     *
     * @return the created instance
     */
    public static CoveragePercentage valueOf(final double percentage) {
        Fraction percentageFraction = Fraction.getFraction(percentage);
        return new CoveragePercentage(percentageFraction.getNumerator(), percentageFraction.getDenominator());
    }

    /**
     * Creates an instance of {@link CoveragePercentage} from a numerator and a denominator.
     *
     * @param numerator
     *         The numerator of the fraction which represents the percentage within the range [0,100]
     * @param denominator
     *         The denominator of the fraction which represents the percentage within the range [0,100] (must not be
     *         zero)
     *
     * @return the created instance
     * @throws IllegalArgumentException
     *         if the denominator is zero
     */
    public static CoveragePercentage valueOf(final int numerator, final int denominator) {
        if (denominator != 0) {
            return new CoveragePercentage(numerator, denominator);
        }
        throw new IllegalArgumentException(DENOMINATOR_ZERO_MESSAGE);
    }

    /**
     * Calculates the coverage percentage.
     *
     * @return the coverage percentage
     */
    public double getDoubleValue() {
        return (double) numerator / denominator;
    }

    /**
     * Formats a percentage to plain text and rounds the value to two decimals.
     *
     * @param locale
     *         The used locale
     *
     * @return the formatted percentage as plain text
     */
    public String formatPercentage(final Locale locale) {
        return String.format(locale, "%.2f%%", getDoubleValue());
    }

    /**
     * Formats a delta percentage to its plain text representation with a leading sign and rounds the value to two
     * decimals.
     *
     * @param locale
     *         The used locale
     *
     * @return the formatted delta percentage as plain text with a leading sign
     */
    public String formatDeltaPercentage(final Locale locale) {
        return String.format(locale, "%+.2f%%", getDoubleValue());
    }

    public int getNumerator() {
        return numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoveragePercentage that = (CoveragePercentage) o;
        return numerator == that.numerator && denominator == that.denominator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }
}
