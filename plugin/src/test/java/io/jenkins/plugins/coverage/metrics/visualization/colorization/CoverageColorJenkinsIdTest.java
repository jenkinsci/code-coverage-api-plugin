package io.jenkins.plugins.coverage.metrics.visualization.colorization;

import org.junit.jupiter.api.Test;

import static io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageColorJenkinsId.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageColorJenkinsId}.
 *
 * @author Florian Orendi
 */
class CoverageColorJenkinsIdTest {

    @Test
    void shouldGetAllIds() {
        assertThat(getAll().size()).isEqualTo(values().length);
    }

    @Test
    void shouldGetColorId() {
        assertThat(GREEN.getJenkinsColorId()).isEqualTo("--green");
    }
}
