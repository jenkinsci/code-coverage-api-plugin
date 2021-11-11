package io.jenkins.plugins.coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import com.google.errorprone.annotations.FormatMethod;

import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;

/**
 *
 * @author Thomas Willeit
 */
public class QualityGateEvaluator {
    final List<QualityGate> qualityGates = new ArrayList<>();

    /**
     * Compares CodeCoverage with Thresholds and sets QualityGateStatus accordingly
     *
     * @param coverageNode Node to test
     * @param logger simple Logger
     * @return QualityGateStatus: Inactive, Warning, Failed, Passed
     */
    public QualityGateStatus evaluate (final CoverageNode coverageNode,  final FormattedLogger logger ){
        if (qualityGates.isEmpty()){
            logger.print("-> INACTIVE - No quality gate defined");
            return QualityGateStatus.INACTIVE;
        }

        SortedMap<CoverageMetric, Double> metricPercentages = coverageNode.getMetricPercentages();
        List<QualityGate> warnings = new ArrayList<>();


        for (QualityGate qualityGate : this.qualityGates) {
            CoverageMetric coverageMetric = qualityGate.getCoverageMetric();
            Double threshold = metricPercentages.get(coverageMetric);


            if (threshold <= qualityGate.getFailedThreshold()){

                logger.print("-> FAILED - Too low Code Coverage failed this build");
                return QualityGateStatus.FAILED;
            }
            else if (threshold <= qualityGate.getWarningThreshold()){
                warnings.add(qualityGate);
            }
        }
        if (warnings.isEmpty()){
            logger.print("-> Successful - All QualityGates passed");
            return QualityGateStatus.PASSED;
        }else{
            for (QualityGate qualityGate : warnings) {
                logger.print("-> Warning - QualityGate: %s CodeCoverage resulted in a warning",
                        qualityGate.getCoverageMetric());
            }
            return QualityGateStatus.WARNING;
        }

    }

    /**
     * Adds QualityGate to evaluate
     * @param qualityGate
     */
    public void add(final QualityGate qualityGate) {
       this.qualityGates.add(qualityGate);
    }

    /**
     * Add multiple QualityGates to evaluate
     * @param additionalQualityGates
     */
    public void addAll(final Collection<? extends QualityGate> additionalQualityGates) {
        this.qualityGates.addAll(additionalQualityGates);
    }

    /**
     * Removes specific QualityGate from evaluation
     * @param gate
     */
    public void remove(final QualityGate gate) {
        this.qualityGates.remove(gate);
    }

    /**
     * Logs results of the quality gate evaluation.
     */
    @FunctionalInterface
    public interface FormattedLogger {
        /**
         * Logs the specified message.
         *
         * @param format
         *         A <a href="../util/Formatter.html#syntax">format string</a>
         * @param args
         *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
         *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be
         *         zero.
         */
        @FormatMethod
        void print(String format, Object... args);
    }

}
