package io.jenkins.plugins.coverage.model.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link WebUtils}.
 *
 * @author Florian Orendi
 */
class WebUtilsTest {

    @Test
    void shouldProvideRelativeCoverageDefaultUrl() {
        assertThat(WebUtils.getRelativeCoverageDefaultUrl()).isEqualTo(WebUtils.COVERAGE_DEFAULT_URL);
    }
}
