package io.jenkins.plugins.coverage.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;

import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateCriticality;

import static org.assertj.core.api.Assertions.*;

class QualityGateEvaluatorTest extends AbstractCoverageTest {
    @Test
    void shouldBeInactiveIfGatesAreEmpty() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        assertThat(evaluator.isEnabled()).isEqualTo(false);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);

        assertThat(result).isEqualTo(QualityGateStatus.INACTIVE);
        assertThat(logger.getMessages()).containsExactly("-> INACTIVE - No quality gate defined");
    }

    @Test
    void shouldPassForTooLowThresholds() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE);
        evaluator.add(0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE);
        evaluator.add(0, Metric.FILE, Baseline.CHANGE, QualityGateCriticality.UNSTABLE);
        evaluator.add(0, Metric.LINE, Baseline.CHANGE, QualityGateCriticality.UNSTABLE);
        evaluator.add(0, Metric.FILE, Baseline.FILE, QualityGateCriticality.UNSTABLE);
        evaluator.add(0, Metric.LINE, Baseline.FILE, QualityGateCriticality.UNSTABLE);

        var minimum = -10;
        evaluator.add(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.CHANGE_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.CHANGE_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.FILE_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.FILE_DELTA, QualityGateCriticality.UNSTABLE);

        assertThat(evaluator.isEnabled()).isEqualTo(true);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);

        assertThat(result).isEqualTo(QualityGateStatus.PASSED);
        assertThat(logger.getMessages()).containsExactly(
                "-> [Overall project - File]: ≪PASSED≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 0.00)",
                "-> [Overall project - Line]: ≪PASSED≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 0.00)",
                "-> [Changed code lines - File]: ≪PASSED≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 0.00)",
                "-> [Changed code lines - Line]: ≪PASSED≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 0.00)",
                "-> [Changed files - File]: ≪PASSED≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 0.00)",
                "-> [Changed files - Line]: ≪PASSED≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 0.00)",
                "-> [Overall project (difference to reference job) - File]: ≪PASSED≫ - (Actual value: FILE: -10/1, Quality gate: -10.00)",
                "-> [Overall project (difference to reference job) - Line]: ≪PASSED≫ - (Actual value: LINE: 5/1, Quality gate: -10.00)",
                "-> [Changed code lines (difference to overall project) - File]: ≪PASSED≫ - (Actual value: FILE: -10/1, Quality gate: -10.00)",
                "-> [Changed code lines (difference to overall project) - Line]: ≪PASSED≫ - (Actual value: LINE: 5/1, Quality gate: -10.00)",
                "-> [Changed files (difference to overall project) - File]: ≪PASSED≫ - (Actual value: FILE: -10/1, Quality gate: -10.00)",
                "-> [Changed files (difference to overall project) - Line]: ≪PASSED≫ - (Actual value: LINE: 5/1, Quality gate: -10.00)");
    }

    @Test
    void shouldReportWarningIfBelowThreshold() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE);
        evaluator.add(76.0, Metric.FILE, Baseline.CHANGE, QualityGateCriticality.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.CHANGE, QualityGateCriticality.UNSTABLE);
        evaluator.add(76.0, Metric.FILE, Baseline.FILE, QualityGateCriticality.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.FILE, QualityGateCriticality.UNSTABLE);

        var minimum = 10;
        evaluator.add(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.CHANGE_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.CHANGE_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.FILE_DELTA, QualityGateCriticality.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.FILE_DELTA, QualityGateCriticality.UNSTABLE);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);

        assertThat(result).isEqualTo(QualityGateStatus.WARNING);
        assertThat(logger.getMessages()).containsExactly(
                "-> [Overall project - File]: ≪WARNING≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 76.00)",
                "-> [Overall project - Line]: ≪WARNING≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 51.00)",
                "-> [Changed code lines - File]: ≪WARNING≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 76.00)",
                "-> [Changed code lines - Line]: ≪WARNING≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 51.00)",
                "-> [Changed files - File]: ≪WARNING≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 76.00)",
                "-> [Changed files - Line]: ≪WARNING≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 51.00)",
                "-> [Overall project (difference to reference job) - File]: ≪WARNING≫ - (Actual value: FILE: -10/1, Quality gate: 10.00)",
                "-> [Overall project (difference to reference job) - Line]: ≪WARNING≫ - (Actual value: LINE: 5/1, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - File]: ≪WARNING≫ - (Actual value: FILE: -10/1, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - Line]: ≪WARNING≫ - (Actual value: LINE: 5/1, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - File]: ≪WARNING≫ - (Actual value: FILE: -10/1, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - Line]: ≪WARNING≫ - (Actual value: LINE: 5/1, Quality gate: 10.00)");
    }

    @Test
    void shouldReportFailureIfBelowThreshold() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.FAILURE);
        evaluator.add(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE);
        evaluator.add(76.0, Metric.FILE, Baseline.CHANGE, QualityGateCriticality.FAILURE);
        evaluator.add(51.0, Metric.LINE, Baseline.CHANGE, QualityGateCriticality.FAILURE);
        evaluator.add(76.0, Metric.FILE, Baseline.FILE, QualityGateCriticality.FAILURE);
        evaluator.add(51.0, Metric.LINE, Baseline.FILE, QualityGateCriticality.FAILURE);

        var minimum = 10;
        evaluator.add(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateCriticality.FAILURE);
        evaluator.add(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateCriticality.FAILURE);
        evaluator.add(minimum, Metric.FILE, Baseline.CHANGE_DELTA, QualityGateCriticality.FAILURE);
        evaluator.add(minimum, Metric.LINE, Baseline.CHANGE_DELTA, QualityGateCriticality.FAILURE);
        evaluator.add(minimum, Metric.FILE, Baseline.FILE_DELTA, QualityGateCriticality.FAILURE);
        evaluator.add(minimum, Metric.LINE, Baseline.FILE_DELTA, QualityGateCriticality.FAILURE);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);

        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
        assertThat(logger.getMessages()).containsExactly(
                "-> [Overall project - File]: ≪FAILED≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 76.00)",
                "-> [Overall project - Line]: ≪FAILED≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 51.00)",
                "-> [Changed code lines - File]: ≪FAILED≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 76.00)",
                "-> [Changed code lines - Line]: ≪FAILED≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 51.00)",
                "-> [Changed files - File]: ≪FAILED≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 76.00)",
                "-> [Changed files - Line]: ≪FAILED≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 51.00)",
                "-> [Overall project (difference to reference job) - File]: ≪FAILED≫ - (Actual value: FILE: -10/1, Quality gate: 10.00)",
                "-> [Overall project (difference to reference job) - Line]: ≪FAILED≫ - (Actual value: LINE: 5/1, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - File]: ≪FAILED≫ - (Actual value: FILE: -10/1, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - Line]: ≪FAILED≫ - (Actual value: LINE: 5/1, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - File]: ≪FAILED≫ - (Actual value: FILE: -10/1, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - Line]: ≪FAILED≫ - (Actual value: LINE: 5/1, Quality gate: 10.00)");
    }

    @Test
    void shouldOverwriteStatus() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE);

        assertThatStatusWillBeOverwritten(logger, statistics, evaluator);
    }

    private static void assertThatStatusWillBeOverwritten(final Logger logger, final CoverageStatistics statistics,
            final QualityGateEvaluator evaluator) {
        QualityGateStatus result = evaluator.evaluate(statistics, logger);
        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
        assertThat(logger.getMessages()).containsExactly(
                "-> [Overall project - File]: ≪WARNING≫ - (Actual value: FILE: 75.00% (3/4), Quality gate: 76.00)",
                "-> [Overall project - Line]: ≪FAILED≫ - (Actual value: LINE: 50.00% (2/4), Quality gate: 51.00)");
    }

    @Test
    void shouldFailIfValueIsNotFound() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(50.0, Metric.PACKAGE, Baseline.PROJECT, QualityGateCriticality.FAILURE);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);
        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
        assertThat(logger.getMessages()).containsExactly(
                "-> [Overall project - Package]: ≪FAILED≫ - (Actual value: n/a, Quality gate: 50.00)");
    }

    @Test
    void shouldAddAllQualityGates() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        List<QualityGate> qualityGates = List.of(
                new QualityGate(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE),
                new QualityGate(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE));

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.addAll(qualityGates);

        assertThatStatusWillBeOverwritten(logger, statistics, evaluator);
    }

    private CoverageStatistics createStatistics() {
        return new CoverageStatistics(fillValues(), fillDeltas(),
                fillValues(), fillDeltas(),
                fillValues(), fillDeltas());
    }

    private List<Value> fillValues() {
        var builder = new CoverageBuilder();
        return List.of(builder.setMetric(Metric.FILE).setCovered(3).setMissed(1).build(),
                builder.setMetric(Metric.LINE).setCovered(2).setMissed(2).build());
    }

    private NavigableMap<Metric, Fraction> fillDeltas() {
        final NavigableMap<Metric, Fraction> deltaMapping = new TreeMap<>();
        deltaMapping.put(Metric.FILE, Fraction.getFraction(-10, 1));
        deltaMapping.put(Metric.LINE, Fraction.getFraction(5, 1));
        return deltaMapping;
    }

    /**
     * Logger for the tests that provides a way verify and clear the messages.
     */
    private static class Logger implements QualityGateEvaluator.FormattedLogger {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void print(final String format, final Object... args) {
            messages.add(String.format(format, args));
        }

        List<String> getMessages() {
            return messages;
        }

        void clear() {
            messages.clear();
        }
    }
}
