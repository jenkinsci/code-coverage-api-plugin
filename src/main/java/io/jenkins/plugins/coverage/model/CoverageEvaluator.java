package io.jenkins.plugins.coverage.model;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.errorprone.annotations.FormatMethod;

/*
 * create a QualityGateStatus based on a list of QualityGates for a specific CoverageNode.
 */
public class CoverageEvaluator {

    private final Set<QualityGate> qualityGateSet = new TreeSet<>();

    /**
     * Evaluates a node by using QualityGates.
     * @param node the given node object to evaluate
     * @param logger the logger
     * @return the resulting QualityGateStatus
     */
    public QualityGateStatus evaluate(final CoverageNode node, final FormattedLogger logger) {

        boolean returnWarning = false;

        if (qualityGateSet.isEmpty()) {
            logger.print("-> INACTIVE - No quality gate defined");
            return QualityGateStatus.INACTIVE;
        }
        else {
            for (QualityGate gate : qualityGateSet) {
                double percentage = node.getMetricPercentages().get(gate.getMetric());
                if (percentage < gate.getLimit()) {
                    if (gate.getQualityGateStatus() == QualityGateStatus.FAILED) {

                        logger.print("-> NOT PASSED: FAILED - Quality Gate: (%s, %.3f, %s); ACHIEVED: %.3f",
                                gate.getQualityGateStatus(), gate.getLimit(), gate.getMetric().getName(), percentage);
                        return QualityGateStatus.FAILED;
                    }
                    else {
                        returnWarning = true;
                        logger.print("-> NOT PASSED: WARNING - Quality Gate: (%s, %.3f, %s); ACHIEVED: %.3f",
                                gate.getQualityGateStatus(), gate.getLimit(), gate.getMetric().getName(), percentage);
                    }
                }
                else {
                    logger.print("-> PASSED - Quality Gate: (%s, %.3f, %s); ACHIEVED: %.3f",
                            gate.getQualityGateStatus(), gate.getLimit(), gate.getMetric().getName(), percentage);
                }
            }
        }
        if (returnWarning) {
            return QualityGateStatus.WARNING;
        }
        return QualityGateStatus.SUCCESSFUL;
    }

    /**
     * Is used to add a QualityGate to the existing QualityGates in qualityGateList.
     * @param gate the QualityGate which should be added
     */
    public void add(final QualityGate gate) {
        this.qualityGateSet.add(gate);
    }

    /**
     * Is used to add a List of QualityGates to the existing QualityGates in qualityGateList.
     * @param gateList the QualityGates which should be added
     */
    public void addAll(final List<QualityGate> gateList) {
        this.qualityGateSet.addAll(gateList);
    }

    /**
     * Is used to remove a QualityGate from qualityGateList.
     * @param gate the QualityGate which should be added
     */
    public void remove(final QualityGate gate) {
        qualityGateSet.remove(gate);
    }

    /**
     * Getter for existing QualityGates
     * @return existing QualityGates
     */
    public Set<QualityGate> getQualityGateSet() {
        return qualityGateSet;
    }

    /**
     * Interface for logging a specified message using a format
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
