package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Loads a color palette and provides these colors and operations on them. The colors are provided as a tuple of fill
 * color and line color, mapped by the id of the fill color.
 *
 * @author Florian Orendi
 */
public class ColorProvider {

    /**
     * Default color that is provided if no color is found in order to guarantee a proper colorization.
     */
    public static final DisplayColors DEFAULT_COLOR = new DisplayColors(Color.black, Color.white);

    static final String BLEND_COLOR_ERROR_MESSAGE = "Color weights have to be greater or equal to zero";

    /**
     * The available {@link DisplayColors display colors} are mapped by the {@link ColorId id} of the fill color.
     */
    private final Map<ColorId, DisplayColors> availableColors;

    /**
     * Creates a color provider which uses the passed colors. Each color entry contains a background and a fitting text
     * color.
     *
     * @param colorMapping
     *         The color mapping to be used
     */
    ColorProvider(final Map<ColorId, DisplayColors> colorMapping) {
        availableColors = new HashMap<>(colorMapping);
    }

    /**
     * Blends two colors.
     *
     * @param color1
     *         The first color
     * @param color2
     *         The second color
     *
     * @return the blended color
     */
    public static Color blendColors(final Color color1, final Color color2) {
        return blendWeightedColors(color1, color2, 1, 1);
    }

    /**
     * Blends two colors using weights that have to be greater then zero.
     *
     * @param color1
     *         The first color
     * @param color2
     *         The second color
     * @param weight1
     *         The weight of the first color
     * @param weight2
     *         The weight of the second color
     *
     * @return the blended color
     */
    public static Color blendWeightedColors(@NonNull final Color color1, @NonNull final Color color2,
            final double weight1, final double weight2) {
        if (weight1 >= 0 && weight2 >= 0) {
            final double total = weight1 + weight2;
            final int r = (int) ((color1.getRed() * weight1 + color2.getRed() * weight2) / total);
            final int g = (int) ((color1.getGreen() * weight1 + color2.getGreen() * weight2) / total);
            final int b = (int) ((color1.getBlue() * weight1 + color2.getBlue() * weight2) / total);
            final int a = (int) ((color1.getAlpha() * weight1 + color2.getAlpha() * weight2) / total);
            return new Color(r, g, b, a);
        }
        throw new IllegalArgumentException(BLEND_COLOR_ERROR_MESSAGE);
    }

    /**
     * Provides the RGBA hex string of the passed color.
     *
     * @param color
     *         The {@link Color}
     * @param alpha
     *         The alpha value within the range [0;255]
     *
     * @return the color as a hex string
     */
    public static String colorAsRGBAHex(final Color color, final int alpha) {
        return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Provides the RGB hex string of the passed color.
     *
     * @param color
     *         The {@link Color}
     *
     * @return the color as a hex string
     */
    public static String colorAsRGBHex(final Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Returns the {@link DisplayColors display colors} for the passed id.
     *
     * @param colorId
     *         The ID of the fill color
     *
     * @return the display colors or the {@link #DEFAULT_COLOR default color} if no color has been found
     */
    public DisplayColors getDisplayColorsOf(final ColorId colorId) {
        if (containsColorId(colorId)) {
            return availableColors.get(colorId);
        }
        return DEFAULT_COLOR;
    }

    /**
     * Checks whether the provider contains {@link DisplayColors display colors} for the passed id.
     *
     * @param colorId
     *         The color id that should be checked
     *
     * @return {@code true} whether the id is available, else {@code false}
     */
    public boolean containsColorId(final ColorId colorId) {
        return availableColors.containsKey(colorId);
    }

    /**
     * Gets the blended {@link DisplayColors display colors} in dependence of the passed weights for each colors.
     *
     * @param weightFirst
     *         The weight of the first colors
     * @param weightSecond
     *         The weight of the second colors
     * @param first
     *         The first display colors
     * @param second
     *         The second display colors
     *
     * @return the blended display colors or the {@link #DEFAULT_COLOR} if one color has not been found
     */
    public DisplayColors getBlendedDisplayColors(final double weightFirst, final double weightSecond,
            final ColorId first, final ColorId second) {
        if (containsColorId(first) && containsColorId(second)) {
            DisplayColors firstColor = getDisplayColorsOf(first);
            DisplayColors secondColor = getDisplayColorsOf(second);
            Color lineColor;
            if (weightFirst > weightSecond) {
                lineColor = firstColor.lineColor;
            }
            else {
                lineColor = secondColor.lineColor;
            }
            Color fillColor = blendWeightedColors(firstColor.fillColor, secondColor.fillColor, weightFirst,
                    weightSecond);
            return new DisplayColors(lineColor, fillColor);
        }
        return DEFAULT_COLOR;
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

        /**
         * Gets the fill color with the passed alpha value. Using a low alpha value might require using another line
         * color than the provided {@link #lineColor}.
         *
         * @param alpha
         *         The color alpha
         *
         * @return the hex code which contains the alpha value
         */
        public String getFillColorAsRGBAHex(final int alpha) {
            return colorAsRGBAHex(fillColor, alpha);
        }

        public String getLineColorAsRGBHex() {
            return colorAsRGBHex(lineColor);
        }

        public String getFillColorAsRGBHex() {
            return colorAsRGBHex(fillColor);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DisplayColors that = (DisplayColors) o;
            return Objects.equals(lineColor, that.lineColor) && Objects.equals(fillColor, that.fillColor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lineColor, fillColor);
        }
    }
}
