package io.jenkins.plugins.coverage.CoveragePublisher.Threshold;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.Adapter;

/**
 * Threshold used in {@link Adapter} in {@CoveragePublisher}.
 */
public class AdapterThreshold extends AbstractThreshold {

    /**
     * Constructor of a Threshold used in {@link Adapter} in {@CoveragePublisher}.
     * @param adapter of threshold
     * @param path to threshold
     */
    public AdapterThreshold(final Adapter adapter, final String path) {
        super(adapter, path);
    }

    /**
     * Setter for target of Threshold using {@link AdapterThresholdTarget}.
     * @param adapterThresholdTarget of threshold
     */
    public void setThresholdTarget(final AdapterThresholdTarget adapterThresholdTarget) {
        this.thresholdTarget.select(adapterThresholdTarget.getValue());
    }
}


