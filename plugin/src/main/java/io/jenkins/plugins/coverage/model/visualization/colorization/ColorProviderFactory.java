package io.jenkins.plugins.coverage.model.visualization.colorization;

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
     * Creates a {@link ColorProvider color provider} which uses the internal {@link CoverageColorPalette color
     * palette}.
     *
     * @return the created color provider
     */
    public static ColorProvider createDefaultColorProvider() {
        return new ColorProvider();
    }

    /**
     * Creates a {@link ColorProvider color provider} which uses a suitable {@link ColorScheme color scheme}.
     *
     * @return the created color provider
     */
    public static ColorProvider createColorProvider() {
        // TODO: create provider dependent on selected color scheme
        return new ColorProvider(ColorScheme.DEFAULT);
    }
}
