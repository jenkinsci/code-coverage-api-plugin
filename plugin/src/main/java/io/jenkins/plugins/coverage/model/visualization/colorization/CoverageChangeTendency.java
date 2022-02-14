package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

/**
 * Provides the colors which should be used for representing difference coverage change tendencies.
 *
 * @author Florian Orendi
 */
public enum CoverageChangeTendency {

    INCREASED(new Color(145, 212, 140), new Color(0, 0, 0)),
    EQUALS(new Color(204, 231, 165), new Color(0, 0, 0)),
    DECREASED(new Color(239, 130, 140), new Color(0, 0, 0)),
    NA(ColorUtils.NA_FILL_COLOR, ColorUtils.NA_LINE_COLOR);

    private final Color fillColor;
    private final Color lineColor;

    CoverageChangeTendency(final Color fillColor, final Color lineColor) {
        this.fillColor = fillColor;
        this.lineColor = lineColor;
    }

    /**
     * Provides the {@link CoverageChangeTendency} which matches with the passed coverage change.
     *
     * @param change
     *         The coverage change
     *
     * @return the matching change level
     */
    public static CoverageChangeTendency getCoverageTendencyOf(final Double change) {
        if (change == null || change.isNaN()) {
            return NA;
        }
        else if (change > 0) {
            return INCREASED;
        }
        else if (change < 0) {
            return DECREASED;
        }
        else {
            return EQUALS;
        }
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getLineColor() {
        return lineColor;
    }
}
