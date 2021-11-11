package io.jenkins.plugins.coverage.model;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QualityGateEvaluatorTest extends AbstractCoverageTest {
    private static final String PROJECT_NAME = "Java coding style: jacoco-codingstyle.xml";

    @Test
    void shouldBeInactiveIfGatesAreEmpty() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        verifyCoverageMetrics(tree);
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();

        QualityGateStatus result = sut.evaluate(stats, logger);

        assertThat(result).isEqualTo(QualityGateStatus.INACTIVE);
    }

    @Test
    void shouldPassAll() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        verifyCoverageMetrics(tree);
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
        verifyCoverageMetrics(tree);
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
        verifyCoverageMetrics(tree);
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
        verifyCoverageMetrics(tree);
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
        verifyCoverageMetrics(tree);
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
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        verifyCoverageMetrics(tree);
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();
        sut.add(0.8, QualityGate.QualityGateType.FILE, QualityGate.QualityGateResult.UNSTABLE);
        boolean result = sut.isEnabled();

        assertThat(result).isEqualTo(true);
    }

    @Test
    void shouldBeDisabled() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        verifyCoverageMetrics(tree);
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

        QualityGateEvaluator sut = new QualityGateEvaluator();

        assertThat(sut.isEnabled()).isEqualTo(false);
    }

    @Test
    void shouldAddAll() {
        Logger logger = new Logger();
        CoverageNode tree = readExampleReport();
        verifyCoverageMetrics(tree);
        CoverageStatistics stats = new CoverageStatistics(tree.getMetricsDistribution());

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




    private void verifyCoverageMetrics(final CoverageNode tree) {
        Assertions.assertThat(tree.getCoverage(LINE)).isSet()
                .hasCovered(294)
                .hasCoveredPercentageCloseTo(0.91, PRECISION)
                .hasMissed(29)
                .hasMissedPercentageCloseTo(0.09, PRECISION)
                .hasTotal(294 + 29);
        assertThat(tree.printCoverageFor(LINE)).isEqualTo("91.02%");

        Assertions.assertThat(tree.getCoverage(BRANCH)).isSet()
                .hasCovered(109)
                .hasCoveredPercentageCloseTo(0.93, PRECISION)
                .hasMissed(7)
                .hasMissedPercentageCloseTo(0.07, PRECISION)
                .hasTotal(109 + 7);
        assertThat(tree.printCoverageFor(BRANCH)).isEqualTo("93.97%");

        Assertions.assertThat(tree.getCoverage(INSTRUCTION)).isSet()
                .hasCovered(1260)
                .hasCoveredPercentageCloseTo(0.93, PRECISION)
                .hasMissed(90)
                .hasMissedPercentageCloseTo(0.07, PRECISION)
                .hasTotal(1260 + 90);
        assertThat(tree.printCoverageFor(INSTRUCTION)).isEqualTo("93.33%");

        Assertions.assertThat(tree.getCoverage(MODULE)).isSet()
                .hasCovered(1)
                .hasCoveredPercentageCloseTo(1, PRECISION)
                .hasMissed(0)
                .hasMissedPercentageCloseTo(0, PRECISION)
                .hasTotal(1);
        assertThat(tree.printCoverageFor(MODULE)).isEqualTo("100.00%");

        Assertions.assertThat(tree).hasName(PROJECT_NAME)
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName(CoverageNode.ROOT);
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