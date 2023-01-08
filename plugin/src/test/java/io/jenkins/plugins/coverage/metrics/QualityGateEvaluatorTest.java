package io.jenkins.plugins.coverage.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Metric;

import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateCriticality;

import static io.jenkins.plugins.coverage.metrics.Assertions.*;

class QualityGateEvaluatorTest extends AbstractCoverageTest {
    @Test
    void shouldBeInactiveIfGatesAreEmpty() {
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();

        QualityGateResult result = evaluator.evaluate(statistics);

        assertThat(result).hasNoMessages().isInactive().isSuccessful().hasOverallStatus(QualityGateStatus.INACTIVE);
    }

    @Test
    void shouldPassForTooLowThresholds() {
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

        assertThat(evaluator).isEnabled();

        QualityGateResult result = evaluator.evaluate(statistics);

        assertThat(result).hasOverallStatus(QualityGateStatus.PASSED).isSuccessful().isNotInactive().hasMessages(
                "-> [Overall project - File]: ≪Success≫ - (Actual value: 75.00%, Quality gate: 0.00)",
                "-> [Overall project - Line]: ≪Success≫ - (Actual value: 50.00%, Quality gate: 0.00)",
                "-> [Changed code lines - File]: ≪Success≫ - (Actual value: 75.00%, Quality gate: 0.00)",
                "-> [Changed code lines - Line]: ≪Success≫ - (Actual value: 50.00%, Quality gate: 0.00)",
                "-> [Changed files - File]: ≪Success≫ - (Actual value: 75.00%, Quality gate: 0.00)",
                "-> [Changed files - Line]: ≪Success≫ - (Actual value: 50.00%, Quality gate: 0.00)",
                "-> [Overall project (difference to reference job) - File]: ≪Success≫ - (Actual value: -10.00%, Quality gate: -10.00)",
                "-> [Overall project (difference to reference job) - Line]: ≪Success≫ - (Actual value: +5.00%, Quality gate: -10.00)",
                "-> [Changed code lines (difference to overall project) - File]: ≪Success≫ - (Actual value: -10.00%, Quality gate: -10.00)",
                "-> [Changed code lines (difference to overall project) - Line]: ≪Success≫ - (Actual value: +5.00%, Quality gate: -10.00)",
                "-> [Changed files (difference to overall project) - File]: ≪Success≫ - (Actual value: -10.00%, Quality gate: -10.00)",
                "-> [Changed files (difference to overall project) - Line]: ≪Success≫ - (Actual value: +5.00%, Quality gate: -10.00)");
    }

    @Test
    void shouldReportUnstableIfBelowThreshold() {
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

        QualityGateResult result = evaluator.evaluate(statistics);

        assertThat(result).hasOverallStatus(QualityGateStatus.WARNING).isNotSuccessful().isNotInactive().hasMessages(
                "-> [Overall project - File]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Overall project - Line]: ≪Unstable≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Changed code lines - File]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Changed code lines - Line]: ≪Unstable≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Changed files - File]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Changed files - Line]: ≪Unstable≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Overall project (difference to reference job) - File]: ≪Unstable≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Overall project (difference to reference job) - Line]: ≪Unstable≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - File]: ≪Unstable≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - Line]: ≪Unstable≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - File]: ≪Unstable≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - Line]: ≪Unstable≫ - (Actual value: +5.00%, Quality gate: 10.00)");
    }

    @Test
    void shouldReportFailureIfBelowThreshold() {
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

        QualityGateResult result = evaluator.evaluate(statistics);

        assertThat(result).hasOverallStatus(QualityGateStatus.FAILED).isNotSuccessful().isNotInactive().hasMessages(
                "-> [Overall project - File]: ≪Failed≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Overall project - Line]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Changed code lines - File]: ≪Failed≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Changed code lines - Line]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Changed files - File]: ≪Failed≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Changed files - Line]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Overall project (difference to reference job) - File]: ≪Failed≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Overall project (difference to reference job) - Line]: ≪Failed≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - File]: ≪Failed≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Changed code lines (difference to overall project) - Line]: ≪Failed≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - File]: ≪Failed≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Changed files (difference to overall project) - Line]: ≪Failed≫ - (Actual value: +5.00%, Quality gate: 10.00)");
    }

    @Test
    void shouldOverwriteStatus() {
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE);

        assertThatStatusWillBeOverwritten(statistics, evaluator);
    }

    @Test
    void shouldFailIfValueIsNotFound() {
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(50.0, Metric.PACKAGE, Baseline.PROJECT, QualityGateCriticality.FAILURE);

        QualityGateResult result = evaluator.evaluate(statistics);
        assertThat(result).hasOverallStatus(QualityGateStatus.FAILED).isNotSuccessful().hasMessages(
                "-> [Overall project - Package]: ≪Failed≫ - (Actual value: n/a, Quality gate: 50.00)");
    }

    @Test
    void shouldAddAllQualityGates() {
        CoverageStatistics statistics = createStatistics();

        List<QualityGate> qualityGates = List.of(
                new QualityGate(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE),
                new QualityGate(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE));

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.addAll(qualityGates);

        assertThatStatusWillBeOverwritten(statistics, evaluator);
    }

    private static void assertThatStatusWillBeOverwritten(final CoverageStatistics statistics,
            final QualityGateEvaluator evaluator) {
        QualityGateResult result = evaluator.evaluate(statistics);
        assertThat(result).hasOverallStatus(QualityGateStatus.FAILED).isNotSuccessful().hasMessages(
                "-> [Overall project - File]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Overall project - Line]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)");
    }
}
