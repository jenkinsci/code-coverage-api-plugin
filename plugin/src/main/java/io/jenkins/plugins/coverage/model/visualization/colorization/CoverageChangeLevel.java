package io.jenkins.plugins.coverage.model.visualization.colorization;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;

public enum CoverageChangeLevel {

    INCREASE_5(5.0, ColorId.DARK_GREEN),
    INCREASE_2(2.0, ColorId.GREEN),
    EQUALS(0.0, ColorId.LIGHT_YELLOW),
    DECREASE_2(-2.0, ColorId.LIGHT_ORANGE),
    DECREASE_5(-5.0, ColorId.DARK_ORANGE),
    DECREASE_10(-10.0, ColorId.LIGHT_RED),
    DECREASE_20(-20.0, ColorId.DARK_RED),
    NA(-100.0, ColorId.WHITE);

    private final double change;
    private final ColorId colorizationId;

    CoverageChangeLevel(final double change, final ColorId colorizationId) {
        this.change = change;
        this.colorizationId = colorizationId;
    }

    /**
     * Gets the display colors for representing the passed coverage difference. If the difference is placed between two
     * levels, the fill colors are blended.
     *
     * @param coverageDifference
     *         The coverage difference
     *
     * @return the {@link DisplayColors display colors}
     */
    public static DisplayColors getDisplayColorsOfCoverageChange(final Double coverageDifference,
            final ColorProvider colorProvider) {
        // TODO: use this to colorize differences between coverage metrics and thresholds e.g.
        for (int i = 0; i < values().length - 1; i++) {
            CoverageChangeLevel level = values()[i];
            if (coverageDifference >= level.change) {
                if (i == 0) {
                    return colorProvider.getDisplayColorsOf(INCREASE_5.colorizationId);
                }
                CoverageChangeLevel upperLevel = values()[i - 1];
                double distanceLevel = coverageDifference - level.change;
                double distanceUpper = upperLevel.change - coverageDifference;
                return colorProvider.getBlendedDisplayColors(
                        distanceLevel, distanceUpper,
                        upperLevel.colorizationId,
                        level.colorizationId);
            }
        }
        return colorProvider.getDisplayColorsOf(NA.colorizationId);
    }
}
