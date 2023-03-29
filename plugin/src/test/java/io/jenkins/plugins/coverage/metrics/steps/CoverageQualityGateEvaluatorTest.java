package io.jenkins.plugins.coverage.metrics.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.util.QualityGate.QualityGateCriticality;
import io.jenkins.plugins.util.QualityGateResult;
import io.jenkins.plugins.util.QualityGateStatus;

import static io.jenkins.plugins.util.assertions.Assertions.*;

class CoverageQualityGateEvaluatorTest extends AbstractCoverageTest {
    @Test
    void shouldBeInactiveIfGatesAreEmpty() {
        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(new ArrayList<>(), createStatistics());

        QualityGateResult result = evaluator.evaluate();

        assertThat(result).hasNoMessages().isInactive().isSuccessful().hasOverallStatus(QualityGateStatus.INACTIVE);
    }

    @Test
    void shouldPassForTooLowThresholds() {
        Collection<CoverageQualityGate> qualityGates = new ArrayList<>();

        qualityGates.add(new CoverageQualityGate(0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.FILE, Baseline.MODIFIED_LINES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.LINE, Baseline.MODIFIED_LINES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.FILE, Baseline.MODIFIED_FILES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.LINE, Baseline.MODIFIED_FILES, QualityGateCriticality.UNSTABLE));

        var minimum = -10;
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.MODIFIED_LINES_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.MODIFIED_LINES_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.MODIFIED_FILES_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.MODIFIED_FILES_DELTA, QualityGateCriticality.UNSTABLE));

        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(qualityGates, createStatistics());

        assertThat(evaluator).isEnabled();

        QualityGateResult result = evaluator.evaluate();

        assertThat(result).hasOverallStatus(QualityGateStatus.PASSED).isSuccessful().isNotInactive().hasMessages(
                "-> [Overall project - File Coverage]: ≪Success≫ - (Actual value: 75.00%, Quality gate: 0.00)",
                "-> [Overall project - Line Coverage]: ≪Success≫ - (Actual value: 50.00%, Quality gate: 0.00)",
                "-> [Modified code lines - File Coverage]: ≪Success≫ - (Actual value: 75.00%, Quality gate: 0.00)",
                "-> [Modified code lines - Line Coverage]: ≪Success≫ - (Actual value: 50.00%, Quality gate: 0.00)",
                "-> [Modified files - File Coverage]: ≪Success≫ - (Actual value: 75.00%, Quality gate: 0.00)",
                "-> [Modified files - Line Coverage]: ≪Success≫ - (Actual value: 50.00%, Quality gate: 0.00)",
                "-> [Overall project (difference to reference job) - File Coverage]: ≪Success≫ - (Actual value: -10.00%, Quality gate: -10.00)",
                "-> [Overall project (difference to reference job) - Line Coverage]: ≪Success≫ - (Actual value: +5.00%, Quality gate: -10.00)",
                "-> [Modified code lines (difference to modified files) - File Coverage]: ≪Success≫ - (Actual value: -10.00%, Quality gate: -10.00)",
                "-> [Modified code lines (difference to modified files) - Line Coverage]: ≪Success≫ - (Actual value: +5.00%, Quality gate: -10.00)",
                "-> [Modified files (difference to overall project) - File Coverage]: ≪Success≫ - (Actual value: -10.00%, Quality gate: -10.00)",
                "-> [Modified files (difference to overall project) - Line Coverage]: ≪Success≫ - (Actual value: +5.00%, Quality gate: -10.00)");
    }

    @Test
    void shouldSkipIfValueNotDefined() {
        Collection<CoverageQualityGate> qualityGates = new ArrayList<>();

        qualityGates.add(new CoverageQualityGate(0, Metric.FILE, Baseline.MODIFIED_LINES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.FILE, Baseline.MODIFIED_FILES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.FILE, Baseline.MODIFIED_LINES_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(0, Metric.LINE, Baseline.MODIFIED_FILES_DELTA, QualityGateCriticality.UNSTABLE));

        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(qualityGates, createOnlyProjectStatistics());

        assertThat(evaluator).isEnabled();

        QualityGateResult result = evaluator.evaluate();

        assertThat(result).hasOverallStatus(QualityGateStatus.INACTIVE).isInactive().hasMessages(
                "-> [Modified code lines - File Coverage]: ≪Not built≫ - (Actual value: n/a, Quality gate: 0.00)",
                "-> [Modified files - File Coverage]: ≪Not built≫ - (Actual value: n/a, Quality gate: 0.00)",
                "-> [Overall project (difference to reference job) - Line Coverage]: ≪Not built≫ - (Actual value: n/a, Quality gate: 0.00)",
                "-> [Modified code lines (difference to modified files) - File Coverage]: ≪Not built≫ - (Actual value: n/a, Quality gate: 0.00)",
                "-> [Modified files (difference to overall project) - Line Coverage]: ≪Not built≫ - (Actual value: n/a, Quality gate: 0.00)");
    }

    @Test
    void shouldReportUnstableIfBelowThreshold() {
        QualityGateResult result = createQualityGateResult();

        assertThat(result).hasOverallStatus(QualityGateStatus.WARNING).isNotSuccessful().isNotInactive().hasMessages(
                "-> [Overall project - File Coverage]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Overall project - Line Coverage]: ≪Unstable≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Modified code lines - File Coverage]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Modified code lines - Line Coverage]: ≪Unstable≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Modified files - File Coverage]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Modified files - Line Coverage]: ≪Unstable≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Overall project (difference to reference job) - File Coverage]: ≪Unstable≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Overall project (difference to reference job) - Line Coverage]: ≪Unstable≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Modified code lines (difference to modified files) - File Coverage]: ≪Unstable≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Modified code lines (difference to modified files) - Line Coverage]: ≪Unstable≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Modified files (difference to overall project) - File Coverage]: ≪Unstable≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Modified files (difference to overall project) - Line Coverage]: ≪Unstable≫ - (Actual value: +5.00%, Quality gate: 10.00)");
    }

    static QualityGateResult createQualityGateResult() {
        Collection<CoverageQualityGate> qualityGates = new ArrayList<>();

        qualityGates.add(new CoverageQualityGate(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(76.0, Metric.FILE, Baseline.MODIFIED_LINES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(51.0, Metric.LINE, Baseline.MODIFIED_LINES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(76.0, Metric.FILE, Baseline.MODIFIED_FILES, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(51.0, Metric.LINE, Baseline.MODIFIED_FILES, QualityGateCriticality.UNSTABLE));

        var minimum = 10;
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.MODIFIED_LINES_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.MODIFIED_LINES_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.MODIFIED_FILES_DELTA, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.MODIFIED_FILES_DELTA, QualityGateCriticality.UNSTABLE));

        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(qualityGates, createStatistics());
        return evaluator.evaluate();
    }

    @Test
    void shouldReportFailureIfBelowThreshold() {
        Collection<CoverageQualityGate> qualityGates = new ArrayList<>();

        qualityGates.add(new CoverageQualityGate(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(76.0, Metric.FILE, Baseline.MODIFIED_LINES, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(51.0, Metric.LINE, Baseline.MODIFIED_LINES, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(76.0, Metric.FILE, Baseline.MODIFIED_FILES, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(51.0, Metric.LINE, Baseline.MODIFIED_FILES, QualityGateCriticality.FAILURE));

        var minimum = 10;
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.MODIFIED_LINES_DELTA, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.MODIFIED_LINES_DELTA, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.FILE, Baseline.MODIFIED_FILES_DELTA, QualityGateCriticality.FAILURE));
        qualityGates.add(new CoverageQualityGate(minimum, Metric.LINE, Baseline.MODIFIED_FILES_DELTA, QualityGateCriticality.FAILURE));

        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(qualityGates, createStatistics());

        QualityGateResult result = evaluator.evaluate();

        assertThat(result).hasOverallStatus(QualityGateStatus.FAILED).isNotSuccessful().isNotInactive().hasMessages(
                "-> [Overall project - File Coverage]: ≪Failed≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Overall project - Line Coverage]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Modified code lines - File Coverage]: ≪Failed≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Modified code lines - Line Coverage]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Modified files - File Coverage]: ≪Failed≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Modified files - Line Coverage]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)",
                "-> [Overall project (difference to reference job) - File Coverage]: ≪Failed≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Overall project (difference to reference job) - Line Coverage]: ≪Failed≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Modified code lines (difference to modified files) - File Coverage]: ≪Failed≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Modified code lines (difference to modified files) - Line Coverage]: ≪Failed≫ - (Actual value: +5.00%, Quality gate: 10.00)",
                "-> [Modified files (difference to overall project) - File Coverage]: ≪Failed≫ - (Actual value: -10.00%, Quality gate: 10.00)",
                "-> [Modified files (difference to overall project) - Line Coverage]: ≪Failed≫ - (Actual value: +5.00%, Quality gate: 10.00)");
    }

    @Test
    void shouldOverwriteStatus() {
        Collection<CoverageQualityGate> qualityGates = new ArrayList<>();

        qualityGates.add(new CoverageQualityGate(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE));
        qualityGates.add(new CoverageQualityGate(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE));

        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(qualityGates, createStatistics());
        assertThatStatusWillBeOverwritten(evaluator);
    }

    @Test
    void shouldAddAllQualityGates() {

        Collection<CoverageQualityGate> qualityGates = List.of(
                new CoverageQualityGate(76.0, Metric.FILE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE),
                new CoverageQualityGate(51.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.FAILURE));

        CoverageQualityGateEvaluator evaluator = new CoverageQualityGateEvaluator(qualityGates, createStatistics());

        assertThatStatusWillBeOverwritten(evaluator);
    }

    private static void assertThatStatusWillBeOverwritten(final CoverageQualityGateEvaluator evaluator) {
        QualityGateResult result = evaluator.evaluate();
        assertThat(result).hasOverallStatus(QualityGateStatus.FAILED).isNotSuccessful().hasMessages(
                "-> [Overall project - File Coverage]: ≪Unstable≫ - (Actual value: 75.00%, Quality gate: 76.00)",
                "-> [Overall project - Line Coverage]: ≪Failed≫ - (Actual value: 50.00%, Quality gate: 51.00)");
    }
}
