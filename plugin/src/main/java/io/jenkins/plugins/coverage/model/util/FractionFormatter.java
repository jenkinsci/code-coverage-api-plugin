package io.jenkins.plugins.coverage.model.util;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;

/**
 * Formats fraction and percentage values represented by {@link Fraction} and provides these values as formatted
 * percentages dependent on the use case.
 *
 * @author Florian Orendi
 */
public class FractionFormatter {

    private static final Fraction HUNDRED = Fraction.getFraction(100, 1);

    private FractionFormatter() {
        // prevents instantiation
    }

    /**
     * Transforms a fraction within the range [0;1] to a percentage value within the range [0;100].
     *
     * @param fraction
     *         The fraction to be transformed
     *
     * @return the fraction as percentage
     */
    public static Fraction transformFractionToPercentage(final Fraction fraction) {
        return fraction.multiplyBy(HUNDRED);
    }

    /**
     * Formats a percentage to plain text and rounds the value to two decimals.
     *
     * @param percentage
     *         The percentage to be formatted
     * @param locale
     *         The used locale
     *
     * @return the formatted percentage as plain text
     */
    public static String formatPercentage(final Fraction percentage, final Locale locale) {
        return String.format(locale, "%.2f%%", percentage.doubleValue());
    }

    /**
     * Formats a delta fraction to its plain text percentage representation with a leading sign and rounds the value to
     * two decimals.
     *
     * @param fraction
     *         The fraction to be formatted
     * @param locale
     *         The used locale
     *
     * @return the formatted delta fraction as plain text with a leading sign
     */
    public static String formatDeltaFraction(final Fraction fraction, final Locale locale) {
        return String.format(locale, "%+.2f%%", transformFractionToPercentage(fraction).doubleValue());
    }

    /**
     * Formats a delta percentage to its plain text representation with a leading sign and rounds the value to two
     * decimals.
     *
     * @param percentage
     *         The percentage to be formatted
     * @param locale
     *         The used locale
     *
     * @return the formatted delta percentage as plain text with a leading sign
     */
    public static String formatDeltaPercentage(final Fraction percentage, final Locale locale) {
        return String.format(locale, "%+.2f%%", percentage.doubleValue());
    }
}
