package io.jenkins.plugins.coverage;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.QualityGateEvaluator.FormattedLogger;
import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QualityGateEvaluatorTest {

    @Test
    void shouldBeInactiveIfGatesAreEmpty() {
        Logger logger = new Logger();
        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        CoverageNode root = mock(CoverageNode.class);

        QualityGateStatus status = evaluator.evaluate(root, Collections.emptyList(), logger);

        assertThat(status).isEqualTo(QualityGateStatus.INACTIVE);
//        assertThat(logger.getMessages()).containsExactly("-> INACTIVE - No quality gate defined");
    }

    @Test
    void shouldBeFailedIfStableQualityGateFailed() {
        Logger logger = new Logger();
        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        root.setUncoveredLines(1);
        QualityGate qualityGateStable = new QualityGate(1, CoverageMetric.FILE, false);

        List<QualityGate> qualityGates = new ArrayList<>();
        qualityGates.add(qualityGateStable);

        QualityGateStatus status = evaluator.evaluate(root, qualityGates, logger);

        assertThat(status).isEqualTo(QualityGateStatus.FAILED);

    }

    @Test
    void shouldBeWarningIfUnstableQualityGateFailed() {
        Logger logger = new Logger();
        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        root.setUncoveredLines(1);
        QualityGate qualityGateStable = new QualityGate(1, CoverageMetric.FILE, true);

        List<QualityGate> qualityGates = new ArrayList<>();
        qualityGates.add(qualityGateStable);

        QualityGateStatus status = evaluator.evaluate(root, qualityGates, logger);

        assertThat(status).isEqualTo(QualityGateStatus.WARNING);
    }

    @Test
    void shouldBePassedIfQualityGateSucceeded() {
        Logger logger = new Logger();
        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        CoverageNode root = mock(CoverageNode.class);
        when(root.getCoverage(CoverageMetric.FILE)).thenReturn(new Coverage(100, 0));

        QualityGate qualityGateStable = new QualityGate(1, CoverageMetric.FILE, true);

        List<QualityGate> qualityGates = new ArrayList<>();
        qualityGates.add(qualityGateStable);

        QualityGateStatus status = evaluator.evaluate(root, qualityGates, logger);

        assertThat(status).isEqualTo(QualityGateStatus.PASSED);
    }

    @Test
    void shouldBePassedIfAllQualityGatesSucceeded() {
        Logger logger = new Logger();
        QualityGateEvaluator evaluator = new QualityGateEvaluator();

        CoverageNode root = mock(CoverageNode.class);
        when(root.getCoverage(CoverageMetric.FILE)).thenReturn(new Coverage(100, 0));
        when(root.getCoverage(CoverageMetric.BRANCH)).thenReturn(new Coverage(100, 0));

        // Min 90% of files need at least one line covered
        QualityGate qualityGateStableOne = new QualityGate(0.9f, CoverageMetric.FILE, true);
        QualityGate qualityGateStableTwo = new QualityGate(1, CoverageMetric.BRANCH, true);

        List<QualityGate> qualityGates = new ArrayList<>();
        qualityGates.add(qualityGateStableOne);
        qualityGates.add(qualityGateStableTwo);

        QualityGateStatus status = evaluator.evaluate(root, qualityGates, logger);

        assertThat(status).isEqualTo(QualityGateStatus.PASSED);
    }

    @Test
    void shouldBeInactiveIfAnyQualityGateMatchCoverageNode() {

    }

    @Test
    void shouldBeInactiveIfNoQualityGateMatchCoverageNode() {

    }

    /**
     * Logger for the tests that provides a way verify and clear the messages.
     */
    private static class Logger implements FormattedLogger {
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
