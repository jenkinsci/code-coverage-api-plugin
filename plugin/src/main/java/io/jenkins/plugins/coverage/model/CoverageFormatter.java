package io.jenkins.plugins.coverage.model;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.IntegerValue;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.MutationValue;
import edu.hm.hafner.metric.Value;

/**
 * A formatter for coverages.
 *
 * @author Florian Orendi
 */
public final class CoverageFormatter {
    private static final Fraction HUNDRED = Fraction.getFraction("100.0");
    private static final String NO_COVERAGE_AVAILABLE = "-";

    /**
     * Formats a percentage to plain text and rounds the value to two decimals.
     *
     * @param locale
     *         The used locale
     *
     * @return the formatted percentage as plain text
     */
    public String formatPercentage(final Coverage coverage, final Locale locale) {
        if (coverage.isSet()) {
            return formatPercentage(coverage.getCoveredPercentage(), locale);
        }
        return NO_COVERAGE_AVAILABLE;
    }

    /**
     * Formats a percentage to plain text and rounds the value to two decimals.
     *
     * @param locale
     *         The used locale
     *
     * @return the formatted percentage as plain text
     */
    private String formatPercentage(final Fraction fraction, final Locale locale) {
        return String.format(locale, "%.2f%%", fraction.multiplyBy(HUNDRED).doubleValue());
    }

    /**
     * Formats a percentage to plain text and rounds the value to two decimals.
     *
     * @param locale
     *         The used locale
     *
     * @return the formatted percentage as plain text
     */
    public String formatPercentage(final int covered, final int total, final Locale locale) {
        return formatPercentage(Fraction.getFraction(covered, total), locale);
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
    public String formatDelta(final Metric metric, final Fraction fraction, final Locale locale) {
        if (metric.equals(Metric.COMPLEXITY) || metric.equals(Metric.LOC)) { // TODO: move to metric?
            return String.format(locale, "%+d", fraction.intValue());
        }
        return String.format(locale, "%+.2f%%", fraction.multiplyBy(HUNDRED).doubleValue());
    }

    public String format(final Value value, final Locale locale) {
        if (value instanceof Coverage) {
            return formatPercentage((Coverage) value, locale);
        }
        if (value instanceof MutationValue) {
            return formatPercentage(((MutationValue) value).getCoveredPercentage(), locale);
        }
        if (value instanceof IntegerValue) {
            return String.valueOf(((IntegerValue) value).getValue());
        }
        return value.toString();
    }
}
