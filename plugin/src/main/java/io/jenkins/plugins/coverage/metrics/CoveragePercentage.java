package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.SafeFraction;

/**
 * Represents a coverage percentage value which can be used in order to show and serialize coverage values. The class
 * can also be used for transforming a coverage fraction into its percentage representation. The percentage is
 * represented by a numerator and a denominator.
 *
 * @author Florian Orendi
 */
// FIXME: not used anymore
public final class CoveragePercentage implements Serializable {
    private static final long serialVersionUID = 3324942976687883481L;

    static final String DENOMINATOR_ZERO_MESSAGE = "The denominator must not be zero";
    private static final Fraction HUNDRED = Fraction.getFraction("100.0");

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
        return new CoveragePercentage(numerator, denominator);
    }

    /**
     * Creates a new {@link CoveragePercentage} instance from the provided string representation. The string
     * representation is expected to contain the numerator and the denominator - separated by a slash, e.g. "500/345",
     * or "100/1". Whitespace characters will be ignored.
     *
     * @param stringRepresentation
     *         string representation to convert from
     *
     * @return the created {@link CoveragePercentage}
     * @throws IllegalArgumentException
     *         if the string is not a valid CoveragePercentage instance
     */
    public static CoveragePercentage valueOf(final String stringRepresentation) {
        try {
            String cleanedFormat = StringUtils.deleteWhitespace(stringRepresentation);
            if (StringUtils.contains(cleanedFormat, "/")) {
                String extractedNumerator = StringUtils.substringBefore(cleanedFormat, "/");
                String extractedDenominator = StringUtils.substringAfter(cleanedFormat, "/");

                int numerator = Integer.parseInt(extractedNumerator);
                int denominator = Integer.parseInt(extractedDenominator);
                return new CoveragePercentage(numerator, denominator);
            }
        }
        catch (NumberFormatException exception) {
            // ignore and throw a specific exception
        }
        throw new IllegalArgumentException(
                String.format("Cannot convert %s to a valid CoveragePercentage instance.", stringRepresentation));
    }

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
        if (denominator == 0) {
            throw new IllegalArgumentException(DENOMINATOR_ZERO_MESSAGE);
        }
        this.numerator = numerator;
        this.denominator = denominator;
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

    /**
     * Returns a string representation for this {@link CoveragePercentage} that can be used to serialize this instance
     * in a simple but still readable way. The serialization contains the numerator and the denominator - separated by a
     * slash, e.g. "100/345", or "0/1".
     *
     * @return a string representation for this {@link CoveragePercentage}
     */
    public String serializeToString() {
        return String.format("%d/%d", getNumerator(), getDenominator());
    }

    @Override
    public String toString() {
        return formatPercentage(Locale.ENGLISH);
    }
}
