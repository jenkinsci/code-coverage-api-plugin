package io.jenkins.plugins.coverage;

import io.jenkins.plugins.coverage.model.CoverageMetric;

public class QualityGate {

    private final float threshold;
    private final CoverageMetric coverageMetric;
    private final QualityGateStatus status;

    public QualityGate(final float threshold, final CoverageMetric coverageMetric, final boolean unstable) {
        this.threshold = threshold;
        this.coverageMetric = coverageMetric;
        this.status = unstable ? QualityGateStatus.WARNING : QualityGateStatus.FAILED;
    }
}
