package io.jenkins.plugins.coverage;

import org.junit.jupiter.api.Test;

import hudson.model.Result;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link QualityGateStatus}.
 *
 * @author Thomas Willeit
 *
 *
 * This Test is not needed, because it only tests code that is not needed
 */
class QualityGateStatusTest {
    @Test
    void shouldIdentifySuccessfulStatus() {
        assertThat(QualityGateStatus.PASSED.isSuccessful()).isTrue();
        assertThat(QualityGateStatus.INACTIVE.isSuccessful()).isTrue();
        assertThat(QualityGateStatus.WARNING.isSuccessful()).isFalse();
        assertThat(QualityGateStatus.FAILED.isSuccessful()).isFalse();
    }

    @Test
    void shouldDefineOrder() {
        assertThat(QualityGateStatus.FAILED.isWorseThan(QualityGateStatus.INACTIVE)).isTrue();
        assertThat(QualityGateStatus.FAILED.isWorseThan(QualityGateStatus.PASSED)).isTrue();
        assertThat(QualityGateStatus.FAILED.isWorseThan(QualityGateStatus.WARNING)).isTrue();

        assertThat(QualityGateStatus.FAILED.isWorseThan(QualityGateStatus.FAILED)).isFalse();

        assertThat(QualityGateStatus.WARNING.isWorseThan(QualityGateStatus.INACTIVE)).isTrue();
        assertThat(QualityGateStatus.WARNING.isWorseThan(QualityGateStatus.PASSED)).isTrue();

        assertThat(QualityGateStatus.WARNING.isWorseThan(QualityGateStatus.FAILED)).isFalse();
        assertThat(QualityGateStatus.WARNING.isWorseThan(QualityGateStatus.WARNING)).isFalse();

        assertThat(QualityGateStatus.PASSED.isWorseThan(QualityGateStatus.INACTIVE)).isTrue();

        assertThat(QualityGateStatus.PASSED.isWorseThan(QualityGateStatus.PASSED)).isFalse();
        assertThat(QualityGateStatus.PASSED.isWorseThan(QualityGateStatus.FAILED)).isFalse();
        assertThat(QualityGateStatus.PASSED.isWorseThan(QualityGateStatus.WARNING)).isFalse();


        assertThat(QualityGateStatus.INACTIVE.isWorseThan(QualityGateStatus.INACTIVE)).isFalse();
        assertThat(QualityGateStatus.INACTIVE.isWorseThan(QualityGateStatus.PASSED)).isFalse();
        assertThat(QualityGateStatus.INACTIVE.isWorseThan(QualityGateStatus.FAILED)).isFalse();
        assertThat(QualityGateStatus.INACTIVE.isWorseThan(QualityGateStatus.WARNING)).isFalse();
    }
}