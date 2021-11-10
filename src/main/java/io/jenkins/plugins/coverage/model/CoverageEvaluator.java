package io.jenkins.plugins.coverage.model;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.errorprone.annotations.FormatMethod;

/*
 * create a QualityGateStatus based on a list of QualityGates for a specific CoverageNode
 */
public class CoverageEvaluator {

    private final Set<QualityGate> qualityGateList = new TreeSet<>();

    public QualityGateStatus evaluate(final CoverageNode node, final FormattedLogger logger) {

        boolean returnWarning = false;

        if (qualityGateList.isEmpty()) {
            logger.print("-> INACTIVE - No quality gate defined");
            return QualityGateStatus.INACTIVE;
        }
        else {
            for (QualityGate gate : qualityGateList) {
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

    public void add(final QualityGate gate) {
        this.qualityGateList.add(gate);
    }

    public void addAll(final List<QualityGate> gateList) {
        this.qualityGateList.addAll(gateList);
    }

    public void remove(final QualityGate gate) {
        qualityGateList.remove(gate);
    }

    public Set<QualityGate> getQualityGateList() {
        return qualityGateList;
    }

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
