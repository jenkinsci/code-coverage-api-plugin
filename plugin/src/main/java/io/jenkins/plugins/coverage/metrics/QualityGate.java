package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;

import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.util.VisibleForTesting;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.verb.POST;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;

import io.jenkins.plugins.coverage.metrics.CoverageViewModel.ValueLabelProvider;
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

    private final double threshold;
    private final Metric metric;
    private final Baseline baseline;
    private final QualityGateStatus status;

    /**
     * Creates a new instance of {@link QualityGate}.
     *
     * @param threshold
     *         the minimum or maximum that is needed for the quality gate
     * @param metric
     *         the metric to compare
     * @param baseline
     *         the baseline to use the bto compare
     * @param result
     *         determines whether the quality gate is a warning or failure
     */
    public QualityGate(final double threshold, final Metric metric, final Baseline baseline,
            final QualityGateResult result) {
        super();

        this.threshold = threshold;
        this.metric = metric;
        this.baseline = baseline;
        status = result.status;
    }

    /**
     * Returns the minimum number of issues that will fail the quality gate.
     *
     * @return minimum number of issues
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Returns the human-readable name of the quality gate.
     *
     * @return the human-readable name
     */
    // TODO: l10n?
    public String getName() {
        return String.format("%s:%s", getBaseline(), getMetric());
    }

    public Metric getMetric() {
        return metric;
    }

    public Baseline getBaseline() {
        return baseline;
    }

    /**
     * Returns the status of the quality gate.
     *
     * @return the status
     */
    public QualityGateStatus getStatus() {
        return status;
    }

    /**
     * Determines the Jenkins build result if the quality gate is failed.
     */
    public enum QualityGateResult {
        /** The build will be marked as unstable. */
        UNSTABLE(QualityGateStatus.WARNING),

        /** The build will be marked as failed. */
        FAILURE(QualityGateStatus.FAILED);

        private final QualityGateStatus status;

        QualityGateResult(final QualityGateStatus status) {
            this.status = status;
        }

        /**
         * Returns the status.
         *
         * @return the status
         */
        // TODO: do we need this mapping?
        public QualityGateStatus getStatus() {
            return status;
        }
    }

    /**
     * Descriptor of the {@link QualityGate}.
     */
    @Extension
    public static class QualityGateDescriptor extends Descriptor<QualityGate> {
        private static final JenkinsFacade JENKINS = new JenkinsFacade();
        private static final ValueLabelProvider LABEL_PROVIDER = new ValueLabelProvider();
        private final ModelValidation modelValidation = new ModelValidation();
        private final JenkinsFacade jenkins;

        @VisibleForTesting
        QualityGateDescriptor(final JenkinsFacade jenkinsFacade) {
            super();

            jenkins = jenkinsFacade;
        }

        /**
         * Creates a new descriptor.
         */
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
            if (JENKINS.hasPermission(Item.CONFIGURE, project)) {
                ListBoxModel options = new ListBoxModel();
                add(options, Metric.MODULE);
                add(options, Metric.PACKAGE);
                add(options, Metric.FILE);
                add(options, Metric.CLASS);
                add(options, Metric.METHOD);
                add(options, Metric.LINE);
                add(options, Metric.BRANCH);
                add(options, Metric.INSTRUCTION);
                add(options, Metric.MUTATION);
                add(options, Metric.COMPLEXITY);
                add(options, Metric.LOC);
                return options;
            }
            return new ListBoxModel();
        }

        private void add(final ListBoxModel options, final Metric metric) {
            options.add(LABEL_PROVIDER.getDisplayName(metric), metric.name());
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
            if (JENKINS.hasPermission(Item.CONFIGURE, project)) {
                ListBoxModel options = new ListBoxModel();
                add(options, Baseline.PROJECT);
                add(options, Baseline.CHANGE);
                add(options, Baseline.FILE);
                add(options, Baseline.PROJECT_DELTA);
                add(options, Baseline.CHANGE_DELTA);
                add(options, Baseline.FILE_DELTA);
                return options;
            }
            return new ListBoxModel();
        }

        private void add(final ListBoxModel options, final Baseline baseline) {
            options.add(LABEL_PROVIDER.getDisplayName(baseline), baseline.name());
        }
    }
}
