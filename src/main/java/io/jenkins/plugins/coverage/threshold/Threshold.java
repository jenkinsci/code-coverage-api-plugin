package io.jenkins.plugins.coverage.threshold;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class Threshold implements ExtensionPoint, Describable<Threshold> {

    private final CoverageMetric threshTarget;


    // healthy when coverage large than healthy thresh
    private float healthyThresh = 80.0f;

    // unstable when coverage less than unstable thresh
    private float unstableThresh = 0.0f;

    // unhealthy when coverage less than unhealthy thresh
    private float unhealthyThresh = 0.0f;

    @DataBoundConstructor
    public Threshold(CoverageMetric threshTarget) {
        this.threshTarget = threshTarget;
    }


    public CoverageMetric getThreshTarget() {
        return threshTarget;
    }

    public float getHealthyThresh() {
        return healthyThresh;
    }

    @DataBoundSetter
    public void setHealthyThresh(float healthyThresh) {
        this.healthyThresh = healthyThresh;
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
