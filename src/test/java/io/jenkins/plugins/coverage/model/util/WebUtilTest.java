package io.jenkins.plugins.coverage.model.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link WebUtil}.
 *
 * @author Florian Orendi
 */
class WebUtilTest {

    @Test
    void shouldProvideRelativeCoverageDefaultUrl() {
        assertThat(WebUtil.getRelativeCoverageDefaultUrl()).isEqualTo(WebUtil.COVERAGE_DEFAULT_URL);
    }
}
