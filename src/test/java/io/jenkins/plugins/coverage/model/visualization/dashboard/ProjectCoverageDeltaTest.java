package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoveragePercentage;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorId;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ProjectCoverageDelta}.
 *
 * @author Florian Orendi
 */
class ProjectCoverageDeltaTest extends CoverageColumnTypeTest {

    @Test
    void shouldGetCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<CoveragePercentage> coverage = PROJECT_COVERAGE_DELTA.getCoverage(action, COVERAGE_METRIC);

        assertThat(coverage).isNotEmpty().satisfies(value -> assertThat(value.get()).isEqualTo(COVERAGE_PERCENTAGE));
    }

    @Test
    void shouldNotReturnNonExistentCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<CoveragePercentage> coverage = PROJECT_COVERAGE_DELTA.getCoverage(action, CoverageMetric.FILE);

        assertThat(coverage).isEmpty();
    }

    @Test
    void shouldGetDisplayColors() {
        DisplayColors color = PROJECT_COVERAGE_DELTA.getDisplayColors(COVERAGE_PERCENTAGE);
        assertThat(color).isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.EXCELLENT));
    }

    @Test
    void shouldFormatCoverage() {
        String formattedCoverage = PROJECT_COVERAGE_DELTA.formatCoverage(COVERAGE_PERCENTAGE, LOCALE);
        assertThat(formattedCoverage).isEqualTo("+50,00%");
    }

    @Test
    void shouldProvideReportUrl() {
        assertThat(PROJECT_COVERAGE_DELTA.getAnchor()).isEqualTo("#overview");
    }
}
