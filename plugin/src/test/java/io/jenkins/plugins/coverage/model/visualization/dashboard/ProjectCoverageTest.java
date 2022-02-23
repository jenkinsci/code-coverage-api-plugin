package io.jenkins.plugins.coverage.model.visualization.dashboard;

import org.apache.commons.lang3.math.Fraction;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;

import static org.assertj.core.api.Assertions.*;

class ProjectCoverageTest extends CoverageColumnTypeTest {

    @Test
    void shouldGetCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<Fraction> coverage = PROJECT_COVERAGE.getCoverage(action, COVERAGE_METRIC);

        assertThat(coverage).isNotEmpty().satisfies(value -> assertThat(value.get()).isEqualTo(COVERAGE));
    }

    @Test
    void shouldNotReturnNonExistentCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<Fraction> coverage = PROJECT_COVERAGE.getCoverage(action, CoverageMetric.FILE);

        assertThat(coverage).isEmpty();
    }

    /*@Test
    void shouldGetFillColor() {
        String color = PROJECT_COVERAGE.getFillColor(COVERAGE);
        assertThat(color).isEqualTo(CoverageColorizationLevelValues.LVL_50_DECREASE5.getFillColorAsHex());
    }

    @Test
    void shouldGetLineColor() {
        String color = PROJECT_COVERAGE.getLineColor(COVERAGE);
        assertThat(color).isEqualTo(CoverageColorizationLevelValues.LVL_50_DECREASE5.getLineColorAsHex());
    }*/

    @Test
    void shouldFormatCoverage() {
        String formattedCoverage = PROJECT_COVERAGE.formatCoverage(COVERAGE, LOCALE);
        assertThat(formattedCoverage).isEqualTo("50,00%");
    }
}
