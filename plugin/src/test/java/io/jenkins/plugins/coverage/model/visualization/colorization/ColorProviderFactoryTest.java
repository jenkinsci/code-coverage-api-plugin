package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static io.jenkins.plugins.coverage.model.visualization.colorization.CoverageColorJenkinsId.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ColorProviderFactory}.
 *
 * @author Florian Orendi
 */
class ColorProviderFactoryTest {

    private static final String TEST_COLOR_HEX = "#ffffff";
    private static final Color TEST_COLOR = Color.decode(TEST_COLOR_HEX);

    @Test
    void shouldCreateDefaultColorProvider() {
        ColorProvider colorProvider = ColorProviderFactory.createDefaultColorProvider();
        for (CoverageColorPalette color : CoverageColorPalette.values()) {
            assertThat(colorProvider.containsColorId(color.getColorId())).isTrue();
        }
    }

    @Test
    void shouldCreateColorProviderWithJenkinsColors() {
        Map<String, String> colorMapping = createColorMapping();
        ColorProvider colorProvider = ColorProviderFactory.createColorProvider(colorMapping);

        for (CoverageColorPalette color : CoverageColorPalette.values()) {
            assertThat(colorProvider.containsColorId(color.getColorId())).isTrue();
            if (!color.getColorId().equals(ColorId.BLACK) && !color.getColorId()
                    .equals(ColorId.WHITE)) { // skip set default color
                assertThat(colorProvider.getDisplayColorsOf(color.getColorId()))
                        .satisfies(displayColor -> assertThat(displayColor.getFillColor()).isEqualTo(TEST_COLOR));
            }
        }
    }

    @Test
    void shouldCreateDefaultColorProviderWithMissingJenkinsColorIds() {
        Map<String, String> colorMapping = createColorMapping();
        colorMapping.remove("--green");
        ColorProvider colorProvider = ColorProviderFactory.createColorProvider(colorMapping);
        for (CoverageColorPalette color : CoverageColorPalette.values()) {
            assertThat(colorProvider.containsColorId(color.getColorId())).isTrue();
        }
    }

    /**
     * Creates a color mapping between the {@link CoverageColorJenkinsId jenkins color id} and the corresponding color
     * hex code.
     *
     * @return the created mapping
     */
    private Map<String, String> createColorMapping() {
        Map<String, String> colorMapping = new HashMap<>();
        getAll().forEach(id -> colorMapping.put(id, TEST_COLOR_HEX));
        return colorMapping;
    }
}
