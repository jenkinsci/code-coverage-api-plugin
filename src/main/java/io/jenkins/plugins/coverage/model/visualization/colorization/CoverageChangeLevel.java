package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

/**
 * Provides the colors which should be used for representing difference coverage change levels.
 *
 * @author Florian Orendi
 */
public enum CoverageChangeLevel {

    // The order from high increase to high decrease is required in order to find the matching change level

    INCREASE_10(10.0, new Color(124, 213, 117), new Color(0, 0, 0)),
    INCREASE_5(5.0, new Color(143, 224, 144), new Color(0, 0, 0)),
    INCREASE_2(2.0, new Color(183, 231, 165), new Color(0, 0, 0)),
    EQUAL(0.0, new Color(221, 231, 165), new Color(0, 0, 0)),
    DECREASE_2(-2.0, new Color(251, 190, 160), new Color(0, 0, 0)),
    DECREASE_5(-5.0, new Color(246, 160, 155), new Color(0, 0, 0)),
    DECREASE_10(-10.0, new Color(239, 130, 140), new Color(0, 0, 0)),
    NA(-101.0, ColorUtils.NA_FILL_COLOR, ColorUtils.NA_LINE_COLOR);

    /**
     * Percentage of how the coverage has changed.
     */
    private final double changeLevel;
    private final Color fillColor;
    private final Color lineColor;

    CoverageChangeLevel(final double changeLevel, final Color fillColor,
            final Color lineColor) {
        this.changeLevel = changeLevel;
        this.fillColor = fillColor;
        this.lineColor = lineColor;
    }

    /**
     * Provides the {@link CoverageChangeLevel} which matches with the passed coverage change.
     *
     * @param coverageChange
     *         The coverage change
     *
     * @return the matching change level
     */
    public static CoverageChangeLevel getCoverageChangeOf(final Double coverageChange) {
        for (final CoverageChangeLevel level : values()) {
            if (coverageChange >= level.changeLevel) {
                return level;
            }
        }
        return NA;
    }

    /**
     * Gets the fill color for representing the passed coverage change. If the change is placed between two levels, the
     * colors are blended.
     *
     * @param coverageChange
     *         The coverage change
     *
     * @return the blended color
     */
    public static Color getBlendedFillColorOf(final Double coverageChange) {
        for (int i = 0; i < values().length - 1; i++) {
            final CoverageChangeLevel level = values()[i];
            if (coverageChange >= level.changeLevel) {
                if (i == 0) {
                    return INCREASE_10.fillColor;
                }
                final CoverageChangeLevel upperLevel = values()[i - 1];
                final double distanceUpper = coverageChange - level.changeLevel;
                final double distanceLower = upperLevel.changeLevel - coverageChange;
                return ColorUtils.blendWeightedColors(level.fillColor, upperLevel.fillColor,
                        distanceLower, distanceUpper);
            }
        }
        return NA.fillColor;
    }

    public double getChangeLevel() {
        return changeLevel;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public Color getFillColor() {
        return fillColor;
    }
}
