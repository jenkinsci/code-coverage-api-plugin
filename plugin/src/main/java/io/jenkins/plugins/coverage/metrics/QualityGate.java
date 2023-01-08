package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;

import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.util.VisibleForTesting;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.verb.POST;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;

import io.jenkins.plugins.util.JenkinsFacade;

/**
 * Defines a quality gate based on a specific threshold of code coverage in the current build. After a build has been
 * finished, a set of {@link QualityGate quality gates} will be evaluated and the overall quality gate status will be
 * reported in Jenkins UI.
 *
 * @author Johannes Walter
 */
public class QualityGate extends AbstractDescribableImpl<QualityGate> implements Serializable {
    private static final long serialVersionUID = -397278599489426668L;

    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private final double threshold;
    private final Metric metric;
    private Baseline baseline = Baseline.PROJECT;
    private QualityGateCriticality criticality = QualityGateCriticality.UNSTABLE;

    /**
     * Creates a new instance of {@link QualityGate}.
     *
     * @param threshold
     *         minimum or maximum value that triggers this quality gate
     * @param metric
     *         the metric to compare
     */
    @DataBoundConstructor
    public QualityGate(final double threshold, final Metric metric) {
        this.threshold = threshold;
        this.metric = metric;
    }

    /**
     * Creates a new instance of {@link QualityGate}.
     *
     * @param threshold
     *         minimum or maximum value that triggers this quality gate
     * @param metric
     *         the metric to compare
     * @param baseline
     *         the baseline to use the bto compare
     * @param criticality
     *         determines whether the build result will be set to unstable or failed if the quality gate is failed
     */
    public QualityGate(final double threshold, final Metric metric,
            final Baseline baseline, final QualityGateCriticality criticality) {
        this(threshold, metric);

        this.baseline = baseline;
        this.criticality = criticality;
    }

    /**
     * Sets the baseline that will be used for the quality gate evaluation.
     *
     * @param baseline
     *         the baseline to use
     */
    @DataBoundSetter
    public void setBaseline(final Baseline baseline) {
        this.baseline = baseline;
    }

    /**
     * Sets the criticality of this quality gate. When a quality gate has been missed, this property determines whether
     * the result of the associated coverage stage will be marked as unstable or failure.
     *
     * @param criticality
     *         the criticality for this quality gate
     */
    @DataBoundSetter
    public void setCriticality(final QualityGateCriticality criticality) {
        this.criticality = criticality;
    }

    /**
     * Returns a human-readable name of the quality gate.
     *
     * @return a human-readable name
     */
    public String getName() {
        return String.format("%s - %s", FORMATTER.getDisplayName(getBaseline()),
                FORMATTER.getDisplayName(getMetric()));
    }

    @Override
    public String toString() {
        return getName() + String.format(" - %s: %f", getCriticality(), getThreshold());
    }

    public double getThreshold() {
        return threshold;
    }

    public Metric getMetric() {
        return metric;
    }

    public Baseline getBaseline() {
        return baseline;
    }

    public QualityGateCriticality getCriticality() {
        return criticality;
    }

    public QualityGateStatus getStatus() {
        return getCriticality().getStatus();
    }

    /**
     * Determines the Jenkins build result if the quality gate is failed.
     */
    public enum QualityGateCriticality {
        /** The build will be marked as unstable. */
        UNSTABLE(QualityGateStatus.WARNING),

        /** The build will be marked as failed. */
        FAILURE(QualityGateStatus.FAILED);

        private final QualityGateStatus status;

        QualityGateCriticality(final QualityGateStatus status) {
            this.status = status;
        }

        /**
         * Returns the status.
         *
         * @return the status
         */
        public QualityGateStatus getStatus() {
            return status;
        }
    }

    /**
     * Descriptor of the {@link QualityGate}.
     */
    @Extension
    public static class QualityGateDescriptor extends Descriptor<QualityGate> {
        private final JenkinsFacade jenkins;

        @VisibleForTesting
        QualityGateDescriptor(final JenkinsFacade jenkinsFacade) {
            super();

            jenkins = jenkinsFacade;
        }

        /**
         * Creates a new descriptor.
         */
        @SuppressWarnings("unused") // Required for Jenkins Extensions
        public QualityGateDescriptor() {
            this(new JenkinsFacade());
        }

        /**
         * Returns a model with all {@link Metric metrics} that can be used in quality gates.
         *
         * @param project
         *         the project that is configured
         *
         * @return a model with all {@link Metric metrics}.
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
        public ListBoxModel doFillMetricItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (jenkins.hasPermission(Item.CONFIGURE, project)) {
                return FORMATTER.getMetricItems();
            }
            return new ListBoxModel();
        }

        /**
         * Returns a model with all {@link Metric metrics} that can be used in quality gates.
         *
         * @param project
         *         the project that is configured
         *
         * @return a model with all {@link Metric metrics}.
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
        public ListBoxModel doFillBaselineItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (jenkins.hasPermission(Item.CONFIGURE, project)) {
                return FORMATTER.getBaselineItems();
            }
            return new ListBoxModel();
        }

        /**
         * Returns a model with all {@link Metric metrics} that can be used in quality gates.
         *
         * @param project
         *         the project that is configured
         *
         * @return a model with all {@link Metric metrics}.
         */
        @POST
        @SuppressWarnings("unused") // used by Stapler view data binding
        public ListBoxModel doFillCriticalityItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (jenkins.hasPermission(Item.CONFIGURE, project)) {
                ListBoxModel options = new ListBoxModel();
                options.add(Messages.QualityGate_Unstable(), QualityGateCriticality.UNSTABLE.name());
                options.add(Messages.QualityGate_Failure(), QualityGateCriticality.FAILURE.name());
                return options;
            }
            return new ListBoxModel();
        }
    }
}
