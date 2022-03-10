package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

/**
 * Provides a color palette which can be used as an plugin internal fallback if no other color schemes have been defined.
 *
 * @author Florian Orendi
 */
public enum CoverageColorPalette {

    WHITE(ColorId.WHITE, new Color(255, 255, 255), new Color(0, 0, 0)),
    BLACK(ColorId.BLACK, new Color(0, 0, 0), new Color(255, 255, 255)),

    DARK_RED(ColorId.INSUFFICIENT, new Color(200, 0, 0, 80), new Color(0, 0, 0)),
    LIGHT_RED(ColorId.VERY_BAD, new Color(255, 50, 50, 80), new Color(0, 0, 0)),

    DARK_ORANGE(ColorId.BAD, new Color(255, 120, 50, 80), new Color(0, 0, 0)),
    ORANGE(ColorId.INADEQUATE, new Color(255, 170, 50, 80), new Color(0, 0, 0)),
    LIGHT_ORANGE(ColorId.BELOW_AVERAGE, new Color(255, 200, 50, 80), new Color(0, 0, 0)),

    DARK_YELLOW(ColorId.AVERAGE, new Color(240, 240, 120, 80), new Color(0, 0, 0)),
    YELLOW(ColorId.ABOVE_AVERAGE, new Color(220, 250, 110, 80), new Color(0, 0, 0)),
    LIGHT_YELLOW(ColorId.GOOD, new Color(200, 255, 100, 80), new Color(0, 0, 0)),

    LIGHT_GREEN(ColorId.VERY_GOOD, new Color(150, 255, 100, 80), new Color(0, 0, 0)),
    GREEN(ColorId.EXCELLENT, new Color(0, 200, 0, 80), new Color(0, 0, 0)),
    DARK_GREEN(ColorId.OUTSTANDING, new Color(0, 130, 0, 80), new Color(0, 0, 0));

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
