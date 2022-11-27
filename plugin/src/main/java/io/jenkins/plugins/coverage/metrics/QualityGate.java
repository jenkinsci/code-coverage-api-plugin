package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;

import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.util.VisibleForTesting;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
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

    private static final ElementFormatter ELEMENT_FORMATTER = new ElementFormatter();
    private final double threshold;
    private final Metric metric;
    private final Baseline baseline;
    private final QualityGateStatus status;
    private final QualityGateResult result;

    /**
     * Creates a new instance of {@link QualityGate}.
     *
     * @param threshold
     *         minimum or maximum value that triggers this quality gate
     * @param metric
     *         the metric to compare
     * @param baseline
     *         the baseline to use the bto compare
     * @param unstable
     *         determines whether the build result will be set to unstable or failed if the quality gate is failed
     */
    @DataBoundConstructor
    public QualityGate(final double threshold, final Metric metric, final Baseline baseline,
            final boolean unstable) {
        this(threshold, metric, baseline,
                unstable ? QualityGateResult.UNSTABLE : QualityGateResult.FAILURE);
    }

    QualityGate(final double threshold, final Metric metric, final Baseline baseline,
            final QualityGateResult result) {
        this.threshold = threshold;
        this.metric = metric;
        this.baseline = baseline;
        this.status = result == QualityGateResult.UNSTABLE ? QualityGateStatus.WARNING : QualityGateStatus.FAILED;
        this.result = result;
    }

    public boolean getUnstable() {
        return result == QualityGateResult.UNSTABLE;
    }

    /**
     * Returns a human-readable name of the quality gate.
     *
     * @return a human-readable name
     */
    public String getName() {
        return String.format("%s - %s", ELEMENT_FORMATTER.getDisplayName(getBaseline()),
                ELEMENT_FORMATTER.getDisplayName(getMetric()));
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
            options.add(ELEMENT_FORMATTER.getDisplayName(metric), metric.name());
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
            options.add(ELEMENT_FORMATTER.getDisplayName(baseline), baseline.name());
        }
    }
}
