package io.jenkins.plugins.coverage.metrics.steps;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Metric;

import nl.jqno.equalsverifier.EqualsVerifier;

import static io.jenkins.plugins.coverage.metrics.steps.CoveragePercentage.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoveragePercentage}.
 *
 * @author Florian Orendi
 */
class CoveragePercentageTest {
    private static final double COVERAGE_FRACTION = 0.5;
    private static final double COVERAGE_PERCENTAGE = 50.0;
    private static final Locale LOCALE = Locale.GERMAN;

    @Test
    void shouldHandleOverflow() {
        Fraction fraction = Fraction.getFraction(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(fraction);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(100);
    }

    @Test
    void shouldCreateCoveragePercentageFromFraction() {
        Fraction fraction = Fraction.getFraction(COVERAGE_FRACTION);
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(fraction);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldCreateCoveragePercentageFromDouble() {
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldCreateCoveragePercentageFromNumeratorAndDenominator() {
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(50, 1);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldNotCreateCoveragePercentageFromNumeratorAndZeroDenominator() {
        assertThatThrownBy(() -> CoveragePercentage.valueOf(50, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DENOMINATOR_ZERO_MESSAGE);
    }

    @Test
    void shouldHaveWorkingGetters() {
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.getNumerator()).isEqualTo(50);
        assertThat(coveragePercentage.getDenominator()).isEqualTo(1);
    }

    @Test
    void shouldGetDoubleValue() {
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldFormatPercentage() {
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.formatPercentage(LOCALE)).isEqualTo("50,00%");
    }

    @Test
    void shouldFormatDeltaPercentage() {
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.formatDeltaPercentage(LOCALE)).isEqualTo("+50,00%");
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(CoveragePercentage.class).verify();
    }

    @Test
    void shouldSerializeInstance() {
        CoveragePercentage percentage = CoveragePercentage.valueOf(49, 1);
        assertThat(percentage.serializeToString())
                .isEqualTo("49/1");
        assertThat(valueOf("49/1")).isEqualTo(percentage)
                .hasToString("49.00%");

        assertThatIllegalArgumentException().isThrownBy(
                () -> Coverage.valueOf(Metric.LINE, "1/0"));
    }
}
