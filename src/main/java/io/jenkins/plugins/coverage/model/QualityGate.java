package io.jenkins.plugins.coverage.model;

import hudson.model.AbstractDescribableImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.function.Function;

public class QualityGate extends AbstractDescribableImpl<QualityGate> implements Serializable {
    private final double threshold;
    private final QualityGateType type;
    private final QualityGateStatus status;

    @DataBoundConstructor
    public QualityGate(double threshold, QualityGateType type, final boolean unstable) {
        super();
        this.threshold = threshold;
        this.type = type;
        this.status = unstable ? QualityGateStatus.WARNING : QualityGateStatus.FAILED;
    }

    public QualityGate(final double threshold, final QualityGateType type, final QualityGateResult result) {
        super();

        this.threshold = threshold;
        this.type = type;
        status = result.status;
    }

    public double getThreshold() {
        return threshold;
    }

    public String getName() {
        return type.getName();
    }

    public QualityGateType getType() {
        return type;
    }

    public QualityGateStatus getStatus() {
        return status;
    }

    public Function<CoverageStatistics, Double> getActualSizeMethodReference() {
        return type.getSizeGetter();
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

        QualityGateType(CoverageMetric metric, CoverageStatistics.StatisticProperties properties) {
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

        public Function<CoverageStatistics, Double> getSizeGetter() {
            return properties.getSizeGetter();
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
