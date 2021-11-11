package io.jenkins.plugins.coverage.model;

import com.google.errorprone.annotations.FormatMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QualityGateEvaluator {
    private final List<QualityGate> qualityGates = new ArrayList<>();

    public QualityGateStatus evaluate(final CoverageStatistics report, final FormattedLogger logger) {
        if (qualityGates.isEmpty()) {
            logger.print("-> INACTIVE - No quality gate defined");

            return QualityGateStatus.INACTIVE;
        }

        QualityGateStatus status = QualityGateStatus.PASSED;

        for (QualityGate qualityGate : qualityGates) {
            double actualSize = qualityGate.getActualSizeMethodReference().apply(report);
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

    public void add(final double size, final QualityGate.QualityGateType type, final QualityGate.QualityGateResult result) {
        qualityGates.add(new QualityGate(size, type, result));
    }

    public void addAll(final Collection<? extends QualityGate> additionalQualityGates) {
        this.qualityGates.addAll(additionalQualityGates);
    }

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
