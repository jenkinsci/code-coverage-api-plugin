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
     * Creates an instance of {@link CoveragePercentage} from a {@link Fraction fraction}.
     *
     * @param fraction
     *         The coverage as fraction
     *
     * @return the created instance
     */
    public static CoveragePercentage getCoveragePercentage(final Fraction fraction) {
        Fraction percentage = fraction.multiplyBy(Fraction.getFraction("100.0"));
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
    public static CoveragePercentage getCoveragePercentage(final double percentage) {
        Fraction percentageFraction = Fraction.getFraction(percentage);
        return new CoveragePercentage(percentageFraction.getNumerator(), percentageFraction.getDenominator());
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