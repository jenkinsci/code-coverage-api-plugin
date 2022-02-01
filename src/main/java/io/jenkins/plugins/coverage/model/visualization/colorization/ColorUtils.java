package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

/**
 * Provides utility for working with colors.
 *
 * @author Florian Orendi
 */
public class ColorUtils {

    /**
     * Default fill color when a content is not available.
     */
    public static final Color NA_FILL_COLOR = new Color(200, 200, 200);
    /**
     * Default line color when a content is not available.
     */
    public static final Color NA_LINE_COLOR = new Color(0, 0, 0);

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
     * Blends two colors using weights.
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
    public static Color blendWeightedColors(final Color color1, final Color color2,
            final double weight1, final double weight2) {
        if (color1 == null) {
            return color2 == null ? NA_FILL_COLOR : color2;
        }
        else if (color2 == null) {
            return color1;
        }
        final double total = weight1 + weight2;
        final int r = (int) ((color1.getRed() * weight1 + color2.getRed() * weight2) / total);
        final int g = (int) ((color1.getGreen() * weight1 + color2.getGreen() * weight2) / total);
        final int b = (int) ((color1.getBlue() * weight1 + color2.getBlue() * weight2) / total);
        return new Color(r, g, b);
    }

    /**
     * Provides the hex string of the passed color.
     *
     * @param color
     *         The {@link Color}
     *
     * @return the color as a hex string
     */
    public static String colorAsHex(final Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
