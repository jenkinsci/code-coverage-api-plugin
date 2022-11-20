package io.jenkins.plugins.coverage.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.errorprone.annotations.FormatMethod;

import edu.hm.hafner.metric.Metric;

import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateResult;

/**
 * Evaluates a set of quality gates for a static analysis report.
 *
 * @author Johannes Walter
 */
public class QualityGateEvaluator {
    private final List<QualityGate> qualityGates = new ArrayList<>();

    /**
     * Enforces the quality gates for the specified run.
     *
     * @param report
     *         the report to evaluate
     * @param logger
     *         the logger that reports the passed and failed quality gate thresholds
     *
     * @return result of the evaluation, expressed by a build state
     */
    public QualityGateStatus evaluate(final CoverageStatistics report, final FormattedLogger logger) {
        if (qualityGates.isEmpty()) {
            logger.print("-> INACTIVE - No quality gate defined");

            return QualityGateStatus.INACTIVE;
        }

        QualityGateStatus status = QualityGateStatus.PASSED;

        for (QualityGate qualityGate : qualityGates) {
            var baseline = qualityGate.getBaseline();
            var possibleValue = report.getValue(baseline, qualityGate.getMetric());
            if (possibleValue.isPresent()) {
                var actualValue = possibleValue.get();
                if (actualValue.isBelowThreshold(qualityGate.getThreshold())) {
                    logger.print("-> <%s> %s - %s (Quality Gate: %.2f)",
                            qualityGate.getName(), qualityGate.getStatus(), actualValue, qualityGate.getThreshold());
                    status = changeQualityStatusIfNotWorse(status, qualityGate);
                }
                else {
                    logger.print("-> <%s> PASSED - %s (Quality Gate: %.2f)",
                            qualityGate.getName(), actualValue, qualityGate.getThreshold());
                }
            }
            else {
                status = changeQualityStatusIfNotWorse(status, qualityGate);
                logger.print("-> <%s> %s - n/a (Quality Gate: %.2f)",
                        qualityGate.getName(), qualityGate.getStatus(), qualityGate.getThreshold());
            }
        }

        return status;
    }

    private QualityGateStatus changeQualityStatusIfNotWorse(QualityGateStatus status, final QualityGate qualityGate) {
        if (qualityGate.getStatus().isWorseThan(status)) {
            status = qualityGate.getStatus();
        }
        return status;
    }

    /**
     * Appends the specified quality gates to the end of the list of quality gates.
     *
     * @param size
     *         the minimum coverage that is needed for the quality gate
     * @param metric
     *         the metric to compare
     * @param baseline
     *         the baseline to use the bto compare
     * @param result
     *         determines whether the quality gate is a warning or failure
     */
    public void add(final double size, final Metric metric, final Baseline baseline, final QualityGateResult result) {
        qualityGates.add(new QualityGate(size, metric, baseline, result));
    }

    /**
     * Appends all the quality gates in the specified collection to the end of the list of quality gates.
     *
     * @param additionalQualityGates
     *         the quality gates to add
     */
    public void addAll(final Collection<? extends QualityGate> additionalQualityGates) {
        this.qualityGates.addAll(additionalQualityGates);
    }

    /**
     * Returns whether at least one quality gate has been added.
     *
     * @return {@code true} if at least one quality gate has been added, {@code false} otherwise
     */
    public boolean isEnabled() {
        return !qualityGates.isEmpty();
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
