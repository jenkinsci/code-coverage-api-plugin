package io.jenkins.plugins.coverage.metrics.color;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageChangeTendency}.
 *
 * @author Florian Orendi
 */
class CoverageChangeTendencyTest {

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();

    @Test
    void shouldGetDisplayColorsForTendency() {
        assertThat(CoverageChangeTendency.getDisplayColorsForTendency(1.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.EXCELLENT));
        assertThat(CoverageChangeTendency.getDisplayColorsForTendency(0.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.AVERAGE));
        assertThat(CoverageChangeTendency.getDisplayColorsForTendency(-1.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.INSUFFICIENT));
        assertThat(CoverageChangeTendency.getDisplayColorsForTendency(null, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.WHITE));
    }

    @Test
    void shouldGetColorizationId() {
        assertThat(CoverageChangeTendency.INCREASED.getColorizationId()).isEqualTo(ColorId.EXCELLENT);
    }
}
