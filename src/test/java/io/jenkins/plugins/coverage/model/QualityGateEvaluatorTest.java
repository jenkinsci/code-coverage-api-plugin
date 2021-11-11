package io.jenkins.plugins.coverage.model;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class QualityGateEvaluatorTest extends AbstractCoverageTest {
    private static final String PROJECT_NAME = "Java coding style: jacoco-codingstyle.xml";

    @Test
    void shouldBeInactiveIfGatesAreEmpty() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();

        QualityGateStatus result = sut.evaluate(stats, logger);

        assertThat(result).isEqualTo(QualityGateStatus.INACTIVE);
    }

    @Test
    void shouldPassAll() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();
        sut.add(0, QualityGate.QualityGateType.FILE, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0, QualityGate.QualityGateType.METHOD, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0, QualityGate.QualityGateType.MODULE, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0, QualityGate.QualityGateType.INSTRUCTION, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0, QualityGate.QualityGateType.LINE, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0, QualityGate.QualityGateType.BRANCH, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0, QualityGate.QualityGateType.PACKAGE, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0, QualityGate.QualityGateType.CLASS, QualityGate.QualityGateResult.UNSTABLE);

        QualityGateStatus result = sut.evaluate(stats, logger);

        assertThat(result).isEqualTo(QualityGateStatus.PASSED);
    }

    @Test
    void evaluateWarning() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();
        sut.add(0.9, QualityGate.QualityGateType.FILE, QualityGate.QualityGateResult.UNSTABLE);

        QualityGateStatus result = sut.evaluate(stats, logger);

        assertThat(result).isEqualTo(QualityGateStatus.WARNING);
    }

    @Test
    void evaluatePassed() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();
        sut.add(0.3, QualityGate.QualityGateType.FILE, QualityGate.QualityGateResult.UNSTABLE);

        QualityGateStatus result = sut.evaluate(stats, logger);

        assertThat(result).isEqualTo(QualityGateStatus.PASSED);
    }

    @Test
    void evaluateFailed() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();
        sut.add(1, QualityGate.QualityGateType.FILE, QualityGate.QualityGateResult.FAILURE);

        QualityGateStatus result = sut.evaluate(stats, logger);

        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
    }

    @Test
    void shouldChangeStatus() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();
        sut.add(0.8, QualityGate.QualityGateType.FILE, QualityGate.QualityGateResult.UNSTABLE);
        sut.add(0.5, QualityGate.QualityGateType.BRANCH, QualityGate.QualityGateResult.FAILURE);
        sut.add(0.95, QualityGate.QualityGateType.INSTRUCTION, QualityGate.QualityGateResult.FAILURE);

        QualityGateStatus result = sut.evaluate(stats, logger);

        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
    }

    @Test
    void shouldBeEnabled() {
        CoverageNode tree = readExampleReport();

        QualityGateEvaluator sut = new QualityGateEvaluator();
        sut.add(0.8, QualityGate.QualityGateType.FILE, QualityGate.QualityGateResult.UNSTABLE);
        boolean result = sut.isEnabled();

        assertThat(result).isEqualTo(true);
    }

    @Test
    void shouldBeDisabled() {
        CoverageNode tree = readExampleReport();

        QualityGateEvaluator sut = new QualityGateEvaluator();

        assertThat(sut.isEnabled()).isEqualTo(false);
    }

    @Test
    void shouldAddAll() {
        CoverageNode tree = readExampleReport();

        QualityGateEvaluator sut = new QualityGateEvaluator();
        List<QualityGate> qualityGates = new ArrayList<>();
        qualityGates.add(new QualityGate(0.8, QualityGate.QualityGateType.FILE,
                QualityGate.QualityGateResult.UNSTABLE));
        qualityGates.add(new QualityGate(0.5,
                QualityGate.QualityGateType.BRANCH, QualityGate.QualityGateResult.FAILURE));
        qualityGates.add(new QualityGate(0.95,
                QualityGate.QualityGateType.INSTRUCTION, QualityGate.QualityGateResult.FAILURE));

        sut.addAll(qualityGates);

        assertThat(sut.isEnabled()).isEqualTo(true);
    }

    private CoverageNode readExampleReport() {
        return CoverageNodeConverter.convert(readResult("jacoco-codingstyle.xml"));
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