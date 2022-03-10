package io.jenkins.plugins.coverage.model.visualization.colorization;

import edu.umd.cs.findbugs.annotations.NonNull;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;

/**
 * Provides the colorization for different coverage change levels.
 *
 * @author Florian Orendi
 */
public enum CoverageChangeLevel {

    INCREASE_5(5.0, ColorId.OUTSTANDING),
    INCREASE_2(2.0, ColorId.EXCELLENT),
    EQUALS(0.0, ColorId.AVERAGE),
    DECREASE_2(-2.0, ColorId.INADEQUATE),
    DECREASE_5(-5.0, ColorId.BAD),
    DECREASE_10(-10.0, ColorId.VERY_BAD),
    DECREASE_20(-20.0, ColorId.INSUFFICIENT),
    NA(-100.0, ColorId.WHITE);

    private final double change;
    private final ColorId colorizationId;

    CoverageChangeLevel(final double change, final ColorId colorizationId) {
        this.change = change;
        this.colorizationId = colorizationId;
    }

    /**
     * Gets the {@link DisplayColors display colors} for representing the passed coverage change. If the change is
     * placed between two levels, the fill colors are blended.
     *
     * @param coverageDifference
     *         The coverage change
     * @param colorProvider
     *         The {@link ColorProvider color provider} to be used
     *
     * @return the display colors
     */
    public static DisplayColors getDisplayColorsOfCoverageChange(final double coverageDifference,
            @NonNull final ColorProvider colorProvider) {
        for (int i = 0; i < values().length - 1; i++) {
            CoverageChangeLevel level = values()[i];
            if (coverageDifference >= level.change) {
                if (i == 0) {
                    return colorProvider.getDisplayColorsOf(level.colorizationId);
                }
                double distanceLevel = coverageDifference - level.change;
                if (distanceLevel == 0) {
                    return colorProvider.getDisplayColorsOf(level.colorizationId);
                }
                CoverageChangeLevel upperLevel = values()[i - 1];
                double distanceUpper = upperLevel.change - coverageDifference;
                return colorProvider.getBlendedDisplayColors(
                        distanceLevel, distanceUpper,
                        upperLevel.colorizationId,
                        level.colorizationId);
            }
        }
        return colorProvider.getDisplayColorsOf(NA.colorizationId);
    }

    public double getChange() {
        return change;
    }

    public ColorId getColorizationId() {
        return colorizationId;
    }
}
