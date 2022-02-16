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

    @Test
    void shouldTransformFractionToPercentage() {
        Fraction fraction = Fraction.ONE_HALF;
        Fraction percentage = Fraction.getFraction(50, 1);

        assertThat(FractionFormatter.transformFractionToPercentage(fraction)).isEqualTo(percentage);
    }

    @Test
    void shouldFormatPercentage() {
        Fraction percentage = Fraction.ONE_HALF;
        Locale locale = Functions.getCurrentLocale();
        String formattedPercentage = String.format(locale, "%.2f%%", percentage.doubleValue());

        assertThat(FractionFormatter.formatPercentage(percentage, locale)).isEqualTo(formattedPercentage);
    }

    @Test
    void shouldFormatDeltaFraction() {
        Fraction fraction = Fraction.ONE_HALF;
        Locale locale = Functions.getCurrentLocale();
        String formattedDelta = String.format(locale, "%+.2f%%",
                fraction.multiplyBy(Fraction.getFraction(100)).doubleValue());

        assertThat(FractionFormatter.formatDeltaFraction(fraction, locale)).isEqualTo(formattedDelta);
    }

    @Test
    void shouldFormatDeltaPercentage() {
        Fraction fraction = Fraction.ONE_HALF;
        Locale locale = Functions.getCurrentLocale();
        String formattedDelta = String.format(locale, "%+.2f%%", fraction.doubleValue());

        assertThat(FractionFormatter.formatDeltaPercentage(fraction, locale)).isEqualTo(formattedDelta);
    }
}
