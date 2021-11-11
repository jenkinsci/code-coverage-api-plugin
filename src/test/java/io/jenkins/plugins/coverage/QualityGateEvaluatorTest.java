package io.jenkins.plugins.coverage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.QualityGateEvaluator.FormattedLogger;
import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link QualityGateEvaluator}.
 *
 * @author Thomas Willeit
 */
@SuppressWarnings("PMD.MoreThanOneLogger")
class QualityGateEvaluatorTest {
    @Test
    void shouldBeInactiveIfGatesAreEmpty() {
        Logger logger = new Logger();
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();
        CoverageNode node = mock(CoverageNode.class);

        assertThat(qualityGateEvaluator.evaluate(node, logger)).isEqualTo(QualityGateStatus.INACTIVE);
        assertThat(logger.getMessages()).containsExactly("-> INACTIVE - No quality gate defined");
        assertThat(qualityGateEvaluator.qualityGates.isEmpty()).isTrue();
    }

    @Test
    void shouldAddOneQualityGate() {
        QualityGate qualityGate = mock(QualityGate.class);
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();
        qualityGateEvaluator.add(qualityGate);

        assertThat(qualityGateEvaluator.qualityGates.isEmpty()).isFalse();
        assertThat(qualityGateEvaluator.qualityGates.size()).isEqualTo(1);
    }

    @Test
    void shouldAddAllQualityGate() {
        QualityGate qualityGate1 = mock(QualityGate.class);
        QualityGate qualityGate2 = mock(QualityGate.class);
        QualityGate qualityGate3 = mock(QualityGate.class);

        List qualityGates = new ArrayList();
        qualityGates.add(qualityGate1);
        qualityGates.add(qualityGate2);
        qualityGates.add(qualityGate3);

        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();
        qualityGateEvaluator.addAll(qualityGates);

        assertThat(qualityGateEvaluator.qualityGates.isEmpty()).isFalse();
        assertThat(qualityGateEvaluator.qualityGates.size()).isEqualTo(3);
    }


    @Test
    void shouldRemoveOneQualityGate() {
        QualityGate qualityGate1 = mock(QualityGate.class);
        QualityGate qualityGate2 = mock(QualityGate.class);
        QualityGate qualityGate3 = mock(QualityGate.class);

        List qualityGates = new ArrayList();
        qualityGates.add(qualityGate1);
        qualityGates.add(qualityGate2);
        qualityGates.add(qualityGate3);

        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();
        qualityGateEvaluator.addAll(qualityGates);

        qualityGateEvaluator.remove(qualityGate3);

        assertThat(qualityGateEvaluator.qualityGates.isEmpty()).isFalse();
        assertThat(qualityGateEvaluator.qualityGates.size()).isEqualTo(2);
    }


