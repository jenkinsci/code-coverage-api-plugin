package io.jenkins.plugins.coverage;

import io.jenkins.plugins.coverage.model.CoverageMetric;

public class QualityGate {

    private final float threshold;
    private final CoverageMetric coverageMetric;

    public QualityGate(final float threshold, final CoverageMetric coverageMetric) {
        this.threshold = threshold;
        this.coverageMetric = coverageMetric;
    }
}
