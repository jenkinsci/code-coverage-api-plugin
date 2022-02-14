package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageType}.
 *
 * @author Florian Orendi
 */
class CoverageTypeTest {

    @Test
    void shouldGetCoverageTypeOfString() {
        assertThat(CoverageType.getCoverageTypeOf("Project Coverage")).isEqualTo(CoverageType.PROJECT);
        assertThat(CoverageType.getCoverageTypeOf("Project Coverage Delta")).isEqualTo(CoverageType.PROJECT_DELTA);
        assertThat(CoverageType.getCoverageTypeOf("Undefined")).isEqualTo(CoverageType.UNDEFINED);
    }

    @Test
    void shouldProvideAvailableCoverageTypes() {
        assertThat(CoverageType.getAvailableCoverageTypes()).containsExactlyInAnyOrder(
                CoverageType.PROJECT,
                CoverageType.PROJECT_DELTA
        );
    }
}
