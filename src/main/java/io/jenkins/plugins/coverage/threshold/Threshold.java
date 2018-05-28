package io.jenkins.plugins.coverage.threshold;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Objects;

public class Threshold implements ExtensionPoint, Describable<Threshold> {

    private final CoverageMetric thresholdTarget;

    // mark build as unstable when coverage is less than this
    private float unstableThreshold = 0.0f;

    // used for calculate healthy scores
    private float unhealthyThreshold = 0.0f;

    private boolean failUnhealthy = false;

    @DataBoundConstructor
    public Threshold(CoverageMetric thresholdTarget) {
        this.thresholdTarget = thresholdTarget;
    }


    public CoverageMetric getThresholdTarget() {
        return thresholdTarget;
    }

    public float getUnstableThreshold() {
        return unstableThreshold;
    }

    @DataBoundSetter
    public void setUnstableThreshold(float unstableThreshold) {
        this.unstableThreshold = unstableThreshold;
    }

    public float getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    @DataBoundSetter
    public void setUnhealthyThreshold(float unhealthyThreshold) {
        this.unhealthyThreshold = unhealthyThreshold;
    }

    public boolean isFailUnhealthy() {
        return failUnhealthy;
    }

    @DataBoundSetter
    public void setFailUnhealthy(boolean failUnhealthy) {
        this.failUnhealthy = failUnhealthy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Threshold threshold = (Threshold) o;
        return getThresholdTarget() == threshold.getThresholdTarget();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getThresholdTarget());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<Threshold> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static final class ThreshHoldDescriptor<T extends Threshold> extends Descriptor<Threshold> {

        public ThreshHoldDescriptor() {
            super(Threshold.class);
        }

        public CoverageMetric[] getAllCoverageMetrics() {
            return CoverageMetric.all();
        }

    }
}
