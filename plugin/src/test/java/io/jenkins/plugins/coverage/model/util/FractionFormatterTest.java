package io.jenkins.plugins.coverage.model.util;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import hudson.Functions;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link FractionFormatter}.
 *
 * @author Florian Orendi
 */
class FractionFormatterTest {

    private final Fraction COVERAGE = Fraction.ONE_HALF;
    private final Locale LOCALE = Locale.GERMAN;

    @Test
    void shouldTransformFractionToPercentage() {
        Fraction percentage = Fraction.getFraction(50, 1);
        assertThat(FractionFormatter.transformFractionToPercentage(COVERAGE)).isEqualTo(percentage);
    }

    @Test
    void shouldFormatPercentage() {
        assertThat(FractionFormatter.formatPercentage(COVERAGE, LOCALE)).isEqualTo("0,50%");
    }

    @Test
    void shouldFormatDeltaFraction() {
        assertThat(FractionFormatter.formatDeltaFraction(COVERAGE, LOCALE)).isEqualTo("+50,00%");
    }

    @Test
    void shouldFormatDeltaPercentage() {
        assertThat(FractionFormatter.formatDeltaPercentage(COVERAGE, LOCALE)).isEqualTo("+0,50%");
    }
}
