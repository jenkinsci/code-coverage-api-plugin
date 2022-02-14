package io.jenkins.plugins.coverage.model.visualization.colorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageChangeTendency}.
 *
 * @author Florian Orendi
 */
class CoverageChangeTendencyTest {

    @Test
    void shouldGetCoverageTendency() {
        assertThat(CoverageChangeTendency.getCoverageTendencyOf(1.0)).isEqualTo(CoverageChangeTendency.INCREASED);
        assertThat(CoverageChangeTendency.getCoverageTendencyOf(0.0)).isEqualTo(CoverageChangeTendency.EQUALS);
        assertThat(CoverageChangeTendency.getCoverageTendencyOf(-1.0)).isEqualTo(CoverageChangeTendency.DECREASED);
        assertThat(CoverageChangeTendency.getCoverageTendencyOf(null)).isEqualTo(CoverageChangeTendency.NA);
    }
}
