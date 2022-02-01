package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ColorUtils}.
 *
 * @author Florian Orendi
 */
class ColoUtilsTest {

    @Test
    void shouldBlendColors() {
        assertThat(ColorUtils.blendColors(Color.yellow, Color.blue)).isEqualTo(new Color(127, 127, 127));
    }

    @Test
    void shouldBlendWeightedColors() {
        Color first = new Color(200, 200, 200);
        Color second = new Color(0, 0, 0);
        double firstWeight = 1;
        double secondWeight = 3;

        assertThat(ColorUtils.blendWeightedColors(first, second, firstWeight, secondWeight))
                .isEqualTo(new Color(50, 50, 50));
        assertThat(ColorUtils.blendWeightedColors(first, null, firstWeight, secondWeight))
                .isEqualTo(first);
        assertThat(ColorUtils.blendWeightedColors(null, second, firstWeight, secondWeight))
                .isEqualTo(second);
        assertThat(ColorUtils.blendWeightedColors(null, null, firstWeight, secondWeight)).isEqualTo(
                ColorUtils.NA_FILL_COLOR);
    }

    @Test
    void shouldProvideColorAsHex() {
        assertThat(ColorUtils.colorAsHex(Color.black)).isEqualTo("#000000");
    }
}
