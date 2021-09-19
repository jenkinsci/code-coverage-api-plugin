package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link Coverage}.
 *
 * @author Ullrich Hafner
 */
class CoverageTest {
    private static final double PRECISION = 0.01;

    @Test
    void shouldProvideNullObject() {
        assertThat(Coverage.NO_COVERAGE).isNotSet()
                .hasCovered(0)
                .hasCoveredPercentageCloseTo(0, PRECISION)
                .hasMissed(0)
                .hasMissedPercentageCloseTo(0, PRECISION)
                .hasTotal(0).hasToString(Coverage.COVERAGE_NOT_AVAILABLE);
        assertThat(Coverage.NO_COVERAGE.printCoveredPercentage()).isEqualTo(Coverage.COVERAGE_NOT_AVAILABLE);
        assertThat(Coverage.NO_COVERAGE.printMissedPercentage()).isEqualTo(Coverage.COVERAGE_NOT_AVAILABLE);
        assertThat(Coverage.NO_COVERAGE.add(Coverage.NO_COVERAGE)).isEqualTo(Coverage.NO_COVERAGE);
    }

    @Test
    void shouldCreatePercentages() {
        Coverage coverage = new Coverage(6, 4);
        assertThat(coverage).isSet()
                .hasCovered(6)
                .hasCoveredPercentageCloseTo(0.60, PRECISION)
                .hasMissed(4)
                .hasMissedPercentageCloseTo(0.40, PRECISION)
                .hasTotal(10)
                .hasToString("60,00 (6/10)");

        assertThat(coverage.printCoveredPercentage()).isEqualTo("60,00");
        assertThat(coverage.printMissedPercentage()).isEqualTo("40,00");

        assertThat(coverage.add(Coverage.NO_COVERAGE)).isEqualTo(coverage);
        Coverage sum = coverage.add(new Coverage(10, 0));
        assertThat(sum).isEqualTo(new Coverage(16, 4));
        assertThat(sum.printCoveredPercentage()).isEqualTo("80,00");
        assertThat(sum.printMissedPercentage()).isEqualTo("20,00");
    }

    @Test
    void shouldVerifyEquals() {
        EqualsVerifier.forClass(Coverage.class).verify();
    }
}
