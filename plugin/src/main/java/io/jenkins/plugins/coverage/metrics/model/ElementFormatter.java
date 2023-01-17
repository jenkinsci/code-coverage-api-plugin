package io.jenkins.plugins.coverage.metrics.model;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FractionValue;
import edu.hm.hafner.metric.IntegerValue;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;

import hudson.util.ListBoxModel;

/**
 * A localized formatter for coverages, metrics, baselines, etc.
 *
 * @author Florian Orendi
 */
public final class ElementFormatter {
    private static final Fraction HUNDRED = Fraction.getFraction("100.0");
    private static final String NO_COVERAGE_AVAILABLE = "-";

    /**
     * Formats a generic value using a specific rendering method. The type of the given {@link Value} instance is used
     * to select the best matching rendering method. This non-object-oriented approach is required since the
     * {@link Value} instances are provided by a library that is not capable of localizing these values for the user.
     *
     * @param value
     *         the value to format
     * @param locale
     *         the locale to use to render the values
     *
     * @return the formatted value as plain text
     */
    public String format(final Value value, final Locale locale) {
        if (value instanceof Coverage) {
            return formatPercentage((Coverage) value, locale);
        }
        if (value instanceof IntegerValue) {
            return String.valueOf(((IntegerValue) value).getValue());
        }
        if (value instanceof FractionValue) {
            return formatDelta(((FractionValue) value).getFraction(), value.getMetric(), locale);
        }
        return value.toString();
    }

    /**
     * Formats a generic value using a specific rendering method. The type of the given {@link Value} instance is used
     * to select the best matching rendering method. This non-object-oriented approach is required since the
     * {@link Value} instances are provided by a library that is not capable of localizing these values for the user.
     *
     * @param value
     *         the value to format
     * @param locale
     *         the locale to use to render the values
     *
     * @return the formatted value as plain text
     */
    public String formatDetails(final Value value, final Locale locale) {
        if (value instanceof Coverage) {
            var coverage = (Coverage) value;
            return formatPercentage(coverage, locale)
                    + formatRatio(coverage.getCovered(), coverage.getTotal());
        }
        if (value instanceof IntegerValue) {
            return String.valueOf(((IntegerValue) value).getValue());
        }
        if (value instanceof FractionValue) {
            return String.format(locale, "%.2f%%", ((FractionValue) value).getFraction().doubleValue());
        }
        return value.toString();
    }

    private String formatRatio(final int covered, final int total) {
        if (total > 0) {
            return String.format(" (%d/%d)", covered, total);
        }
        return StringUtils.EMPTY;
    }

    /**
     * Formats a coverage as a percentage number. The shown value is multiplied by 100 and rounded by two decimals.
     *
     * @param coverage
     *         the coverage to format
     * @param locale
     *         the locale to use to render the values
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
     * Formats a fraction in the interval [0, 1] as a percentage number. The shown value is multiplied by 100 and
     * rounded by two decimals.
     *
     * @param fraction
     *         the fraction to format (in the interval [0, 1])
     * @param locale
     *         the locale to use to render the values
     *
     * @return the formatted percentage as plain text
     */
    private String formatPercentage(final Fraction fraction, final Locale locale) {
        return String.format(locale, "%.2f%%", fraction.multiplyBy(HUNDRED).doubleValue());
    }

    /**
     * Formats a coverage given by covered and total elements as a percentage number. The shown value is multiplied by
     * 100 and * rounded by two decimals.
     *
     * @param covered
     *         the number of covered items
     * @param total
     *         the number of total items
     * @param locale
     *         the locale to use to render the values
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
     * @param fraction
     *         the value of the delta
     * @param metric
     *         the metric of the value
     * @param locale
     *         the locale to use to render the values
     *
     * @return the formatted delta percentage as plain text with a leading sign
     */
    public String formatDelta(final Fraction fraction, final Metric metric, final Locale locale) {
        if (metric.equals(Metric.COMPLEXITY) || metric.equals(Metric.LOC)) { // TODO: move to metric?
            return String.format(locale, "%+d", fraction.intValue());
        }
        return String.format(locale, "%+.2f%%", fraction.multiplyBy(HUNDRED).doubleValue());
    }

