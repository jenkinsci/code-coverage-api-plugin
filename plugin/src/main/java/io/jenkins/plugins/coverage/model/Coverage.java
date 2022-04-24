package io.jenkins.plugins.coverage.model;

import java.io.Serializable;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.VisibleForTesting;

/**
 * Value of a code coverage item. The code coverage is measured using the number of covered and missed items. The type
 * of items (line, instruction, branch, file, etc.) is provided by the companion class {@link CoverageMetric}.
 *
 * @author Ullrich Hafner
 */
public final class Coverage implements Serializable {
    private static final long serialVersionUID = -3802318446471137305L;

    private static final Fraction HUNDRED = Fraction.getFraction(100, 1);

    private final int covered;
    private final int missed;

    /**
     * Creates a new {@link Coverage} instance from the provided string representation. The string representation is
     * expected to contain the number of covered items and the total number of items - separated by a slash, e.g.
     * "100/345", or "0/0". Whitespace characters will be ignored.
     *
     * @param stringRepresentation
     *         string representation to convert from
     *
     * @return the created coverage
     * @throws IllegalArgumentException
     *         if the string is not a valid Coverage instance
     */
    public static Coverage valueOf(final String stringRepresentation) {
        try {
            String cleanedFormat = StringUtils.deleteWhitespace(stringRepresentation);
            if (StringUtils.contains(cleanedFormat, "/")) {
                String extractedCovered = StringUtils.substringBefore(cleanedFormat, "/");
                String extractedTotal = StringUtils.substringAfter(cleanedFormat, "/");

                int covered = Integer.parseInt(extractedCovered);
                int total = Integer.parseInt(extractedTotal);
                if (total >= covered) {
                    return new CoverageBuilder().setCovered(covered).setMissed(total - covered).build();
                }
            }
        }
        catch (NumberFormatException exception) {
            // ignore and throw a specific exception
        }
        throw new IllegalArgumentException(
                String.format("Cannot convert %s to a valid Coverage instance.", stringRepresentation));
    }

    /**
     * Creates a new code coverage with the specified values.
     *
     * @param covered
     *         the number of covered items
     * @param missed
     *         the number of missed items
     */
    private Coverage(final int covered, final int missed) {
        this.covered = covered;
        this.missed = missed;
    }

    /**
     * Returns the number of covered items.
     *
     * @return the number of covered items
     */
    public int getCovered() {
        return covered;
    }

    /**
     * Returns the covered percentage as a {@link Fraction} in the range of {@code [0, 1]}.
     *
     * @return the covered percentage
     */
    public Fraction getCoveredPercentage() {
        if (getTotal() == 0) {
            return Fraction.ZERO;
        }
        return Fraction.getFraction(covered, getTotal());
    }

    /**
     * Returns the covered percentage as rounded integer value in the range of {@code [0, 100]}.
     *
     * @return the covered percentage
     */
    // TODO: we should make the charts accept float values
    public int getRoundedPercentage() {
        if (getTotal() == 0) {
            return 0;
        }
        return (int) Math.round(getCoveredPercentage().multiplyBy(Coverage.HUNDRED).doubleValue());
    }

    /**
     * Formats the covered percentage as String (with a precision of two digits after the comma). Uses
     * {@code Locale.getDefault()} to format the percentage.
     *
     * @return the covered percentage
     * @see #formatCoveredPercentage(Locale)
     */
    public String formatCoveredPercentage() {
        return formatCoveredPercentage(Locale.getDefault());
    }

    /**
     * Formats the covered percentage as String (with a precision of two digits after the comma).
     *
     * @param locale
     *         the locale to use when formatting the percentage
     *
     * @return the covered percentage
     */
    public String formatCoveredPercentage(final Locale locale) {
        return printPercentage(locale, getCoveredPercentage());
    }

    /**
     * Returns the number of missed items.
     *
     * @return the number of missed items
     */
    public int getMissed() {
        return missed;
    }

    /**
     * Returns the missed percentage as a {@link Fraction} in the range of {@code [0, 1]}.
     *
     * @return the missed percentage
     */
    public Fraction getMissedPercentage() {
        if (getTotal() == 0) {
            return Fraction.ZERO;
        }
        return Fraction.ONE.subtract(getCoveredPercentage());
    }

    /**
     * Formats the missed percentage as formatted String (with a precision of two digits after the comma). Uses
     * {@code Locale.getDefault()} to format the percentage.
     *
     * @return the missed percentage
     */
    public String formatMissedPercentage() {
        return formatMissedPercentage(Locale.getDefault());
    }

