package io.jenkins.plugins.coverage.metrics;

import java.io.Serializable;

import edu.hm.hafner.metric.Metric;

import hudson.model.AbstractDescribableImpl;

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
}
