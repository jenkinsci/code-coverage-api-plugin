package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import io.jenkins.plugins.coverage.model.CoverageEvaluator.FormattedLogger;

import static org.assertj.core.api.Assertions.*;

class CoverageEvaluatorTest extends AbstractCoverageTest {
    @Test
    void testSingleFileQualityGate() {
        Logger logger = new Logger();
        CoverageNode coverageNode = CoverageNodeConverter.convert(readResult("jacoco-analysis-model.xml"));
        CoverageEvaluator evaluator = new CoverageEvaluator();
        evaluator.add(new QualityGate(0.95, 0.80, CoverageMetric.BRANCH));
        QualityGateStatus qualityGateStatus = evaluator.evaluate(coverageNode, logger);

        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.WARNING);
    }

    @Test
    void testMultipleQualityGates() {
        Logger logger = new Logger();
        CoverageNode coverageNode = CoverageNodeConverter.convert(readResult("jacoco-analysis-model.xml"));
        CoverageEvaluator evaluator = new CoverageEvaluator();
        List<QualityGate> qualityGates = new ArrayList<>();
        QualityGate lineQualityGate = new QualityGate(0.95, 0.80, CoverageMetric.LINE);
        qualityGates.add(lineQualityGate);
        qualityGates.add(new QualityGate(0.95, 0.80, CoverageMetric.METHOD));
        qualityGates.add(new QualityGate(0.95, 0.80, CoverageMetric.CLASS));
        evaluator.addAll(qualityGates);
        QualityGateStatus qualityGateStatus = evaluator.evaluate(coverageNode, logger);

        assertThat(qualityGates).hasSize(3);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.SUCCESSFUL);

        qualityGates.remove(lineQualityGate);
        qualityGateStatus = evaluator.evaluate(coverageNode, logger);
        assertThat(qualityGates).hasSize(2);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.SUCCESSFUL);

    }

    @Test
    void shouldFail() {
        Logger logger = new Logger();
        CoverageNode coverageNode = CoverageNodeConverter.convert(readResult("jacoco-analysis-model.xml"));
        CoverageEvaluator evaluator = new CoverageEvaluator();
        evaluator.add(new QualityGate(0.99, 0.80, CoverageMetric.FILE));
        evaluator.add(new QualityGate(0.99, 0.80, CoverageMetric.METHOD));
        evaluator.add(new QualityGate(0.99, 0.91, CoverageMetric.BRANCH));
        QualityGateStatus qualityGateStatus = evaluator.evaluate(coverageNode, logger);

        assertThat(logger.getMessages()).containsExactly(
                "-> FAILED - QualityGate: " + CoverageMetric.BRANCH.getName() + " - warn/fail/actual: " + 0.99 + "/"
                        + 0.91 + "/"
                        + 0.89);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.FAILED);
    }

    @Test
    void shouldBeInactive() {
        Logger logger = new Logger();
        CoverageNode coverageNode = CoverageNodeConverter.convert(readResult("jacoco-analysis-model.xml"));
        CoverageEvaluator evaluator = new CoverageEvaluator();
        QualityGateStatus qualityGateStatus = evaluator.evaluate(coverageNode, logger);

        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.INACTIVE);
    }

    //add addALL and remove tests

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