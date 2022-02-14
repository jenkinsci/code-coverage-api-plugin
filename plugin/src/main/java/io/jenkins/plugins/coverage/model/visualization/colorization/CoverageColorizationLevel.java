package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

/**
 * Provides the display colors which can be used for representing different coverage levels and differences.
 *
 * @author Florian Orendi
 */
public enum CoverageColorizationLevel {

    // The following order from 'good' (high level) to 'not good' is required in order to find the matching color

    LVL_95_INCREASE10(95.0, 10.0, new Color(125, 210, 110), new Color(0, 0, 0)),
    LVL_90_INCREASE5(90.0, 5.0, new Color(150, 214, 167), new Color(0, 0, 0)),
    LVL_80_INCREASE2(80.0, 2.0, new Color(180, 228, 169), new Color(0, 0, 0)),
    LVL_70_EQUALS(70.0, 0.0, new Color(210, 241, 170), new Color(0, 0, 0)),
    LVL_60_DECREASE2(60.0, -2.0, new Color(251, 190, 160), new Color(0, 0, 0)),
    LVL_50_DECREASE5(50.0, -5.0, new Color(246, 160, 155), new Color(0, 0, 0)),
    LVL_0_DECREASE10(0.0, -10.0, new Color(239, 130, 140), new Color(0, 0, 0)),
    NA(-1.0, -100.0, ColorUtils.NA_FILL_COLOR, ColorUtils.NA_LINE_COLOR);

    /**
     * The coverage percentage.
     */
    private final double coveragePercentage;
    private final double coverageDifference;
    private final Color fillColor;
    private final Color lineColor;

    CoverageColorizationLevel(final double coveragePercentage, final double coverageDifference, final Color fillColor,
            final Color lineColor) {
        this.coveragePercentage = coveragePercentage;
        this.coverageDifference = coverageDifference;
        this.fillColor = fillColor;
        this.lineColor = lineColor;
    }

    /**
     * Gets the display colors for representing the passed coverage amount. If the value is placed between two levels,
     * the fill colors are blended.
     *
     * @param coveragePercentage
     *         The coverage percentage
     *
     * @return the {@link DisplayColors display colors}
     */
    public static DisplayColors getDisplayColorsOfCoveragePercentage(final Double coveragePercentage) {
        for (int i = 0; i < values().length - 1; i++) {
            CoverageColorizationLevel level = values()[i];
            if (coveragePercentage >= level.coveragePercentage) {
                if (i == 0) {
                    return new DisplayColors(LVL_95_INCREASE10.lineColor, LVL_95_INCREASE10.fillColor);
                }
                CoverageColorizationLevel upperLevel = values()[i - 1];
                double distanceLevel = coveragePercentage - level.coveragePercentage;
                double distanceUpper = upperLevel.coveragePercentage - coveragePercentage;
                return getDisplayColors(distanceUpper, distanceLevel, upperLevel, level);
            }
        }
        return new DisplayColors(NA.lineColor, NA.fillColor);
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
    public static DisplayColors getDisplayColorsOfCoverageDifference(final Double coverageDifference) {
        // TODO: use this to colorize differences between coverage metrics and thresholds e.g.
        for (int i = 0; i < values().length - 1; i++) {
            CoverageColorizationLevel level = values()[i];
            if (coverageDifference >= level.coverageDifference) {
                if (i == 0) {
                    return new DisplayColors(LVL_95_INCREASE10.lineColor, LVL_95_INCREASE10.fillColor);
                }
                CoverageColorizationLevel upperLevel = values()[i - 1];
                double distanceLevel = coverageDifference - level.coverageDifference;
                double distanceUpper = upperLevel.coverageDifference - coverageDifference;
                return getDisplayColors(distanceUpper, distanceLevel, upperLevel, level);
            }
        }
        return new DisplayColors(NA.lineColor, NA.fillColor);
    }

    /**
     * Gets the {@link DisplayColors display colors} in dependence of the distances to the nearest two levels.
     *
     * @param distanceUpper
     *         The distance to the upper level
     * @param distanceLevel
     *         The distance to the found level
     * @param upperLevel
     *         The upper level
     * @param level
     *         The found level
     *
     * @return the matching display colors
     */
    private static DisplayColors getDisplayColors(final double distanceUpper, final double distanceLevel,
            final CoverageColorizationLevel upperLevel, final CoverageColorizationLevel level) {
        Color lineColor;
        if (distanceUpper > distanceLevel) {
            lineColor = upperLevel.lineColor;
        }
        else {
            lineColor = level.lineColor;
        }
        Color fillColor = ColorUtils.blendWeightedColors(
                level.fillColor, upperLevel.fillColor, distanceUpper, distanceLevel);
        return new DisplayColors(lineColor, fillColor);
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public double getCoverageDifference() {
        return coverageDifference;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getLineColor() {
        return lineColor;
    }

    /**
     * Wraps the fill color and the line color that should be used in order to visualize coverage values.
     *
     * @author Florian Orendi
     */
    public static class DisplayColors {

        private final Color lineColor;
        private final Color fillColor;

        /**
         * Creates a wrapper for the colors used for displaying values.
         *
         * @param lineColor
         *         The used line color
         * @param fillColor
         *         The used fill color
         */
        public DisplayColors(final Color lineColor, final Color fillColor) {
            this.lineColor = lineColor;
            this.fillColor = fillColor;
        }

        public Color getLineColor() {
            return lineColor;
        }

        public Color getFillColor() {
            return fillColor;
        }
    }
}
