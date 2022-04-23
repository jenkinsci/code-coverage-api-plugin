package io.jenkins.plugins.coverage.model;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.jqno.equalsverifier.EqualsVerifier;

import io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder;

import static io.jenkins.plugins.coverage.model.Assertions.*;
import static io.jenkins.plugins.coverage.model.Coverage.CoverageBuilder.*;

/**
 * Tests the class {@link Coverage}.
 *
 * @author Ullrich Hafner
 */
class CoverageTest {
    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldProvideNullObject() {
        assertThat(Coverage.NO_COVERAGE).isNotSet()
                .hasCovered(0)
                .hasCoveredPercentage(Fraction.ZERO)
                .hasRoundedPercentage(0)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(0)
                .hasToString(Messages.Coverage_Not_Available());
        assertThat(Coverage.NO_COVERAGE.formatCoveredPercentage()).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(Coverage.NO_COVERAGE.formatMissedPercentage()).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(Coverage.NO_COVERAGE.add(Coverage.NO_COVERAGE)).isEqualTo(Coverage.NO_COVERAGE);

        assertThat(Coverage.NO_COVERAGE.serializeToString()).isEqualTo("0/0");
        assertThat(Coverage.valueOf("0/0")).isEqualTo(Coverage.NO_COVERAGE);
    }

    @Test
    void shouldCreatePercentages() {
        Coverage coverage = new CoverageBuilder().setCovered(6).setMissed(4).build();
        assertThat(coverage).isSet()
                .hasCovered(6)
                .hasCoveredPercentage(Fraction.getFraction(6, 10))
                .hasRoundedPercentage(60)
                .hasMissed(4)
                .hasMissedPercentage(Fraction.getFraction(4, 10))
                .hasTotal(10)
                .hasToString("60.00% (6/10)");

        assertThat(coverage.serializeToString()).isEqualTo("6/10");
        assertThat(Coverage.valueOf("6/10")).isEqualTo(coverage);

        assertThat(coverage.formatCoveredPercentage()).isEqualTo("60.00%");
        assertThat(coverage.formatMissedPercentage()).isEqualTo("40.00%");

        assertThat(coverage.add(Coverage.NO_COVERAGE)).isEqualTo(coverage);
        Coverage sum = coverage.add(new CoverageBuilder().setCovered(10).setMissed(0).build());
        assertThat(sum).isEqualTo(new CoverageBuilder().setCovered(16).setMissed(4).build()).hasRoundedPercentage(80);
        assertThat(sum.formatCoveredPercentage()).isEqualTo("80.00%");
        assertThat(sum.formatMissedPercentage()).isEqualTo("20.00%");
    }

    @Test
    void shouldThrowExceptionForInvalidBuilderArguments() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CoverageBuilder().setCovered(1).setMissed(1).setTotal(1).build());
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CoverageBuilder().setCovered(1).build());
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CoverageBuilder().setMissed(1).build());
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CoverageBuilder().setTotal(1).build());
    }

    @Test
    void shouldProvideMultipleOptionsToCreateCoverage() {
        assertThat(new CoverageBuilder().setCovered(1).setMissed(2).build())
                .hasCovered(1)
                .hasMissed(2)
                .hasTotal(3);
        assertThat(new CoverageBuilder().setCovered(1).setTotal(3).build())
                .hasCovered(1)
                .hasMissed(2)
                .hasTotal(3);
        assertThat(new CoverageBuilder().setMissed(2).setTotal(3).build())
                .hasCovered(1)
                .hasMissed(2)
                .hasTotal(3);
    }

    @Test
    void shouldCacheValues() {
        for (int covered = 0; covered < CACHE_SIZE; covered++) {
            for (int missed = 0; missed < CACHE_SIZE; missed++) {
                CoverageBuilder builder = new CoverageBuilder().setCovered(covered).setMissed(missed);

                assertThat(builder.build())
                        .isSameAs(builder.build())
                        .hasCovered(covered)
                        .hasMissed(missed);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] Illegal coverage serialization = \"{0}\"")
    @ValueSource(strings = {"", "-", "/", "0/", "0/0/0", "/0", "a/1", "1/a", "1.0/1.0", "4/3"})
    @DisplayName("Should throw exception for illegal serializations")
    void shouldThrowExceptionForInvalidCoverages(final String serialization) {
        assertThatIllegalArgumentException().isThrownBy(() -> Coverage.valueOf(serialization))
                .withMessageContaining(serialization);
    }

    @Test
    void shouldVerifyEquals() {
        EqualsVerifier.forClass(Coverage.class).verify();
    }
}
