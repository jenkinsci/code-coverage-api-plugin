package io.jenkins.plugins.coverage;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageMetric;

import static org.assertj.core.api.Assertions.*;

// is class needed? QualityGateEvaluatorTest is already testing
class QualityGateTest {

    @Test
    void shouldCreateQualityGate() {
        CoverageMetric coverageMetric = CoverageMetric.LINE;
        Double warningThreshold = 50.0;
        Double failedThreshold = 5.0;

        QualityGate qualityGate = new QualityGate(coverageMetric, warningThreshold, failedThreshold);

        assertThat(qualityGate.getWarningThreshold()).isEqualTo(50.0);
        assertThat(qualityGate.getFailedThreshold()).isEqualTo(5.0);
        assertThat(qualityGate.getCoverageMetric()).isEqualTo(CoverageMetric.LINE);
    }
}