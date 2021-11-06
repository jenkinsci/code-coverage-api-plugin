
package io.jenkins.plugins.coverage;

import io.jenkins.plugins.coverage.model.CoverageMetric;

public class QualityGate {

    private final int threshold;
    private final CoverageMetric metric;
    private final QualityGateStatus status = QualityGateStatus.INACTIVE;
    private final QualityGateStatus statusWhenNotPassed;


    public QualityGate(final int requiredPercentage, final CoverageMetric metric, final QualityGateStatus statusWhenNotPassed) {
        this.threshold = requiredPercentage;
        this.metric = metric;
        this.statusWhenNotPassed = statusWhenNotPassed;
    }
}
