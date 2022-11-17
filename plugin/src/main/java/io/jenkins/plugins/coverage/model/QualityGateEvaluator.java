package io.jenkins.plugins.coverage.model;

import com.google.errorprone.annotations.FormatMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            double actualSize = qualityGate.getActualCoverageReference().apply(report);
            if (actualSize < qualityGate.getThreshold()) {
                logger.print("-> %s - %s: %f - Quality QualityGate: %f",
                        qualityGate.getStatus(), qualityGate.getName(), actualSize, qualityGate.getThreshold());
                if (qualityGate.getStatus().isWorseThan(status)) {
                    status = qualityGate.getStatus();
                }
            }
            else {
                logger.print("-> PASSED - %s: %f - Quality QualityGate: %f",
                        qualityGate.getName(), actualSize, qualityGate.getThreshold());
            }
        }

        return status;
    }

    /**
     * Appends the specified quality gates to the end of the list of quality gates.
     *
     * @param size
     *         the minimum coverage that is needed for the quality gate
     * @param type
     *         the type of the quality gate
     * @param result
     *         determines whether the quality gate is a warning or failure
     */
    public void add(final double size, final QualityGate.QualityGateType type, final QualityGate.QualityGateResult result) {
        qualityGates.add(new QualityGate(size, type, result));
    }

    /**
     * Appends all of the quality gates in the specified collection to the end of the list of quality gates.
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
