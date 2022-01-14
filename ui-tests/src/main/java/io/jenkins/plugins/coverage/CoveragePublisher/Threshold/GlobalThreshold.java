package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.Adapter;

/**
 * Global Threshold used in {@CoveragePublisher}.
 */
public class GlobalThreshold extends AbstractThreshold {


    /**
     * Constructor of a Threshold used in {@link Adapter} in {@CoveragePublisher}.
     * @param coveragePublisher of threshold
     * @param path to threshold
     */
    public GlobalThreshold(final CoveragePublisher coveragePublisher, final String path) {
        super(coveragePublisher, path);
    }

    /**
     * Setter for target of Threshold using {@link AdapterThresholdTarget}.
     * @param globalThresholdTarget of threshold
     */
    public void setThresholdTarget(final GlobalThresholdTarget globalThresholdTarget) {
        this.thresholdTarget.set(globalThresholdTarget.getValue());

    }
}
