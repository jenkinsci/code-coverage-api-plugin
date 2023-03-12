package io.jenkins.plugins.coverage.metrics.color;

import java.awt.*;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static io.jenkins.plugins.coverage.metrics.color.ColorProvider.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ColorProvider}.
 *
 * @author Florian Orendi
 */
class ColorProviderTest {

    @Test
    void shouldGetDisplayColorsOfId() {
        ColorProvider colorProvider = createDefaultColorProvider();
        DisplayColors displayColors = colorProvider.getDisplayColorsOf(ColorId.EXCELLENT);

        assertThat(displayColors.getFillColor()).isEqualTo(CoverageColorPalette.GREEN.getFillColor());
        assertThat(displayColors.getLineColor()).isEqualTo(CoverageColorPalette.GREEN.getLineColor());
        assertThat(colorProvider.getDisplayColorsOf(null)).isEqualTo(DEFAULT_COLOR);
    }

    @Test
    void shouldCheckForExistentColorId() {
        ColorProvider colorProvider = createDefaultColorProvider();
        assertThat(colorProvider.containsColorId(ColorId.EXCELLENT)).isTrue();
        assertThat(colorProvider.containsColorId(null)).isFalse();
    }

    @Test
    void shouldGetBlendedDisplayColors() {
        ColorProvider colorProvider = createDefaultColorProvider();

        assertThat(colorProvider.getBlendedDisplayColors(1, 1, null, ColorId.EXCELLENT))
                .isEqualTo(DEFAULT_COLOR);
        assertThat(colorProvider.getBlendedDisplayColors(1, 1, ColorId.EXCELLENT, null))
                .isEqualTo(DEFAULT_COLOR);
        assertThat(colorProvider.getBlendedDisplayColors(1, 1, null, null))
                .isEqualTo(DEFAULT_COLOR);
        assertThat(colorProvider.getBlendedDisplayColors(2, 1, ColorId.BLACK, ColorId.WHITE))
                .isEqualTo(new DisplayColors(new Color(0xFFFFFF), new Color(0x555555)));
        assertThat(colorProvider.getBlendedDisplayColors(1, 2, ColorId.BLACK, ColorId.WHITE))
                .isEqualTo(new DisplayColors(new Color(0x000000), new Color(0xAAAAAA)));
    }

    @Test
    void shouldBlendColors() {
        assertThat(blendColors(Color.yellow, Color.blue)).isEqualTo(new Color(127, 127, 127));
    }

    @Test
    void shouldBlendWeightedColors() {
        Color first = new Color(200, 200, 200);
        Color second = new Color(0, 0, 0);
        double firstWeight = 1;
        double secondWeight = 3;

        assertThat(blendWeightedColors(first, second, firstWeight, secondWeight))
                .isEqualTo(new Color(50, 50, 50));
        assertThatThrownBy(() -> blendWeightedColors(first, second, -1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLEND_COLOR_ERROR_MESSAGE);
        assertThatThrownBy(() -> blendWeightedColors(first, second, 1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLEND_COLOR_ERROR_MESSAGE);
    }

    @Test
    void shouldProvideColorAsHex() {
        assertThat(colorAsRGBHex(Color.black)).isEqualTo("#000000");
        assertThat(colorAsRGBAHex(Color.black, 255)).isEqualTo("#000000FF");
    }

    @Test
    void shouldProvideColorAsHexForDisplayColors() {
        DisplayColors displayColors = new DisplayColors(Color.black, Color.white);

        assertThat(displayColors.getFillColorAsRGBAHex(255)).isEqualTo("#FFFFFFFF");
        assertThat(displayColors.getLineColorAsRGBHex()).isEqualTo("#000000");
        assertThat(displayColors.getFillColorAsRGBHex()).isEqualTo("#FFFFFF");
    }

    @Test
    void shouldObeyEqualsContractForDisplayColors() {
        EqualsVerifier.forClass(DisplayColors.class)
                .usingGetClass()
                .verify();
    }

    private ColorProvider createDefaultColorProvider() {
        return ColorProviderFactory.createDefaultColorProvider();
    }
}
