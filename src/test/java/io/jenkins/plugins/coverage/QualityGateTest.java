package io.jenkins.plugins.coverage;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageMetric;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link QualityGate}.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
class QualityGateTest {

    @Test
    void shouldReturnThreshold() {
        CoverageMetric coverageMetric = mock(CoverageMetric.class);
        QualityGate qualityGate = new QualityGate(22.3, coverageMetric, true);
        assertThat(qualityGate.getThreshold()).isEqualTo(22.3);

        QualityGate qualityGate2 = new QualityGate(-52.1235076, coverageMetric, true);
        assertThat(qualityGate2.getThreshold()).isEqualTo(-52.1235076);
    }

    @Test
    void shouldReturnCoverageMetric() {
        CoverageMetric coverageMetric = mock(CoverageMetric.class);
        QualityGate qualityGate = new QualityGate(0.0, coverageMetric, true);
        assertThat(qualityGate.getCoverageMetric()).isEqualTo(coverageMetric);
    }

    @Test
    void shouldReturnStatusStable() {
        CoverageMetric coverageMetric = mock(CoverageMetric.class);
        QualityGate qualityGate = new QualityGate(0.0, coverageMetric, false);
        assertThat(qualityGate.getStatusIfNotPassedSuccessful()).isEqualTo(QualityGateStatus.FAILED);
    }

    @Test
    void shouldReturnStatusUnstable() {
        CoverageMetric coverageMetric = mock(CoverageMetric.class);
        QualityGate qualityGate = new QualityGate(0.0, coverageMetric, true);
        assertThat(qualityGate.getStatusIfNotPassedSuccessful()).isEqualTo(QualityGateStatus.WARNING);
    }
}
