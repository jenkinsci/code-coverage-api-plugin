package io.jenkins.plugins.coverage.threshold;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageElementRegister;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Threshold implements ExtensionPoint, Describable<Threshold>, Serializable {

    private final String thresholdTarget;

    // mark build as unstable when coverage is less than this.
    private float unstableThreshold = 0.0f;

    // used for calculating healthy scores.
    private float unhealthyThreshold = 0.0f;

    private boolean failUnhealthy = false;

    @DataBoundConstructor
    public Threshold(String thresholdTarget) {
        this.thresholdTarget = thresholdTarget;
    }


    public String getThresholdTarget() {
        return thresholdTarget;
    }

    public CoverageElement getThresholdTargetElement() {
        return CoverageElement.get(getThresholdTarget());
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
        return Objects.equals(getThresholdTarget(), threshold.getThresholdTarget());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getThresholdTarget());
    }

    @Override
    public String toString() {
        return thresholdTarget + " {" +
                "unstableThreshold=" + unstableThreshold +
                ", unhealthyThreshold=" + unhealthyThreshold +
                "}";
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

        public CoverageElement[] getAllCoverageMetrics() {
            return CoverageElementRegister.all();
        }

        public CoverageElement[] getCoverageMetricsByType(String coverageElementType) {
            if (StringUtils.isEmpty(coverageElementType)) {
                return getAllCoverageMetrics();
            }

            return CoverageElementRegister.listCommonsAndSpecificType(coverageElementType);
        }
    }
}
