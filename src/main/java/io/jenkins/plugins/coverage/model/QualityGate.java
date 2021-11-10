package io.jenkins.plugins.coverage.model;

/**
 * wird vom Benutzer erfasst und dient dazu die Qualitätsanforderungen an den Build zu erfassen (failed, warning,
 * successful, inactive)
 */
public class QualityGate {
    private CoverageMetric coverageMetric;
    private double warningLimit;
    private double failedLimit;

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