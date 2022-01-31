package io.jenkins.plugins.coverage.model;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the class {@link Coverage}.
 *
 * @author Ullrich Hafner
 */
class CoverageTest {
    @Test
    void shouldProvideNullObject() {
        assertThat(Coverage.NO_COVERAGE).isNotSet()
                .hasCovered(0)
                .hasCoveredPercentage(Fraction.ZERO)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(0).hasToString(Messages.Coverage_Not_Available());
        assertThat(Coverage.NO_COVERAGE.formatCoveredPercentage()).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(Coverage.NO_COVERAGE.formatMissedPercentage()).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(Coverage.NO_COVERAGE.add(Coverage.NO_COVERAGE)).isEqualTo(Coverage.NO_COVERAGE);
    }

    @Test
    void shouldCreatePercentages() {
        Coverage coverage = new Coverage(6, 4);
        assertThat(coverage).isSet()
                .hasCovered(6)
                .hasCoveredPercentage(Fraction.getFraction(6, 10))
                .hasMissed(4)
                .hasMissedPercentage(Fraction.getFraction(4, 10))
                .hasTotal(10)
                .hasToString("60.00% (6/10)");

        assertThat(coverage.formatCoveredPercentage()).isEqualTo("60.00%");
        assertThat(coverage.formatMissedPercentage()).isEqualTo("40.00%");

        assertThat(coverage.add(Coverage.NO_COVERAGE)).isEqualTo(coverage);
        Coverage sum = coverage.add(new Coverage(10, 0));
        assertThat(sum).isEqualTo(new Coverage(16, 4));
        assertThat(sum.formatCoveredPercentage()).isEqualTo("80.00%");
        assertThat(sum.formatMissedPercentage()).isEqualTo("20.00%");
    }

    @Test
    void shouldVerifyEquals() {
        EqualsVerifier.forClass(Coverage.class).verify();
    }
}
