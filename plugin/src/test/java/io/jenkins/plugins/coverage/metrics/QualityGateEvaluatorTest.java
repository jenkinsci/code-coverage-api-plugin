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

import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateResult;

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
        evaluator.add(0, Metric.FILE, Baseline.PROJECT, QualityGateResult.UNSTABLE);
        evaluator.add(0, Metric.LINE, Baseline.PROJECT, QualityGateResult.UNSTABLE);
        evaluator.add(0, Metric.FILE, Baseline.CHANGE, QualityGateResult.UNSTABLE);
        evaluator.add(0, Metric.LINE, Baseline.CHANGE, QualityGateResult.UNSTABLE);
        evaluator.add(0, Metric.FILE, Baseline.FILE, QualityGateResult.UNSTABLE);
        evaluator.add(0, Metric.LINE, Baseline.FILE, QualityGateResult.UNSTABLE);

        var minimum = -10;
        evaluator.add(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.CHANGE_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.CHANGE_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.FILE_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.FILE_DELTA, QualityGateResult.UNSTABLE);

        assertThat(evaluator.isEnabled()).isEqualTo(true);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);

        assertThat(result).isEqualTo(QualityGateStatus.PASSED);
        assertThat(logger.getMessages()).containsExactly(
                "-> <PROJECT:FILE> PASSED - FILE: 75.00% (3/4) (Quality Gate: 0.00)",
                "-> <PROJECT:LINE> PASSED - LINE: 50.00% (2/4) (Quality Gate: 0.00)",
                "-> <CHANGE:FILE> PASSED - FILE: 75.00% (3/4) (Quality Gate: 0.00)",
                "-> <CHANGE:LINE> PASSED - LINE: 50.00% (2/4) (Quality Gate: 0.00)",
                "-> <FILE:FILE> PASSED - FILE: 75.00% (3/4) (Quality Gate: 0.00)",
                "-> <FILE:LINE> PASSED - LINE: 50.00% (2/4) (Quality Gate: 0.00)",
                "-> <PROJECT_DELTA:FILE> PASSED - FILE: -10/1 (Quality Gate: -10.00)",
                "-> <PROJECT_DELTA:LINE> PASSED - LINE: 5/1 (Quality Gate: -10.00)",
                "-> <CHANGE_DELTA:FILE> PASSED - FILE: -10/1 (Quality Gate: -10.00)",
                "-> <CHANGE_DELTA:LINE> PASSED - LINE: 5/1 (Quality Gate: -10.00)",
                "-> <FILE_DELTA:FILE> PASSED - FILE: -10/1 (Quality Gate: -10.00)",
                "-> <FILE_DELTA:LINE> PASSED - LINE: 5/1 (Quality Gate: -10.00)");
    }

    @Test
    void shouldReportWarningIfBelowThreshold() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(76.0, Metric.FILE, Baseline.PROJECT, QualityGateResult.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.PROJECT, QualityGateResult.UNSTABLE);
        evaluator.add(76.0, Metric.FILE, Baseline.CHANGE, QualityGateResult.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.CHANGE, QualityGateResult.UNSTABLE);
        evaluator.add(76.0, Metric.FILE, Baseline.FILE, QualityGateResult.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.FILE, QualityGateResult.UNSTABLE);

        var minimum = 10;
        evaluator.add(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.CHANGE_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.CHANGE_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.FILE, Baseline.FILE_DELTA, QualityGateResult.UNSTABLE);
        evaluator.add(minimum, Metric.LINE, Baseline.FILE_DELTA, QualityGateResult.UNSTABLE);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);

        assertThat(result).isEqualTo(QualityGateStatus.WARNING);
        assertThat(logger.getMessages()).containsExactly(
                "-> <PROJECT:FILE> WARNING - FILE: 75.00% (3/4) (Quality Gate: 76.00)",
                "-> <PROJECT:LINE> WARNING - LINE: 50.00% (2/4) (Quality Gate: 51.00)",
                "-> <CHANGE:FILE> WARNING - FILE: 75.00% (3/4) (Quality Gate: 76.00)",
                "-> <CHANGE:LINE> WARNING - LINE: 50.00% (2/4) (Quality Gate: 51.00)",
                "-> <FILE:FILE> WARNING - FILE: 75.00% (3/4) (Quality Gate: 76.00)",
                "-> <FILE:LINE> WARNING - LINE: 50.00% (2/4) (Quality Gate: 51.00)",
                "-> <PROJECT_DELTA:FILE> WARNING - FILE: -10/1 (Quality Gate: 10.00)",
                "-> <PROJECT_DELTA:LINE> WARNING - LINE: 5/1 (Quality Gate: 10.00)",
                "-> <CHANGE_DELTA:FILE> WARNING - FILE: -10/1 (Quality Gate: 10.00)",
                "-> <CHANGE_DELTA:LINE> WARNING - LINE: 5/1 (Quality Gate: 10.00)",
                "-> <FILE_DELTA:FILE> WARNING - FILE: -10/1 (Quality Gate: 10.00)",
                "-> <FILE_DELTA:LINE> WARNING - LINE: 5/1 (Quality Gate: 10.00)");
    }

    @Test
    void shouldReportFailureIfBelowThreshold() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(76.0, Metric.FILE, Baseline.PROJECT, QualityGateResult.FAILURE);
        evaluator.add(51.0, Metric.LINE, Baseline.PROJECT, QualityGateResult.FAILURE);
        evaluator.add(76.0, Metric.FILE, Baseline.CHANGE, QualityGateResult.FAILURE);
        evaluator.add(51.0, Metric.LINE, Baseline.CHANGE, QualityGateResult.FAILURE);
        evaluator.add(76.0, Metric.FILE, Baseline.FILE, QualityGateResult.FAILURE);
        evaluator.add(51.0, Metric.LINE, Baseline.FILE, QualityGateResult.FAILURE);

        var minimum = 10;
        evaluator.add(minimum, Metric.FILE, Baseline.PROJECT_DELTA, QualityGateResult.FAILURE);
        evaluator.add(minimum, Metric.LINE, Baseline.PROJECT_DELTA, QualityGateResult.FAILURE);
        evaluator.add(minimum, Metric.FILE, Baseline.CHANGE_DELTA, QualityGateResult.FAILURE);
        evaluator.add(minimum, Metric.LINE, Baseline.CHANGE_DELTA, QualityGateResult.FAILURE);
        evaluator.add(minimum, Metric.FILE, Baseline.FILE_DELTA, QualityGateResult.FAILURE);
        evaluator.add(minimum, Metric.LINE, Baseline.FILE_DELTA, QualityGateResult.FAILURE);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);

        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
        assertThat(logger.getMessages()).containsExactly(
                "-> <PROJECT:FILE> FAILED - FILE: 75.00% (3/4) (Quality Gate: 76.00)",
                "-> <PROJECT:LINE> FAILED - LINE: 50.00% (2/4) (Quality Gate: 51.00)",
                "-> <CHANGE:FILE> FAILED - FILE: 75.00% (3/4) (Quality Gate: 76.00)",
                "-> <CHANGE:LINE> FAILED - LINE: 50.00% (2/4) (Quality Gate: 51.00)",
                "-> <FILE:FILE> FAILED - FILE: 75.00% (3/4) (Quality Gate: 76.00)",
                "-> <FILE:LINE> FAILED - LINE: 50.00% (2/4) (Quality Gate: 51.00)",
                "-> <PROJECT_DELTA:FILE> FAILED - FILE: -10/1 (Quality Gate: 10.00)",
                "-> <PROJECT_DELTA:LINE> FAILED - LINE: 5/1 (Quality Gate: 10.00)",
                "-> <CHANGE_DELTA:FILE> FAILED - FILE: -10/1 (Quality Gate: 10.00)",
                "-> <CHANGE_DELTA:LINE> FAILED - LINE: 5/1 (Quality Gate: 10.00)",
                "-> <FILE_DELTA:FILE> FAILED - FILE: -10/1 (Quality Gate: 10.00)",
                "-> <FILE_DELTA:LINE> FAILED - LINE: 5/1 (Quality Gate: 10.00)");
    }

    @Test
    void shouldOverwriteStatus() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(76.0, Metric.FILE, Baseline.PROJECT, QualityGateResult.UNSTABLE);
        evaluator.add(51.0, Metric.LINE, Baseline.PROJECT, QualityGateResult.FAILURE);

        assertThatStatusWillBeOverwritten(logger, statistics, evaluator);
    }

    @Test
    void shouldFailIfValueIsNotFound() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.add(50.0, Metric.PACKAGE, Baseline.PROJECT, QualityGateResult.FAILURE);

        QualityGateStatus result = evaluator.evaluate(statistics, logger);
        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
        assertThat(logger.getMessages()).containsExactly(
                "-> <PROJECT:PACKAGE> FAILED - n/a (Quality Gate: 50.00)");
    }

    private static void assertThatStatusWillBeOverwritten(final Logger logger, final CoverageStatistics statistics,
            final QualityGateEvaluator evaluator) {
        QualityGateStatus result = evaluator.evaluate(statistics, logger);
        assertThat(result).isEqualTo(QualityGateStatus.FAILED);
        assertThat(logger.getMessages()).containsExactly(
                "-> <PROJECT:FILE> WARNING - FILE: 75.00% (3/4) (Quality Gate: 76.00)",
                "-> <PROJECT:LINE> FAILED - LINE: 50.00% (2/4) (Quality Gate: 51.00)");
    }

    @Test
    void shouldAddAllQualityGates() {
        Logger logger = new Logger();
        CoverageStatistics statistics = createStatistics();

        List<QualityGate> qualityGates = List.of(
                new QualityGate(76.0, Metric.FILE, Baseline.PROJECT, QualityGateResult.UNSTABLE),
                new QualityGate(51.0, Metric.LINE, Baseline.PROJECT, QualityGateResult.FAILURE));

        QualityGateEvaluator evaluator = new QualityGateEvaluator();
        evaluator.addAll(qualityGates);

        assertThatStatusWillBeOverwritten(logger, statistics, evaluator);
    }

    private CoverageStatistics createStatistics() {
        return new CoverageStatistics(fillValues(), fillDeltas(),
                fillValues(), fillDeltas(),
                fillValues(), fillDeltas());
    }

    private NavigableMap<Metric, Value> fillValues() {
        final NavigableMap<Metric, Value> valueMapping = new TreeMap<>();
        var builder = new CoverageBuilder();
        valueMapping.put(Metric.FILE, builder.setMetric(Metric.FILE).setCovered(3).setMissed(1).build());
        valueMapping.put(Metric.LINE, builder.setMetric(Metric.LINE).setCovered(2).setMissed(2).build());
        return valueMapping;
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
