package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorId;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link ProjectCoverage}.
 *
 * @author Florian Orendi
 */
class ChangeCoverageTest extends CoverageColumnTypeTest {

    @Test
    void shouldGetCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<Fraction> coverage = CHANGE_COVERAGE.getCoverage(action, COVERAGE_METRIC);

        assertThat(coverage).isNotEmpty().satisfies(value -> assertThat(value.get()).isEqualTo(COVERAGE));
    }

    @Test
    void shouldNotReturnNonExistentCoverage() {
        CoverageBuildAction action = createCoverageBuildAction();
        Optional<Fraction> coverage = CHANGE_COVERAGE.getCoverage(action, CoverageMetric.FILE);

        assertThat(coverage).isEmpty();
    }

    @Test
    void shouldGetDisplayColors() {
        DisplayColors color = CHANGE_COVERAGE.getDisplayColors(COVERAGE);
        assertThat(color).isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.VERY_BAD));
    }

    @Test
    void shouldFormatCoverage() {
        String formattedCoverage = CHANGE_COVERAGE.formatCoverage(COVERAGE, LOCALE);
        assertThat(formattedCoverage).isEqualTo("50,00%");
    }
}
