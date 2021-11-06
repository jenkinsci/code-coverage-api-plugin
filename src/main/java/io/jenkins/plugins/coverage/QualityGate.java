package io.jenkins.plugins.coverage;

import java.io.Serializable;

import java.util.Objects;

import edu.hm.hafner.util.VisibleForTesting;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.util.JenkinsFacade;

public class QualityGate implements Serializable {

    private final float threshold;
    private final CoverageMetric type;
    private final QualityGateStatus status;

    public QualityGate(final float threshold, final CoverageMetric type, final boolean unstable) {
        this.threshold = threshold;
        this.type = type;
        this.status = unstable ? QualityGateStatus.WARNING : QualityGateStatus.FAILED;
    }

    public float getThreshold() {
        return threshold;
    }

    public QualityGateStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QualityGate that = (QualityGate) o;
        return threshold == that.threshold && type == that.type && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, type, status);
    }
}
