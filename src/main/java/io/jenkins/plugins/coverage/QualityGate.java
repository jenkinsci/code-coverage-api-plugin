package io.jenkins.plugins.coverage;

import io.jenkins.plugins.coverage.model.CoverageMetric;

public class QualityGate {

    private Double warningThreshold;
    private Double failedThreshold;
    private CoverageMetric coverageMetric;

    public QualityGate(final CoverageMetric coverageMetric,final double warningThreshold, final double failedThreshold){
        this.coverageMetric = coverageMetric;
        this.warningThreshold = warningThreshold;
        this.failedThreshold = failedThreshold;
    }

    public Double getWarningThreshold() {
        return warningThreshold;
    }

    public Double getFailedThreshold() {
        return failedThreshold;
    }

    public CoverageMetric getCoverageMetric() {
        return coverageMetric;
    }
}
