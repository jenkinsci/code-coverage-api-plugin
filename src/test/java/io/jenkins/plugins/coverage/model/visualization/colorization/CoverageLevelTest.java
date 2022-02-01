package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageLevel}.
 *
 * @author Florian Orendi
 */
class CoverageLevelTest {

    @Test
    void shouldGetCoverageLevel() {
        assertThat(CoverageLevel.getCoverageValueOf(50.0)).isEqualTo(CoverageLevel.OVER_50);
        assertThat(CoverageLevel.getCoverageValueOf(49.0)).isEqualTo(CoverageLevel.OVER_0);
        assertThat(CoverageLevel.getCoverageValueOf(-100.0)).isEqualTo(CoverageLevel.NA);
    }

    @Test
    void shouldGetBlendedFillColor() {
        Color blendedColor = ColorUtils.blendColors(CoverageLevel.OVER_60.getFillColor(),
                CoverageLevel.OVER_70.getFillColor());

        assertThat(CoverageLevel.getBlendedFillColorOf(65.0)).isEqualTo(blendedColor);
        assertThat(CoverageLevel.getBlendedFillColorOf(96.0)).isEqualTo(CoverageLevel.OVER_95.getFillColor());
        assertThat(CoverageLevel.getBlendedFillColorOf(50.0)).isEqualTo(CoverageLevel.OVER_50.getFillColor());
        assertThat(CoverageLevel.getBlendedFillColorOf(-100.0)).isEqualTo(CoverageLevel.NA.getFillColor());
    }
}
