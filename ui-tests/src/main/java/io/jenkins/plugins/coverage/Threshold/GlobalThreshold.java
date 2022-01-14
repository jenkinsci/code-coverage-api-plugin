package io.jenkins.plugins.coverage.Threshold;

import io.jenkins.plugins.coverage.CoveragePublisher;

public class GlobalThreshold extends AbstractThreshold {

    public GlobalThreshold(final CoveragePublisher coveragePublisher, final String path) {
        super(coveragePublisher, path);
    }

    public void setThresholdTarget(GlobalThresholdTarget globalThreshold) {
        this.thresholdTarget.set(globalThreshold.getName());

    }
}

