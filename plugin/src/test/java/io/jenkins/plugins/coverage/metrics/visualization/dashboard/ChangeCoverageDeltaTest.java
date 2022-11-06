package io.jenkins.plugins.coverage.metrics.visualization.dashboard;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Metric;

import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;
import io.jenkins.plugins.coverage.metrics.CoveragePercentage;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorId;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ProjectCoverage}.
 *
 * @author Florian Orendi
 */
class ChangeCoverageDeltaTest extends CoverageColumnTypeTest {

    @Test
    void shouldGetCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<CoveragePercentage> coverage = CHANGE_COVERAGE_DELTA.getCoverage(action, COVERAGE_METRIC);

        assertThat(coverage).isNotEmpty().satisfies(value -> assertThat(value.get()).isEqualTo(COVERAGE_PERCENTAGE));
    }

    @Test
    void shouldNotReturnNonExistentCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<CoveragePercentage> coverage = CHANGE_COVERAGE_DELTA.getCoverage(action, Metric.FILE);

        assertThat(coverage).isEmpty();
    }

    @Test
    void shouldGetDisplayColors() {
        DisplayColors color = CHANGE_COVERAGE_DELTA.getDisplayColors(COVERAGE_PERCENTAGE);
        assertThat(color).isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.EXCELLENT));
    }

    @Test
    void shouldFormatCoverage() {
        String formattedCoverage = CHANGE_COVERAGE_DELTA.formatCoverage(COVERAGE_PERCENTAGE, LOCALE);
        assertThat(formattedCoverage).isEqualTo("+50,00%");
    }

    @Test
    void shouldProvideReportUrl() {
        assertThat(CHANGE_COVERAGE_DELTA.getAnchor()).isEqualTo("#changeCoverage");
    }
}
