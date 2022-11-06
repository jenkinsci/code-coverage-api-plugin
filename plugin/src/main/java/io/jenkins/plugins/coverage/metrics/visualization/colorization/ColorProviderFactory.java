package io.jenkins.plugins.coverage.metrics.visualization.colorization;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;

/**
 * Provides factory methods for creating different {@link ColorProvider color providers}.
 *
 * @author Florian Orendi
 */
public class ColorProviderFactory {

    private ColorProviderFactory() {
        // prevents initialization
    }

    /**
     * Creates a {@link ColorProvider color provider} which uses the internal
     * {@link CoverageColorPalette color palette}.
     *
     * @return the created color provider
     */
    public static ColorProvider createDefaultColorProvider() {
        return new ColorProvider(getDefaultColors());
    }

    /**
     * Creates a {@link ColorProvider color provider} which uses the set Jenkins colors. Required color keys are:
     * '--green', '--light-green', '--yellow', '--light-yellow', '--orange', '--light-orange', '--red', '--light-red' -
     * see {@link CoverageColorJenkinsId}. If colors are missing, the internal default colors are used - see
     * {@link CoverageColorPalette}.
     *
     * @param colors
     *         Maps {@link CoverageColorJenkinsId jenkins color IDs}
     *
     * @return the created color provider
     */
    public static ColorProvider createColorProvider(final Map<String, String> colors) {
        if (!colors.keySet().equals(CoverageColorJenkinsId.getAll()) || !verifyHexCodes(colors.values())) {
            return createDefaultColorProvider();
        }
        Map<ColorId, DisplayColors> colorMap = new HashMap<>();
        // TODO: use dynamic text color (not provided yet)
        colorMap.put(ColorId.INSUFFICIENT,
                createDisplayColor(colors.get(CoverageColorJenkinsId.RED.getJenkinsColorId()), "#ffffff"));
        colorMap.put(ColorId.VERY_BAD,
                createDisplayColor(colors.get(CoverageColorJenkinsId.LIGHT_RED.getJenkinsColorId()), "#ffffff"));
        colorMap.put(ColorId.BAD,
                createDisplayColor(colors.get(CoverageColorJenkinsId.ORANGE.getJenkinsColorId()), "#000000"));
        colorMap.put(ColorId.INADEQUATE,
                createDisplayColor(colors.get(CoverageColorJenkinsId.LIGHT_ORANGE.getJenkinsColorId()), "#000000"));
        colorMap.put(ColorId.AVERAGE,
                createDisplayColor(colors.get(CoverageColorJenkinsId.YELLOW.getJenkinsColorId()), "#000000"));
        colorMap.put(ColorId.GOOD,
                createDisplayColor(colors.get(CoverageColorJenkinsId.LIGHT_YELLOW.getJenkinsColorId()), "#000000"));
        colorMap.put(ColorId.VERY_GOOD,
                createDisplayColor(colors.get(CoverageColorJenkinsId.LIGHT_GREEN.getJenkinsColorId()), "#000000"));
        colorMap.put(ColorId.EXCELLENT,
                createDisplayColor(colors.get(CoverageColorJenkinsId.GREEN.getJenkinsColorId()), "#ffffff"));
        colorMap.put(ColorId.BLACK, createDisplayColor(CoverageColorPalette.BLACK));
        colorMap.put(ColorId.WHITE, createDisplayColor(CoverageColorPalette.WHITE));
        return new ColorProvider(colorMap);
    }

    /**
     * Loads the internally usable {@link CoverageColorPalette color palette}. This can be also used as a fallback.
     *
     * @return the default color mapping
     */
    private static Map<ColorId, DisplayColors> getDefaultColors() {
        return Arrays.stream(CoverageColorPalette.values())
                .collect(Collectors.toMap(CoverageColorPalette::getColorId, ColorProviderFactory::createDisplayColor));
    }

    /**
     * Verifies that all passed strings are color hex codes.
     *
     * @param hexCodes
     *         The strings to be investigated
     *
     * @return {@code true} if all strings are hex codes
     */
    private static boolean verifyHexCodes(final Collection<String> hexCodes) {
        Pattern hexPattern = Pattern.compile("^#[A-Fa-f0-9]{6}$");
        for (String hex : hexCodes) {
            if (!hexPattern.matcher(hex).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a pair of {@link DisplayColors display colors} from the {@link CoverageColorPalette}.
     *
     * @param color
     *         The passed palette color
     *
     * @return the created display color
     */
    private static DisplayColors createDisplayColor(final CoverageColorPalette color) {
        return new DisplayColors(color.getLineColor(), color.getFillColor());
    }

    /**
     * Creates a pair of {@link DisplayColors display colors} from the passed hex colors.
     *
     * @param backgroundColorHex
     *         The hex code of the background color
     * @param textColorHex
     *         The hex code of the text color
     *
     * @return the created display color
     */
    private static DisplayColors createDisplayColor(final String backgroundColorHex, final String textColorHex) {
        Color backgroundColor = Color.decode(backgroundColorHex);
        Color textColor = Color.decode(textColorHex);
        return new DisplayColors(textColor, backgroundColor);
    }
}
