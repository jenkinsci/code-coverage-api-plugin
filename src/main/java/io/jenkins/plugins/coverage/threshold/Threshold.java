package io.jenkins.plugins.coverage.threshold;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.targets.CoverageMetric;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class Threshold implements ExtensionPoint, Describable<Threshold> {
    private final CoverageMetric threshTarget;

    private final float healthyThresh;
    private final float unstableThresh;
    private final float unhealthyThresh;

    @DataBoundConstructor
    public Threshold(CoverageMetric threshTarget, float healthyThresh, float unstableThresh, float unhealthyThresh) {
        this.threshTarget = threshTarget;
        this.healthyThresh = healthyThresh;
        this.unstableThresh = unstableThresh;
        this.unhealthyThresh = unhealthyThresh;
    }

    public CoverageMetric getThreshTarget() {
        return threshTarget;
    }

    public float getHealthyThresh() {
        return healthyThresh;
    }

    public float getUnstableThresh() {
        return unstableThresh;
    }

    public float getUnhealthyThresh() {
        return unhealthyThresh;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<Threshold> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public static final class ThreshHoldDescriptor<T extends Threshold> extends Descriptor<Threshold> {

        public ThreshHoldDescriptor() {
            super(Threshold.class);
        }
    }
}
