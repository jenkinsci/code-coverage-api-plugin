package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.errorprone.annotations.FormatMethod;

/**
 * Evaluates a set of quality gates for code coverage.
 *
 * @author Adrian Germeck
 */
public class CoverageEvaluator {
    private final List<QualityGate> qualityGateList = new ArrayList<>();

    /**
     * Enforces quality gates for the specified run.
     *
     * @param node
     *         the CoverageNode to evaluate
     * @param logger
     *         the logger that reports the passed and failed quality gate thresholds
     *
     * @return result of the evaluation, expressed by a QualityGateStatus
     */
    public QualityGateStatus evaluate(final CoverageNode node, final FormattedLogger logger) {
        if (this.qualityGateList.isEmpty()) {
            logger.print("-> INACTIVE - No quality gate defined");
            return QualityGateStatus.INACTIVE;
        }

        Map<QualityGate, Double> warningList = new HashMap<>();
        SortedMap<CoverageMetric, Double> metricPercentages = node.getMetricPercentages();

        for (QualityGate qualityGate : this.qualityGateList) {
            CoverageMetric coverageMetric = qualityGate.getCoverageMetric();
            Double percentage = metricPercentages.get(coverageMetric);

            if (percentage < qualityGate.getFailedLimit()) {
                logger.print("-> FAILED - QualityGate: %s - warn/fail/actual: %.2f/%.2f/%.2f",
                        qualityGate.getCoverageMetric(), qualityGate.getWarningLimit(), qualityGate.getFailedLimit(),
                        percentage);
                return QualityGateStatus.FAILED;
            }
            else if (percentage < qualityGate.getWarningLimit()) {
                warningList.put(qualityGate, percentage);
            }
        }
        if (warningList.isEmpty()) {
            logger.print("-> SUCCESSFUL - QualityGate");
            return QualityGateStatus.SUCCESSFUL;
        }
        else {
            for (Map.Entry<QualityGate, Double> entry : warningList.entrySet()) {
                final QualityGate qualityGate = entry.getKey();
                logger.print("-> WARNING - QualityGate: %s - warn/fail/actual: %.2f/%.2f/%.2f",
                        qualityGate.getCoverageMetric(), qualityGate.getWarningLimit(),
                        qualityGate.getFailedLimit(), entry.getValue());
            }
            return QualityGateStatus.WARNING;
        }
    }

    public void add(final QualityGate gate) {
        this.qualityGateList.add(gate);
    }

    public void addAll(final List<QualityGate> gateList) {
        qualityGateList.addAll(gateList);
    }

    public void remove(final QualityGate gate) {
        qualityGateList.remove(gate);
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