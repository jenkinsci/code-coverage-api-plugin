package io.jenkins.plugins.coverage.Threshold;

import org.jenkinsci.test.acceptance.po.PageArea;
import org.jenkinsci.test.acceptance.po.PageObject;

import io.jenkins.plugins.coverage.CoveragePublisher.Adapter;

public class AdapterThreshold extends AbstractThreshold{
    protected AdapterThreshold(final PageObject context, final String path) {
        super(context, path);
    }

    public AdapterThreshold(final Adapter adapter,
            final String path) {
        super(adapter, path);
    }

    public void setThresholdTarget(final AdapterThresholdTarget target) {
        this.thresholdTarget.select(target.getName());
    }
}


