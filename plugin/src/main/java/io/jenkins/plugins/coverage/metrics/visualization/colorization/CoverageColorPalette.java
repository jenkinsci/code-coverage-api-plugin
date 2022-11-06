package io.jenkins.plugins.coverage.metrics.visualization.colorization;

import java.awt.*;

/**
 * Provides a color palette which can be used as a plugin internal fallback if no other color schemes have been defined.
 * The defined colors correspond to the <a href="https://weekly.ci.jenkins.io/design-library/Colors/">Jenkins Design
 * Library</a>.
 *
 * @author Florian Orendi
 */
public enum CoverageColorPalette {

    WHITE(ColorId.WHITE, new Color(255, 255, 255), new Color(0, 0, 0)),
    BLACK(ColorId.BLACK, new Color(0, 0, 0), new Color(255, 255, 255)),

    RED(ColorId.INSUFFICIENT, new Color(230, 0, 31), new Color(255, 255, 255)),
    LIGHT_RED(ColorId.VERY_BAD, new Color(255, 77, 101), new Color(255, 255, 255)),

    ORANGE(ColorId.BAD, new Color(254, 130, 10), new Color(0, 0, 0)),
    LIGHT_ORANGE(ColorId.INADEQUATE, new Color(254, 182, 112), new Color(0, 0, 0)),

    YELLOW(ColorId.AVERAGE, new Color(255, 204, 0), new Color(0, 0, 0)),
    LIGHT_YELLOW(ColorId.GOOD, new Color(255, 224, 102), new Color(0, 0, 0)),

    LIGHT_GREEN(ColorId.VERY_GOOD, new Color(75, 223, 124), new Color(0, 0, 0)),
    GREEN(ColorId.EXCELLENT, new Color(30, 166, 75), new Color(255, 255, 255));

    private final ColorId colorId;
    private final Color fillColor;
    private final Color lineColor;

    CoverageColorPalette(final ColorId colorId, final Color fillColor, final Color lineColor) {
        this.colorId = colorId;
        this.fillColor = fillColor;
        this.lineColor = lineColor;
    }

    public ColorId getColorId() {
        return colorId;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getLineColor() {
        return lineColor;
    }
}
