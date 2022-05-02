package io.jenkins.plugins.coverage.model;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

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
    void shouldCreateCoveragePercentageFromFraction() {
        Fraction fraction = Fraction.getFraction(COVERAGE_FRACTION);
        CoveragePercentage coveragePercentage = CoveragePercentage.getCoveragePercentage(fraction);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldCreateCoveragePercentageFromDouble() {
        CoveragePercentage coveragePercentage = CoveragePercentage.getCoveragePercentage(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldHaveWorkingGetters() {
        CoveragePercentage coveragePercentage = CoveragePercentage.getCoveragePercentage(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.getNumerator()).isEqualTo(50);
        assertThat(coveragePercentage.getDenominator()).isEqualTo(1);
    }

    @Test
    void shouldGetDoubleValue() {
        CoveragePercentage coveragePercentage = CoveragePercentage.getCoveragePercentage(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldFormatPercentage() {
        CoveragePercentage coveragePercentage = CoveragePercentage.getCoveragePercentage(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.formatPercentage(LOCALE)).isEqualTo("50,00%");
    }

    @Test
    void shouldFormatDeltaPercentage() {
        CoveragePercentage coveragePercentage = CoveragePercentage.getCoveragePercentage(COVERAGE_PERCENTAGE);
        assertThat(coveragePercentage.formatDeltaPercentage(LOCALE)).isEqualTo("+50,00%");
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(CoveragePercentage.class).verify();
    }
}