    /**
     * Returns a localized human-readable name for the specified metric.
     *
     * @param metric
     *         the metric to get the name for
     *
     * @return the display name
     */
    public String getDisplayName(final Metric metric) {
        switch (metric) {
            case CONTAINER:
                return Messages.Metric_CONTAINER();
            case MODULE:
                return Messages.Metric_MODULE();
            case PACKAGE:
                return Messages.Metric_PACKAGE();
            case FILE:
                return Messages.Metric_FILE();
            case CLASS:
                return Messages.Metric_CLASS();
            case METHOD:
                return Messages.Metric_METHOD();
            case LINE:
                return Messages.Metric_LINE();
            case BRANCH:
                return Messages.Metric_BRANCH();
            case INSTRUCTION:
                return Messages.Metric_INSTRUCTION();
            case MUTATION:
                return Messages.Metric_MUTATION();
            case COMPLEXITY:
                return Messages.Metric_COMPLEXITY();
            case COMPLEXITY_DENSITY:
                return Messages.Metric_COMPLEXITY_DENSITY();
            case LOC:
                return Messages.Metric_LOC();
            default:
                throw new NoSuchElementException("No display name found for metric " + metric);
        }
    }

    /**
     * Returns a localized human-readable name for the specified baseline.
     *
     * @param baseline
     *         the baseline to get the name for
     *
     * @return the display name
     */
    public String getDisplayName(final Baseline baseline) {
        switch (baseline) {
            case PROJECT:
                return Messages.Baseline_PROJECT();
            case MODIFIED_LINES:
                return Messages.Baseline_MODIFIED_LINES();
            case MODIFIED_FILES:
                return Messages.Baseline_MODIFIED_FILES();
            case PROJECT_DELTA:
                return Messages.Baseline_PROJECT_DELTA();
            case MODIFIED_LINES_DELTA:
                return Messages.Baseline_MODIFIED_LINES_DELTA();
            case MODIFIED_FILES_DELTA:
                return Messages.Baseline_MODIFIED_FILES_DELTA();
            default:
                throw new NoSuchElementException("No display name found for baseline " + baseline);
        }
    }

    /**
     * Returns all available metrics as a {@link ListBoxModel}.
     *
     * @return the metrics in a {@link ListBoxModel}
     */
    public ListBoxModel getMetricItems() {
        ListBoxModel options = new ListBoxModel();
        add(options, Metric.MODULE);
        add(options, Metric.PACKAGE);
        add(options, Metric.FILE);
        add(options, Metric.CLASS);
        add(options, Metric.METHOD);
        add(options, Metric.LINE);
        add(options, Metric.BRANCH);
        add(options, Metric.INSTRUCTION);
        add(options, Metric.MUTATION);
        add(options, Metric.COMPLEXITY);
        add(options, Metric.LOC);
        return options;
    }

    private void add(final ListBoxModel options, final Metric metric) {
        options.add(getDisplayName(metric), metric.name());
    }

    /**
     * Returns all available baselines as a {@link ListBoxModel}.
     *
     * @return the baselines in a {@link ListBoxModel}
     */
    public ListBoxModel getBaselineItems() {
        ListBoxModel options = new ListBoxModel();
        add(options, Baseline.PROJECT);
        add(options, Baseline.MODIFIED_LINES);
        add(options, Baseline.MODIFIED_FILES);
        add(options, Baseline.PROJECT_DELTA);
        add(options, Baseline.MODIFIED_LINES_DELTA);
        add(options, Baseline.MODIFIED_FILES_DELTA);
        return options;
    }

    private void add(final ListBoxModel options, final Baseline baseline) {
        options.add(getDisplayName(baseline), baseline.name());
    }
}
