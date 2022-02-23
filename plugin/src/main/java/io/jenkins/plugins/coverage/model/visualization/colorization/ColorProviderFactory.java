package io.jenkins.plugins.coverage.model.visualization.colorization;

public class ColorProviderFactory {

    public static ColorProvider createDefaultColorProvider() {
        return new ColorProvider();
    }

    public static ColorProvider createColorProvider() {
        // TODO: create color Provider dependent on view
        return createDefaultColorProvider();
    }
}