    /**
     * Formats the missed percentage as formatted String (with a precision of two digits after the comma).
     *
     * @param locale
     *         the locale to use when formatting the percentage
     *
     * @return the missed percentage
     */
    public String formatMissedPercentage(final Locale locale) {
        return printPercentage(locale, getMissedPercentage());
    }

    private String printPercentage(final Locale locale, final Fraction percentage) {
        if (isSet()) {
            return String.format(locale, "%.2f%%", percentage.multiplyBy(HUNDRED).doubleValue());
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Add the coverage details from the specified instance to the coverage details of this instance.
     *
     * @param additional
     *         the additional coverage details
     *
     * @return the sum of this and the additional coverage
     */
    public Coverage add(final Coverage additional) {
        return new CoverageBuilder().setCovered(covered + additional.getCovered())
                .setMissed(missed + additional.getMissed())
                .build();
    }

    @Override
    public String toString() {
        int total = getTotal();
        if (total > 0) {
            return String.format("%s (%s)", formatCoveredPercentage(), getCoveredPercentage());
        }
        return Messages.Coverage_Not_Available();
    }

    public int getTotal() {
        return missed + covered;
    }

    public boolean isSet() {
        return getTotal() > 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Coverage coverage = (Coverage) o;

        if (covered != coverage.covered) {
            return false;
        }
        return missed == coverage.missed;
    }

    @Override
    public int hashCode() {
        int result = covered;
        result = 31 * result + missed;
        return result;
    }

    /**
     * Returns a string representation for this {@link Coverage} that can be used to serialize this instance in a simple
     * but still readable way. The serialization contains the number of covered items and the total number of items -
     * separated by a slash, e.g. "100/345", or "0/0".
     *
     * @return a string representation for this {@link Coverage}
     */
    public String serializeToString() {
        return String.format("%d/%d", getCovered(), getTotal());
    }

    /**
     * Builder to create an cache new {@link Coverage} instances.
     */
    public static class CoverageBuilder {
        @VisibleForTesting
        static final int CACHE_SIZE = 16;
        private static final Coverage[] CACHE = new Coverage[CACHE_SIZE * CACHE_SIZE];

        static {
            for (int covered = 0; covered < CACHE_SIZE; covered++) {
                for (int missed = 0; missed < CACHE_SIZE; missed++) {
                    CACHE[getCacheIndex(covered, missed)] = new Coverage(covered, missed);
                }
            }
        }

        private static int getCacheIndex(final int covered, final int missed) {
            return covered * CACHE_SIZE + missed;
        }

        /** Null object that indicates that the code coverage has not been measured. */
        public static final Coverage NO_COVERAGE = CACHE[0];

        private int covered;
        private boolean isCoveredSet;
        private int missed;
        private boolean isMissedSet;
        private int total;
        private boolean isTotalSet;

        /**
         * Sets the number of total items.
         *
         * @param total
         *         the number of total items
         *
         * @return this
         */
        public CoverageBuilder setTotal(final int total) {
            this.total = total;
            isTotalSet = true;
            return this;
        }

        /**
         * Sets the number of covered items.
         *
         * @param covered
         *         the number of covered items
         *
         * @return this
         */
        public CoverageBuilder setCovered(final int covered) {
            this.covered = covered;
            isCoveredSet = true;
            return this;
        }

        /**
         * Sets the number of missed items.
         *
         * @param missed
         *         the number of missed items
         *
         * @return this
         */
        public CoverageBuilder setMissed(final int missed) {
            this.missed = missed;
            isMissedSet = true;
            return this;
        }

        /**
         * Creates the new {@link Coverage} instance.
         *
         * @return the new instance
         */
        @SuppressWarnings("PMD.CyclomaticComplexity")
        public Coverage build() {
            if (isCoveredSet && isMissedSet && isTotalSet) {
                throw new IllegalArgumentException(
                        "Setting all three values covered, missed, and total is not allowed, just select two of them.");
            }
            if (isTotalSet) {
                if (isCoveredSet) {
                    return createOrGetCoverage(covered, total - covered);
                }
                else if (isMissedSet) {
                    return createOrGetCoverage(total - missed, missed);
                }
            }
            else {
                if (isCoveredSet && isMissedSet) {
                    return createOrGetCoverage(covered, missed);
                }
            }
            throw new IllegalArgumentException("You must set exactly two properties.");
        }

        @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
        private Coverage createOrGetCoverage(final int covered, final int missed) {
            if (covered < CACHE_SIZE && missed < CACHE_SIZE) {
                return CACHE[getCacheIndex(covered, missed)];
            }
            return new Coverage(covered, missed);
        }
    }
}
