package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ColorProvider}.
 *
 * @author Florian Orendi
 */
class ColorProviderTest {

    @Test
    void shouldLoadColors() {
        // TODO: this test case is trivial at the moment and needs to get expanded with further color schemes
        ColorProvider colorProvider = createDefaultColorProvider();
        colorProvider.loadColors(ColorScheme.DEFAULT);
        assertThat(colorProvider.containsColorId(ColorId.GREEN)).isTrue();
    }

    @Test
    void shouldGetDisplayColorsOfId() {
        ColorProvider colorProvider = createDefaultColorProvider();
        DisplayColors displayColors = colorProvider.getDisplayColorsOf(ColorId.GREEN);

        assertThat(displayColors.getFillColor()).isEqualTo(CoverageColorPalette.GREEN.getFillColor());
        assertThat(displayColors.getLineColor()).isEqualTo(CoverageColorPalette.GREEN.getLineColor());
        assertThat(colorProvider.getDisplayColorsOf(null)).isEqualTo(DEFAULT_COLOR);
    }

    @Test
    void shouldCheckForExistentColorId() {
        ColorProvider colorProvider = createDefaultColorProvider();
        assertThat(colorProvider.containsColorId(ColorId.GREEN)).isTrue();
        assertThat(colorProvider.containsColorId(null)).isFalse();
    }

    @Test
    void shouldGetBlendedDisplayColors() {
        ColorProvider colorProvider = createDefaultColorProvider();

        assertThat(colorProvider.getBlendedDisplayColors(1, 1, null, ColorId.GREEN))
                .isEqualTo(ColorProvider.DEFAULT_COLOR);
        assertThat(colorProvider.getBlendedDisplayColors(1, 1, ColorId.GREEN, null))
                .isEqualTo(ColorProvider.DEFAULT_COLOR);
        assertThat(colorProvider.getBlendedDisplayColors(1, 1, null, null))
                .isEqualTo(ColorProvider.DEFAULT_COLOR);
        assertThat(colorProvider.getBlendedDisplayColors(2, 1, ColorId.BLACK, ColorId.WHITE))
                .isEqualTo(new DisplayColors(new Color(0xFFFFFF), new Color(0x555555)));
        assertThat(colorProvider.getBlendedDisplayColors(1, 2, ColorId.BLACK, ColorId.WHITE))
                .isEqualTo(new DisplayColors(new Color(0x000000), new Color(0xAAAAAA)));
    }

    @Test
    void shouldBlendColors() {
        assertThat(ColorProvider.blendColors(Color.yellow, Color.blue)).isEqualTo(new Color(127, 127, 127));
    }

    @Test
    void shouldBlendWeightedColors() {
        Color first = new Color(200, 200, 200);
        Color second = new Color(0, 0, 0);
        double firstWeight = 1;
        double secondWeight = 3;

        assertThat(ColorProvider.blendWeightedColors(first, second, firstWeight, secondWeight))
                .isEqualTo(new Color(50, 50, 50));
        assertThatThrownBy(() -> ColorProvider.blendWeightedColors(first, second, -1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLEND_COLOR_ERROR_MESSAGE);
        assertThatThrownBy(() -> ColorProvider.blendWeightedColors(first, second, 1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLEND_COLOR_ERROR_MESSAGE);
    }

    @Test
    void shouldProvideColorAsHex() {
        assertThat(ColorProvider.colorAsHex(Color.black)).isEqualTo("#000000FF");
    }

    @Test
    void shouldProvideColorAsHexForDisplayColors() {
        DisplayColors displayColors = new DisplayColors(Color.black, Color.white);
        assertThat(displayColors.getLineColorAsHex()).isEqualTo("#000000FF");
        assertThat(displayColors.getFillColorAsHex()).isEqualTo("#FFFFFFFF");
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
