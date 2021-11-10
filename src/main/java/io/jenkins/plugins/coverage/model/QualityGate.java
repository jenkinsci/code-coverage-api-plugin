package io.jenkins.plugins.coverage.model;

/**
 * Stores quality gate limits for specific metric.
 *
 * @author Adrian Germeck
 */
public class QualityGate {
    private final CoverageMetric coverageMetric;
    private final double warningLimit;
    private final double failedLimit;

    /**
     * Creates a quality gate.
     *
     * @param warningLimit
     *         coverage percentage of upper border for warning
     * @param failedLimit
     *         coverage percentage of upper border for fail
     * @param coverageMetric
     *         type of coverage metric
     */
    public QualityGate(final double warningLimit, final double failedLimit, final CoverageMetric coverageMetric) {
        if (warningLimit < failedLimit) {
            this.warningLimit = failedLimit;
            this.failedLimit = warningLimit;
        }
        else {
            this.warningLimit = warningLimit;
            this.failedLimit = failedLimit;
        }
        this.coverageMetric = coverageMetric;
    }

    public double getWarningLimit() {
        return warningLimit;
    }

    public double getFailedLimit() {
        return failedLimit;
    }

    public CoverageMetric getCoverageMetric() {
        return coverageMetric;
    }
}