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

    private final CoverageMetric threshTarget;

    // mark build as unstable when coverage is less than this
    private float unstableThresh = 0.0f;

    // used for calculate healthy scores
    private float unhealthyThresh = 0.0f;

    private boolean failUnhealthy = false;

    @DataBoundConstructor
    public Threshold(CoverageMetric threshTarget) {
        this.threshTarget = threshTarget;
    }


    public CoverageMetric getThreshTarget() {
        return threshTarget;
    }

    public float getUnstableThresh() {
        return unstableThresh;
    }

    @DataBoundSetter
    public void setUnstableThresh(float unstableThresh) {
        this.unstableThresh = unstableThresh;
    }

    public float getUnhealthyThresh() {
        return unhealthyThresh;
    }

    @DataBoundSetter
    public void setUnhealthyThresh(float unhealthyThresh) {
        this.unhealthyThresh = unhealthyThresh;
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
        return getThreshTarget() == threshold.getThreshTarget();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getThreshTarget());
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
    }
}
