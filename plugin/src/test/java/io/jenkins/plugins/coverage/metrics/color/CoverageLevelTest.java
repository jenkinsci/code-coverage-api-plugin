package io.jenkins.plugins.coverage.metrics.color;

import java.awt.*;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider.DisplayColors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageLevel}.
 *
 * @author Florian Orendi
 */
class CoverageLevelTest {

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();

    @Test
    void shouldHaveWorkingGetters() {
        CoverageLevel coverageLevel = CoverageLevel.LVL_0;
        assertThat(coverageLevel.getLevel()).isEqualTo(0.0);
        assertThat(coverageLevel.getColorizationId()).isEqualTo(ColorId.INSUFFICIENT);
    }

    @Test
    void shouldGetDisplayColorsOfCoveragePercentage() {
        Color blendedColor = ColorProvider.blendColors(
                COLOR_PROVIDER.getDisplayColorsOf(CoverageLevel.LVL_60.getColorizationId()).getFillColor(),
                COLOR_PROVIDER.getDisplayColorsOf(CoverageLevel.LVL_70.getColorizationId()).getFillColor());

        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(65.0, COLOR_PROVIDER))
                .isEqualTo(new DisplayColors(COLOR_PROVIDER.getDisplayColorsOf(ColorId.BLACK).getFillColor(),
                        blendedColor));
        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(96.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.EXCELLENT));
        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(50.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.VERY_BAD));
        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(-2.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.WHITE));
    }
}
