package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageColorizationLevel.DisplayColors;

import static io.jenkins.plugins.util.assertions.Assertions.*;

/**
 * Test class for {@link CoverageColorizationLevel}.
 *
 * @author Florian Orendi
 */
class CoverageColorizationLevelTest {

    @Test
    void shouldHaveWorkingGettersForDisplayColors() {
        Color fillColor = Color.white;
        Color lineColor = Color.black;
        DisplayColors displayColors = new DisplayColors(lineColor, fillColor);
        assertThat(displayColors.getFillColor()).isEqualTo(fillColor);
        assertThat(displayColors.getLineColor()).isEqualTo(lineColor);
    }

    @Test
    void shouldGetDisplayColorsOfCoveragePercentage() {
        Color blendedColor = ColorUtils.blendColors(
                CoverageColorizationLevel.LVL_60_DECREASE2.getFillColor(),
                CoverageColorizationLevel.LVL_70_EQUALS.getFillColor());

        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoveragePercentage(65.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.LVL_60_DECREASE2.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(blendedColor);
        });
        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoveragePercentage(96.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.LVL_95_INCREASE10.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(CoverageColorizationLevel.LVL_95_INCREASE10.getFillColor());
        });
        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoveragePercentage(50.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.LVL_50_DECREASE5.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(CoverageColorizationLevel.LVL_50_DECREASE5.getFillColor());
        });
        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoveragePercentage(-2.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.NA.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(CoverageColorizationLevel.NA.getFillColor());
        });
    }

    @Test
    void shouldGetDisplayColorsOfCoverageDifference() {
        Color blendedColorDecreased = ColorUtils.blendColors(
                CoverageColorizationLevel.LVL_70_EQUALS.getFillColor(),
                CoverageColorizationLevel.LVL_60_DECREASE2.getFillColor());
        Color blendedColorIncreased = ColorUtils.blendColors(
                CoverageColorizationLevel.LVL_80_INCREASE2.getFillColor(),
                CoverageColorizationLevel.LVL_70_EQUALS.getFillColor());

        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoverageDifference(-1.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.LVL_60_DECREASE2.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(blendedColorDecreased);
        });
        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoverageDifference(1.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.LVL_70_EQUALS.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(blendedColorIncreased);
        });
        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoverageDifference(15.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.LVL_95_INCREASE10.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(CoverageColorizationLevel.LVL_95_INCREASE10.getFillColor());
        });
        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoverageDifference(-2.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.LVL_60_DECREASE2.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(CoverageColorizationLevel.LVL_60_DECREASE2.getFillColor());
        });
        assertThat(CoverageColorizationLevel.getDisplayColorsOfCoverageDifference(-105.0)).satisfies(level -> {
            assertThat(level.getLineColor()).isEqualTo(CoverageColorizationLevel.NA.getLineColor());
            assertThat(level.getFillColor()).isEqualTo(CoverageColorizationLevel.NA.getFillColor());
        });
    }
}
