package io.jenkins.plugins.coverage.model;

import java.io.Serializable;

import io.jenkins.plugins.coverage.targets.CoverageElement;

/**
 * Value of a code coverage item. The code coverage is measured using the number of covered and missed items. The type
 * of items (line, instruction, branch, file, etc.) is provided by the companion class {@link CoverageElement}.
 *
 * @author Ullrich Hafner
 */
public final class Coverage implements Serializable {
    private static final long serialVersionUID = -3802318446471137305L;

    /** Null object that indicates that the code coverage has not been measured. */
    public static final Coverage NO_COVERAGE = new Coverage(0, 0);

    static final String COVERAGE_NOT_AVAILABLE = "n/a";

    private final int covered;
    private final int missed;

    /**
     * Creates a new code coverage with the specified values.
     *
     * @param covered
     *         the number of covered items
     * @param missed
     *         the number of missed items
     */
    public Coverage(final int covered, final int missed) {
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
     * Returns the covered percentage in the range of {@code [0, 1]}.
     *
     * @return the covered percentage
     */
    public double getCoveredPercentage() {
        if (getTotal() == 0) {
            return 0;
        }
        return covered * 1.0 / getTotal();
    }

    /**
     * Prints the covered percentage as formatted String (with a precision of two digits after the comma).
     *
     * @return the covered percentage
     */
    public String printCoveredPercentage() {
        return printPercentage(getCoveredPercentage());
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
     * Returns the missed percentage in the range of {@code [0, 1]}.
     *
     * @return the missed percentage
     */
    public double getMissedPercentage() {
        if (getTotal() == 0) {
            return 0;
        }
        return 1 - getCoveredPercentage();
    }

    /**
     * Prints the missed percentage as formatted String (with a precision of two digits after the comma).
     *
     * @return the missed percentage
     */
    public String printMissedPercentage() {
        return printPercentage(getMissedPercentage());
    }

    private String printPercentage(final double percentage) {
        if (isSet()) {
            return String.format("%.2f%%", percentage * 100);
        }
        return COVERAGE_NOT_AVAILABLE;
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
        return new Coverage(covered + additional.getCovered(),
                missed + additional.getMissed());
    }

    @Override
    public String toString() {
        int total = getTotal();
        if (total > 0) {
            return String.format("%.2f (%d/%d)", getCoveredPercentage() * 100, covered, total);
        }
        return COVERAGE_NOT_AVAILABLE;
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
}
