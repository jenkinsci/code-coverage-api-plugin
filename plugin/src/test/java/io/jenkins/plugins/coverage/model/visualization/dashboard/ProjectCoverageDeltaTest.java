package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;

import static org.assertj.core.api.Assertions.*;

class ProjectCoverageDeltaTest extends CoverageColumnTypeTest {

    @Test
    void shouldGetCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<Fraction> coverage = PROJECT_COVERAGE_DELTA.getCoverage(action, COVERAGE_METRIC);

        assertThat(coverage).isNotEmpty().satisfies(value -> assertThat(value.get()).isEqualTo(COVERAGE_DELTA));
    }

    @Test
    void shouldNotReturnNonExistentCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<Fraction> coverage = PROJECT_COVERAGE_DELTA.getCoverage(action, CoverageMetric.FILE);

        assertThat(coverage).isEmpty();
    }

    /*@Test
    void shouldGetFillColor() {
        String color = PROJECT_COVERAGE_DELTA.getFillColor(COVERAGE_DELTA);
        assertThat(color).isEqualTo(CoverageChangeTendencyValues.INCREASED.getFillColorAsHex());
    }

    @Test
    void shouldGetLineColor() {
        String color = PROJECT_COVERAGE_DELTA.getLineColor(COVERAGE_DELTA);
        assertThat(color).isEqualTo(CoverageChangeTendencyValues.INCREASED.getLineColorAsHex());
    }*/

    @Test
    void shouldFormatCoverage() {
        String formattedCoverage = PROJECT_COVERAGE_DELTA.formatCoverage(COVERAGE_DELTA, LOCALE);
        assertThat(formattedCoverage).isEqualTo("+50,00%");
    }
}
