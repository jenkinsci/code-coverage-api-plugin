package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;

public class GlobalThreshold extends AbstractThreshold {

    public GlobalThreshold(final CoveragePublisher coveragePublisher, final String path) {
        super(coveragePublisher, path);
    }

    public void setThresholdTarget(GlobalThresholdTarget globalThreshold) {
        this.thresholdTarget.set(globalThreshold.getName());

    }
}

