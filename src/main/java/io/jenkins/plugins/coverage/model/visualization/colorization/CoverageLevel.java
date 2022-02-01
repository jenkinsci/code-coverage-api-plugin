package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

/**
 * Provides the colors which should be used for representing difference coverage percentage levels.
 *
 * @author Florian Orendi
 */
public enum CoverageLevel {

    // The order from high coverage to low coverage is required in order to find the matching coverage level

    OVER_95(95.0, new Color(125, 210, 110), new Color(0, 0, 0)),
    OVER_90(90.0, new Color(150, 214, 167), new Color(0, 0, 0)),
    OVER_80(80.0, new Color(180, 228, 169), new Color(0, 0, 0)),
    OVER_70(70.0, new Color(210, 241, 170), new Color(0, 0, 0)),
    OVER_60(60.0, new Color(251, 190, 160), new Color(0, 0, 0)),
    OVER_50(50.0, new Color(246, 160, 155), new Color(0, 0, 0)),
    OVER_0(0.0, new Color(239, 130, 140), new Color(0, 0, 0)),
    NA(-1.0, ColorUtils.NA_FILL_COLOR, ColorUtils.NA_LINE_COLOR);

    /**
     * The coverage percentage.
     */
    private final double coverage;
    private final Color fillColor;
    private final Color lineColor;

    CoverageLevel(final double coverage, final Color fillColor,
            final Color lineColor) {
        this.coverage = coverage;
        this.fillColor = fillColor;
        this.lineColor = lineColor;
    }

    /**
     * Provides the {@link CoverageLevel} which matches with the passed coverage percentage.
     *
     * @param coverage
     *         The code coverage percentage
     *
     * @return the matching coverage level
     */
    public static CoverageLevel getCoverageValueOf(final Double coverage) {
        for (final CoverageLevel level : values()) {
            if (coverage >= level.coverage) {
                return level;
            }
        }
        return NA;
    }

    /**
     * Gets the fill color for representing the passed coverage percentage. If the percentage is placed between two
     * levels, the colors are blended.
     *
     * @param coverageLevel
     *         The coverage percentage
     *
     * @return the blended color
     */
    public static Color getBlendedFillColorOf(final Double coverageLevel) {
        if (coverageLevel >= 0) {
            for (int i = 0; i < values().length - 1; i++) {
                final CoverageLevel level = values()[i];
                if (coverageLevel == level.coverage) {
                    return level.fillColor;
                }
                else if (coverageLevel >= level.coverage) {
                    if (i == 0) {
                        return OVER_95.fillColor;
                    }
                    final CoverageLevel upperLevel = values()[i - 1];
                    final double distanceUpper = coverageLevel - level.coverage;
                    final double distanceLower = upperLevel.coverage - coverageLevel;
                    return ColorUtils.blendWeightedColors(level.fillColor, upperLevel.fillColor,
                            distanceLower, distanceUpper);
                }
            }
        }
        return NA.fillColor;
    }

    public double getCoverage() {
        return coverage;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getLineColor() {
        return lineColor;
    }
}
