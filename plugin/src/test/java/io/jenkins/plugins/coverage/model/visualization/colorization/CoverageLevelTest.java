package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageLevel}.
 *
 * @author Florian Orendi
 */
class CoverageLevelTest {

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createColorProvider();

    @Test
    void shouldHaveWorkingGetters() {
        CoverageLevel coverageLevel = CoverageLevel.LVL_0;
        assertThat(coverageLevel.getLevel()).isEqualTo(0.0);
        assertThat(coverageLevel.getColorizationId()).isEqualTo(ColorId.DARK_RED);
    }

    @Test
    void shouldGetDisplayColorsOfCoveragePercentage() {
        Color blendedColor = ColorProvider.blendColors(
                COLOR_PROVIDER.getDisplayColorsOf(CoverageLevel.LVL_65.getColorizationId()).getFillColor(),
                COLOR_PROVIDER.getDisplayColorsOf(CoverageLevel.LVL_70.getColorizationId()).getFillColor());

        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(67.5, COLOR_PROVIDER))
                .isEqualTo(new DisplayColors(COLOR_PROVIDER.getDisplayColorsOf(ColorId.BLACK).getFillColor(),
                        blendedColor));
        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(96.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.DARK_GREEN));
        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(50.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.LIGHT_RED));
        assertThat(CoverageLevel.getDisplayColorsOfCoverageLevel(-2.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.WHITE));
    }
}
