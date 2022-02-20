package io.jenkins.plugins.coverage.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A leaf in the coverage hierarchy. A leaf is a non-divisible coverage metric like line or branch coverage.
 *
 * @author Ullrich Hafner
 */
public final class CoverageLeaf implements Serializable {
    private static final long serialVersionUID = -1062406664372222691L;

    private final CoverageMetric metric;
    private final Coverage coverage;

    /**
     * Creates a new leaf with the given coverage for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param coverage
     *         the coverage of the element
     */
    public CoverageLeaf(final CoverageMetric metric, final Coverage coverage) {
        this.metric = metric;
        this.coverage = coverage;
    }

    public CoverageMetric getMetric() {
        return metric;
    }

    /**
     * Returns the coverage for the specified metric.
     *
     * @param searchMetric
     *         the metric to get the coverage for
     *
     * @return coverage ratio
     */
    public Coverage getCoverage(final CoverageMetric searchMetric) {
        if (metric.equals(searchMetric)) {
            return coverage;
        }
        return Coverage.NO_COVERAGE;
    }

    public CoverageLeaf copyLeaf() {
        return new CoverageLeaf(metric, coverage.copy());
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", metric, coverage);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CoverageLeaf that = (CoverageLeaf) o;
        return Objects.equals(metric, that.metric) && Objects.equals(coverage, that.coverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric, coverage);
    }
}
