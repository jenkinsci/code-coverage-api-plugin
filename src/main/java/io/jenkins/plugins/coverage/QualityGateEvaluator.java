package io.jenkins.plugins.coverage;

import java.util.List;

import com.google.errorprone.annotations.FormatMethod;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;

public class QualityGateEvaluator {

    public QualityGateStatus evaluate(final CoverageNode rootNode, final List<QualityGate> qualityGates,
            final FormattedLogger logger) {
        if (qualityGates.isEmpty()) {
            logger.print("-> INACTIVE - No quality gate defined");

            return QualityGateStatus.INACTIVE;
        }
        QualityGateStatus status = QualityGateStatus.PASSED;

        for (QualityGate qualityGate : qualityGates) {
            Coverage coverage = rootNode.getCoverage(qualityGate.getType());
            if (coverage != null) {
                if (coverage.getCoveredPercentage() < qualityGate.getThreshold()) {
                    if (qualityGate.getStatusIfNotPassedSuccesful().isWorseThan(status)) {
                        status = qualityGate.getStatusIfNotPassedSuccesful();
                    }
                }
            }
        }
        return status;
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
