package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

public enum CoverageColorPalette {

    UNDEFINED(ColorId.WHITE, new Color(255, 255, 255), new Color(0, 0, 0)),

    DARK_RED(ColorId.DARK_RED, new Color(200, 0, 0, 80), new Color(0, 0, 0)),
    RED(ColorId.RED, new Color(255, 0, 0, 80), new Color(0, 0, 0)),
    LIGHT_RED(ColorId.LIGHT_RED, new Color(255, 50, 50, 80), new Color(0, 0, 0)),

    DARK_ORANGE(ColorId.DARK_ORANGE, new Color(255, 120, 50, 80), new Color(0, 0, 0)),
    ORANGE(ColorId.ORANGE, new Color(255, 170, 50, 80), new Color(0, 0, 0)),
    LIGHT_ORANGE(ColorId.LIGHT_ORANGE, new Color(255, 200, 50, 80), new Color(0, 0, 0)),

    DARK_YELLOW(ColorId.DARK_YELLOW, new Color(255, 220, 50, 80), new Color(0, 0, 0)),
    YELLOW(ColorId.YELLOW, new Color(255, 240, 100, 80), new Color(0, 0, 0)),
    LIGHT_YELLOW(ColorId.LIGHT_YELLOW, new Color(255, 255, 180, 80), new Color(0, 0, 0)),

    LIGHT_GREEN(ColorId.LIGHT_GREEN, new Color(150, 255, 100, 80), new Color(0, 0, 0)),
    GREEN(ColorId.GREEN, new Color(0, 200, 0, 80), new Color(0, 0, 0)),
    DARK_GREEN(ColorId.DARK_GREEN, new Color(0, 130, 0, 80), new Color(0, 0, 0));

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
