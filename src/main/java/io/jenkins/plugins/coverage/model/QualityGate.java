package io.jenkins.plugins.coverage.model;

import hudson.model.AbstractDescribableImpl;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Defines a quality gate based on a specific threshold of code coverage in the current build. After a
 * build has been finished, a set of {@link QualityGate quality gates} will be evaluated and the overall quality gate
 * status will be reported in Jenkins UI.
 *
 * @author Johannes Walter
 */
public class QualityGate extends AbstractDescribableImpl<QualityGate> implements Serializable {
    private static final long serialVersionUID = -397278599489426668L;

    private final double threshold;
    private final QualityGateType type;
    private final QualityGateStatus status;

    /**
     * Creates a new instance of {@link QualityGate}.
     *
     * @param threshold
     *         the minimum coverage that is needed for the quality gate
     * @param type
     *         the type of the quality gate
     * @param result
     *         determines whether the quality gate is a warning or failure
     */
    public QualityGate(final double threshold, final QualityGateType type, final QualityGateResult result) {
        super();

        this.threshold = threshold;
        this.type = type;
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
     * Returns the human readable name of the quality gate.
     *
     * @return the human readable name
     */
    public String getName() {
        return type.getName();
    }

    /**
     * Returns the type of the quality gate.
     *
     * @return the type of quality gate
     */
    public QualityGateType getType() {
        return type;
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
     * Returns the method that should be used to determine the actual coverage in the build.
     *
     * @return threshold getter
     */
    public Function<CoverageStatistics, Double> getActualCoverageReference() {
        return type.getCoverageGetter();
    }

    /**
     * Available quality gate types.
     */
    public enum QualityGateType {
        FILE(CoverageMetric.FILE, CoverageStatistics.StatisticProperties.FILE),
        MODULE(CoverageMetric.MODULE, CoverageStatistics.StatisticProperties.MODULE),
        METHOD(CoverageMetric.METHOD, CoverageStatistics.StatisticProperties.METHOD),
        LINE(CoverageMetric.LINE, CoverageStatistics.StatisticProperties.LINE),
        PACKAGE(CoverageMetric.PACKAGE, CoverageStatistics.StatisticProperties.PACKAGE),
        BRANCH(CoverageMetric.BRANCH, CoverageStatistics.StatisticProperties.BRANCH),
        CLASS(CoverageMetric.CLASS, CoverageStatistics.StatisticProperties.CLASS),
        INSTRUCTION(CoverageMetric.INSTRUCTION, CoverageStatistics.StatisticProperties.INSTRUCTION);

        private final CoverageMetric metric;
        private final CoverageStatistics.StatisticProperties properties;

        /**
         * Ctor for QualityGateTypes.
         *
         * @param metric The underlying metric
         * @param properties The statistics properties
         */
        QualityGateType(final CoverageMetric metric, final CoverageStatistics.StatisticProperties properties) {
            this.metric = metric;
            this.properties = properties;
        }

        /**
         * Returns the name of this type.
         *
         * @return name
         */
        public String getName() {
            return metric.getName();
        }

        /**
         * Returns the method that should be used to determine the actual coverage in the build.
         *
         * @return threshold getter
         */
        public Function<CoverageStatistics, Double> getCoverageGetter() {
            return properties.getCoverageGetter();
        }

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
        public QualityGateStatus getStatus() {
            return status;
        }
    }
}
