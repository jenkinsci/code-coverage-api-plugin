package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageChangeLevel}.
 *
 * @author Florian Orendi
 */
class CoverageChangeLevelTest {

    @Test
    void shouldGetCoverageChangeLevel() {
        assertThat(CoverageChangeLevel.getCoverageChangeOf(2.0)).isEqualTo(CoverageChangeLevel.INCREASE_2);
        assertThat(CoverageChangeLevel.getCoverageChangeOf(1.0)).isEqualTo(CoverageChangeLevel.EQUAL);
        assertThat(CoverageChangeLevel.getCoverageChangeOf(-2.0)).isEqualTo(CoverageChangeLevel.DECREASE_2);
        assertThat(CoverageChangeLevel.getCoverageChangeOf(-105.0)).isEqualTo(CoverageChangeLevel.NA);
    }

    @Test
    void shouldGetBlendedFillColor() {
        Color blendedColor = ColorUtils.blendColors(CoverageChangeLevel.EQUAL.getFillColor(),
                CoverageChangeLevel.INCREASE_2.getFillColor());

        assertThat(CoverageChangeLevel.getBlendedFillColorOf(1.0)).isEqualTo(blendedColor);
        assertThat(CoverageChangeLevel.getBlendedFillColorOf(15.0)).isEqualTo(
                CoverageChangeLevel.INCREASE_10.getFillColor());
        assertThat(CoverageChangeLevel.getBlendedFillColorOf(-2.0)).isEqualTo(
                CoverageChangeLevel.DECREASE_2.getFillColor());
        assertThat(CoverageChangeLevel.getBlendedFillColorOf(-105.0)).isEqualTo(CoverageChangeLevel.NA.getFillColor());
    }
}
