package io.jenkins.plugins.coverage.model.visualization.colorization;

import java.awt.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ColorProvider}.
 *
 * @author Florian Orendi
 */
class ColorProviderTest {

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
    }

    @Test
    void shouldProvideColorAsHex() {
        assertThat(ColorProvider.colorAsHex(Color.black)).isEqualTo("#000000FF");
    }
}
