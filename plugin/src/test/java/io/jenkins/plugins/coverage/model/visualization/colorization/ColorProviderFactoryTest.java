package io.jenkins.plugins.coverage.model.visualization.colorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ColorProviderFactory}.
 *
 * @author Florian Orendi
 */
class ColorProviderFactoryTest {

    @Test
    void shouldCreateDefaultColorProvider() {
        ColorProvider colorProvider = ColorProviderFactory.createDefaultColorProvider();
        for (CoverageColorPalette color : CoverageColorPalette.values()) {
            assertThat(colorProvider.containsColorId(color.getColorId())).isTrue();
        }
    }
}
