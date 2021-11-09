package io.jenkins.plugins.coverage;

import java.util.List;
import java.util.Optional;

import com.google.errorprone.annotations.FormatMethod;

import io.jenkins.plugins.coverage.model.CoverageNode;

/**
 * Evaluates a set of quality gates for a static analysis report.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
public class QualityGateEvaluator {

    /**
     * Enforces this quality gate for the specified run.
     *
     * @param coverageNode
     *         the coverage root node to evaluate
     * @param qualityGates
     *          the quality gates to be checked
     * @param logger
     *         the logger that reports the passed and failed quality gate thresholds
     *
     * @return result of the evaluation, expressed by a build state
     */
    public QualityGateStatus evaluate(final CoverageNode coverageNode, final List<QualityGate> qualityGates,
            final FormattedLogger logger) {

        if (qualityGates.isEmpty()) {
            logger.print("-> INACTIVE - No quality gate defined");
            return QualityGateStatus.INACTIVE;
        }

        // TODO: Do we need logger prints like .. ? (if not, maybe adjust the method-documentation)
        // logger.print("-> %s - %s: %d - Quality QualityGate: %d", qualityGate.getStatus(), qualityGate.getName(), actualSize, qualityGate.getThreshold());
        // logger.print("-> PASSED - %s: %d - Quality QualityGate: %d", qualityGate.getName(), actualSize, qualityGate.getThreshold());

        return qualityGates.stream().reduce(QualityGateStatus.PASSED,
                (currentStatus, qualityGate) ->
                    Optional.ofNullable(coverageNode.getCoverage(qualityGate.getType()))
                            .filter(coverage -> coverage.getCoveredPercentage() < qualityGate.getThreshold())
                            .map(coverage -> qualityGate.getStatusIfNotPassedSuccessful())
                            .filter(status -> status.isWorseThan(currentStatus))
                            .orElse(currentStatus),
                QualityGateStatus::getWorseStatus);
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
