package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import static io.jenkins.plugins.coverage.model.Assertions.*;

class QualityGateTest {

    @Test
    void warningLimitShouldBeHigherThanFailedLimit() {
        int failedLimit = 40;
        int warningLimit = 60;
        QualityGate qualityGateRight = new QualityGate(warningLimit, failedLimit, CoverageMetric.LINE);
        QualityGate qualityGateWrong = new QualityGate(failedLimit, warningLimit, CoverageMetric.LINE);

        assertThat(qualityGateRight.getFailedLimit()).isEqualTo(failedLimit);
        assertThat(qualityGateRight.getWarningLimit()).isEqualTo(warningLimit);

        assertThat(qualityGateWrong.getFailedLimit()).isEqualTo(failedLimit);
        assertThat(qualityGateWrong.getWarningLimit()).isEqualTo(warningLimit);
    }
}