    @Test
    void testSingleQualityGateShouldFail(){
        Logger logger = new Logger();
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();
        QualityGate qualityGate = new QualityGate(CoverageMetric.LINE, 50.0, 5.0);
        qualityGateEvaluator.add(qualityGate);


        CoverageNode coverageNode = mock(CoverageNode.class);
        Map<CoverageMetric, Double> coverage = new TreeMap<CoverageMetric, Double>();
        coverage.put(CoverageMetric.LINE, 51.0);
        when(coverageNode.getMetricPercentages()).thenReturn((SortedMap<CoverageMetric, Double>) coverage);


        QualityGateStatus qualityGateStatus = qualityGateEvaluator.evaluate(coverageNode, logger);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.PASSED);
    }

    @Test
    void testSingleQualityGateShouldWarn(){
        Logger logger = new Logger();
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();
        QualityGate qualityGate = new QualityGate(CoverageMetric.LINE, 50.0, 5.0);
        qualityGateEvaluator.add(qualityGate);


        CoverageNode coverageNode = mock(CoverageNode.class);
        Map<CoverageMetric, Double> coverage = new TreeMap<CoverageMetric, Double>();
        coverage.put(CoverageMetric.LINE, 50.0);
        when(coverageNode.getMetricPercentages()).thenReturn((SortedMap<CoverageMetric, Double>) coverage);


        QualityGateStatus qualityGateStatus = qualityGateEvaluator.evaluate(coverageNode, logger);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.WARNING);
    }

    @Test
    void testSingleQualityGateShouldPass(){
        Logger logger = new Logger();
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();
        QualityGate qualityGate = new QualityGate(CoverageMetric.LINE, 50.0, 5.0);
        qualityGateEvaluator.add(qualityGate);


        CoverageNode coverageNode = mock(CoverageNode.class);
        Map<CoverageMetric, Double> coverage = new TreeMap<CoverageMetric, Double>();
        coverage.put(CoverageMetric.LINE, 5.0);
        when(coverageNode.getMetricPercentages()).thenReturn((SortedMap<CoverageMetric, Double>) coverage);


        QualityGateStatus qualityGateStatus = qualityGateEvaluator.evaluate(coverageNode, logger);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.FAILED);
    }

    @Test
    void testMultipleQualityGatesShouldWarn(){
        Logger logger = new Logger();
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();

        QualityGate qualityGate1 = new QualityGate(CoverageMetric.LINE, 50.0, 5.0);
        QualityGate qualityGate2 = new QualityGate(CoverageMetric.BRANCH, 50.0, 5.0);
        qualityGateEvaluator.add(qualityGate1);
        qualityGateEvaluator.add(qualityGate2);


        CoverageNode coverageNode = mock(CoverageNode.class);
        Map<CoverageMetric, Double> coverage = new TreeMap<CoverageMetric, Double>();
        coverage.put(CoverageMetric.LINE, 51.0);
        coverage.put(CoverageMetric.BRANCH, 50.0); // only Branch Coverage is not passing, still results in Warning
        when(coverageNode.getMetricPercentages()).thenReturn((SortedMap<CoverageMetric, Double>) coverage);


        QualityGateStatus qualityGateStatus = qualityGateEvaluator.evaluate(coverageNode, logger);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.WARNING);
    }

    @Test
    void testMultipleQualityGatesShouldFail(){
        Logger logger = new Logger();
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();

        QualityGate qualityGate1 = new QualityGate(CoverageMetric.LINE, 50.0, 5.0);
        QualityGate qualityGate2 = new QualityGate(CoverageMetric.BRANCH, 50.0, 5.0);
        qualityGateEvaluator.add(qualityGate1);
        qualityGateEvaluator.add(qualityGate2);


        CoverageNode coverageNode = mock(CoverageNode.class);
        Map<CoverageMetric, Double> coverage = new TreeMap<CoverageMetric, Double>();
        coverage.put(CoverageMetric.LINE, 51.0);
        coverage.put(CoverageMetric.BRANCH, 5.0); // only Branch Coverage fails, still results in failing
        when(coverageNode.getMetricPercentages()).thenReturn((SortedMap<CoverageMetric, Double>) coverage);


        QualityGateStatus qualityGateStatus = qualityGateEvaluator.evaluate(coverageNode, logger);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.FAILED);
    }


    @Test
    void testMultipleQualityGatesShouldPass(){
        Logger logger = new Logger();
        QualityGateEvaluator qualityGateEvaluator = new QualityGateEvaluator();

        QualityGate qualityGate1 = new QualityGate(CoverageMetric.LINE, 50.0, 5.0);
        QualityGate qualityGate2 = new QualityGate(CoverageMetric.BRANCH, 50.0, 5.0);
        qualityGateEvaluator.add(qualityGate1);
        qualityGateEvaluator.add(qualityGate2);


        CoverageNode coverageNode = mock(CoverageNode.class);
        Map<CoverageMetric, Double> coverage = new TreeMap<CoverageMetric, Double>();
        coverage.put(CoverageMetric.LINE, 51.0);
        coverage.put(CoverageMetric.BRANCH, 51.0);
        when(coverageNode.getMetricPercentages()).thenReturn((SortedMap<CoverageMetric, Double>) coverage);


        QualityGateStatus qualityGateStatus = qualityGateEvaluator.evaluate(coverageNode, logger);
        assertThat(qualityGateStatus).isEqualTo(QualityGateStatus.PASSED);
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