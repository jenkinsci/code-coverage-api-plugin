package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.exception.QualityGatesInvalidException;
import io.jenkins.plugins.coverage.model.CoverageEvaluator.FormattedLogger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoverageEvaluatorTest extends AbstractCoverageTest {

    @Test
    void shouldReturnWarningLine() throws QualityGatesInvalidException {
        Logger logger = new Logger();
        CoverageNode node = getCoverageNode();
        CoverageEvaluator evaluator = new CoverageEvaluator();
        evaluator.add(new QualityGate(CoverageMetric.LINE, 96, QualityGateStatus.WARNING));
        evaluator.add(new QualityGate(CoverageMetric.LINE, 95, QualityGateStatus.FAILED));


        QualityGateStatus buildStatus = evaluator.evaluate(node, logger);
        assertThat(buildStatus).isEqualTo(QualityGateStatus.FAILED);


    }

    private CoverageNode getCoverageNode() {
        CoverageNode rootMock = mock(CoverageNode.class);
        SortedMap<CoverageMetric, Double> metrics = new TreeMap<>();
        metrics.put(CoverageMetric.PACKAGE, 50.00);
        metrics.put(CoverageMetric.FILE, 60.00);
        metrics.put(CoverageMetric.CLASS, 70.00);
        metrics.put(CoverageMetric.METHOD, 80.00);
        metrics.put(CoverageMetric.INSTRUCTION, 85.00);
        metrics.put(CoverageMetric.LINE, 90.00);
        metrics.put(CoverageMetric.BRANCH, 95.00);
        when(rootMock.getMetricPercentages()).thenReturn(metrics);
        return rootMock;
    }

    @Test
    void shouldReturnWarningPackage() throws QualityGatesInvalidException {
        Logger logger = new Logger();
        CoverageNode node = getCoverageNode();
        CoverageEvaluator evaluator = new CoverageEvaluator();
        evaluator.add(new QualityGate(CoverageMetric.PACKAGE, 30, QualityGateStatus.FAILED));
        evaluator.add(new QualityGate(CoverageMetric.PACKAGE, 60, QualityGateStatus.WARNING));
        QualityGateStatus buildStatus = evaluator.evaluate(node, logger);
        assertThat(buildStatus).isEqualTo(QualityGateStatus.WARNING);
    }

    @Test
    void shouldSuccess() throws QualityGatesInvalidException {
        Logger logger = new Logger();
        CoverageNode node = getCoverageNode();
        CoverageEvaluator evaluator = new CoverageEvaluator();

        evaluator.add(new QualityGate(CoverageMetric.INSTRUCTION, 30, QualityGateStatus.FAILED));
        evaluator.add(new QualityGate(CoverageMetric.LINE, 30, QualityGateStatus.FAILED));
        evaluator.add(new QualityGate(CoverageMetric.PACKAGE, 40, QualityGateStatus.WARNING));
        evaluator.add(new QualityGate(CoverageMetric.BRANCH, 40, QualityGateStatus.WARNING));

        QualityGateStatus buildStatus = evaluator.evaluate(node, logger);
        String[] loggerValues
                   = {"-> PASSED - Quality Gate: (FAILED, 30.000, Line); ACHIEVED: 90.000",
                      "-> PASSED - Quality Gate: (FAILED, 30.000, Instruction); ACHIEVED: 85.000",
                      "-> PASSED - Quality Gate: (WARNING, 40.000, Package); ACHIEVED: 50.000",
                      "-> PASSED - Quality Gate: (WARNING, 40.000, Branch); ACHIEVED: 95.000"};

        assertThat(logger.getMessages()).containsExactlyElementsOf(Arrays.asList(loggerValues));
        logger.clear();
        assertThat(buildStatus).isEqualTo(QualityGateStatus.SUCCESSFUL);
    }

    @Test
    void shouldReturnFailFile() throws QualityGatesInvalidException {
        Logger logger = new Logger();
        CoverageNode node = getCoverageNode();
        CoverageEvaluator evaluator = new CoverageEvaluator();

        evaluator.add(new QualityGate(CoverageMetric.FILE, 70, QualityGateStatus.FAILED));
        evaluator.add(new QualityGate(CoverageMetric.FILE, 75, QualityGateStatus.WARNING));

        QualityGateStatus buildStatus = evaluator.evaluate(node, logger);
        assertThat(buildStatus).isEqualTo(QualityGateStatus.FAILED);
        String[] loggerValues
                   = {"-> NOT PASSED: FAILED - Quality Gate: (FAILED, 70.000, File); ACHIEVED: 60.000"};

        assertThat(logger.getMessages()).containsExactlyElementsOf(Arrays.asList(loggerValues));
        logger.clear();
    }

    @Test
    void shouldReturnFailMethod() throws QualityGatesInvalidException {
        Logger logger = new Logger();
        CoverageNode node = getCoverageNode();
        CoverageEvaluator evaluator = new CoverageEvaluator();
        evaluator.add(new QualityGate(CoverageMetric.METHOD, 85, QualityGateStatus.FAILED));
        evaluator.add(new QualityGate(CoverageMetric.METHOD, 90, QualityGateStatus.WARNING));
        QualityGateStatus buildStatus = evaluator.evaluate(node, logger);
        assertThat(buildStatus).isEqualTo(QualityGateStatus.FAILED);
        String[] loggerValues
                   = {"-> NOT PASSED: FAILED - Quality Gate: (FAILED, 85.000, Method); ACHIEVED: 80.000"};
        assertThat(logger.getMessages()).containsExactlyElementsOf(Arrays.asList(loggerValues));
        logger.clear();
    }

    @Test
    void shouldReturnInactive() {
        Logger logger = new Logger();
        CoverageNode node = getCoverageNode();
        CoverageEvaluator evaluator = new CoverageEvaluator();
        QualityGateStatus buildStatus = evaluator.evaluate(node, logger);
        assertThat(buildStatus).isEqualTo(QualityGateStatus.INACTIVE);
        String[] loggerValues
                   = {"-> INACTIVE - No quality gate defined"};

        assertThat(logger.getMessages()).containsExactlyElementsOf(Arrays.asList(loggerValues));
        logger.clear();
    }

    @Test
    void shouldAddOneQualityGateToEmptyList() throws QualityGatesInvalidException {
        CoverageEvaluator evaluator = new CoverageEvaluator();
        QualityGate gate = new QualityGate(CoverageMetric.LINE, 50, QualityGateStatus.WARNING);
        evaluator.add(gate);
        assertThat(evaluator.getQualityGateSet()).hasSize(1);
    }

    @Test
    void shouldAddThreeOutOfFourQualityGatesFromList() throws QualityGatesInvalidException {
        CoverageEvaluator evaluator = new CoverageEvaluator();
        evaluator.addAll(Arrays.asList(new QualityGate(CoverageMetric.LINE, 50, QualityGateStatus.WARNING),
                new QualityGate(CoverageMetric.LINE, 30, QualityGateStatus.WARNING),
                new QualityGate(CoverageMetric.BRANCH, 10, QualityGateStatus.WARNING),
                new QualityGate(CoverageMetric.BRANCH, 10, QualityGateStatus.FAILED)));
        assertThat(evaluator.getQualityGateSet()).hasSize(3);
    }

    @Test
    void shouldRemoveAQualityGate() throws QualityGatesInvalidException {
        CoverageEvaluator evaluator = new CoverageEvaluator();
        QualityGate gate = new QualityGate(CoverageMetric.LINE, 50, QualityGateStatus.WARNING);
        evaluator.add(gate);
        evaluator.remove(gate);
        assertThat(evaluator.getQualityGateSet()).isEmpty();
    }

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