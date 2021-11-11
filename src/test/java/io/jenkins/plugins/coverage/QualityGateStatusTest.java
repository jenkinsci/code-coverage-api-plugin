package io.jenkins.plugins.coverage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link QualityGateStatus}.
 *
 * @author Michael Müller, Nikolas Paripovic
 */
class QualityGateStatusTest {

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

    @Test
    void shouldReturnWorseStatus() {

        assertThat(QualityGateStatus.FAILED.getWorseStatus(QualityGateStatus.INACTIVE)).isEqualTo(QualityGateStatus.FAILED);
        assertThat(QualityGateStatus.FAILED.getWorseStatus(QualityGateStatus.PASSED)).isEqualTo(QualityGateStatus.FAILED);
        assertThat(QualityGateStatus.FAILED.getWorseStatus(QualityGateStatus.WARNING)).isEqualTo(QualityGateStatus.FAILED);

        assertThat(QualityGateStatus.FAILED.getWorseStatus(QualityGateStatus.FAILED)).isEqualTo(QualityGateStatus.FAILED);

        assertThat(QualityGateStatus.WARNING.getWorseStatus(QualityGateStatus.INACTIVE)).isEqualTo(QualityGateStatus.WARNING);
        assertThat(QualityGateStatus.WARNING.getWorseStatus(QualityGateStatus.PASSED)).isEqualTo(QualityGateStatus.WARNING);

        assertThat(QualityGateStatus.WARNING.getWorseStatus(QualityGateStatus.FAILED)).isEqualTo(QualityGateStatus.FAILED);
        assertThat(QualityGateStatus.WARNING.getWorseStatus(QualityGateStatus.WARNING)).isEqualTo(QualityGateStatus.WARNING);

        assertThat(QualityGateStatus.PASSED.getWorseStatus(QualityGateStatus.INACTIVE)).isEqualTo(QualityGateStatus.PASSED);

        assertThat(QualityGateStatus.PASSED.getWorseStatus(QualityGateStatus.PASSED)).isEqualTo(QualityGateStatus.PASSED);
        assertThat(QualityGateStatus.PASSED.getWorseStatus(QualityGateStatus.FAILED)).isEqualTo(QualityGateStatus.FAILED);
        assertThat(QualityGateStatus.PASSED.getWorseStatus(QualityGateStatus.WARNING)).isEqualTo(QualityGateStatus.WARNING);

        assertThat(QualityGateStatus.INACTIVE.getWorseStatus(QualityGateStatus.INACTIVE)).isEqualTo(QualityGateStatus.INACTIVE);
        assertThat(QualityGateStatus.INACTIVE.getWorseStatus(QualityGateStatus.PASSED)).isEqualTo(QualityGateStatus.PASSED);
        assertThat(QualityGateStatus.INACTIVE.getWorseStatus(QualityGateStatus.FAILED)).isEqualTo(QualityGateStatus.FAILED);
        assertThat(QualityGateStatus.INACTIVE.getWorseStatus(QualityGateStatus.WARNING)).isEqualTo(QualityGateStatus.WARNING);

    }
}
