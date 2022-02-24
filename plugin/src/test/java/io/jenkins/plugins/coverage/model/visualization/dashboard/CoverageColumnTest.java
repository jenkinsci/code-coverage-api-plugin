package io.jenkins.plugins.coverage.model.visualization.dashboard;

import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import hudson.Functions;
import hudson.model.Job;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.util.FractionFormatter;
import io.jenkins.plugins.coverage.model.util.WebUtils;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProviderFactory;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;

import static io.jenkins.plugins.coverage.model.testutil.CoverageStubs.*;
import static io.jenkins.plugins.coverage.model.testutil.JobStubs.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageColumn}.
 *
 * @author Florian Orendi
 */
class CoverageColumnTest {

    private static final String COLUMN_NAME = "Test Column";
    private static final ProjectCoverage PROJECT_COVERAGE = new ProjectCoverage();
    private static final ProjectCoverageDelta PROJECT_COVERAGE_DELTA = new ProjectCoverageDelta();
    private static final CoverageMetric COVERAGE_METRIC = CoverageMetric.BRANCH;

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createColorProvider();

    @Test
    void shouldHaveWorkingDataGetters() {
        CoverageColumn column = createColumn();
        assertThat(column.getColumnName()).isEqualTo(COLUMN_NAME);
        assertThat(column.getCoverageType()).isEqualTo(PROJECT_COVERAGE.getDisplayName());
        assertThat(column.getCoverageMetric()).isEqualTo(COVERAGE_METRIC.getName());
    }

    @Test
    void shouldProvideBackgroundColorFillPercentage() {
        CoverageColumn column = createColumn();
        assertThat(column.getBackgroundColorFillPercentage("+5,0%")).isEqualTo("100%");
        assertThat(column.getBackgroundColorFillPercentage("5,00%")).isEqualTo("5.00%");
    }

    @Test
    void shouldShowNoResultIfBuild() {
        CoverageColumn column = createColumn();

        Job<?, ?> job = createJob();

        assertThat(column.getCoverageText(job)).isEqualTo(CoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.COVERAGE_DEFAULT_URL);

        Optional<Fraction> coverageValue = column.getCoverageValue(job);
        assertThat(coverageValue).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultIfNoAction() {
        CoverageColumn column = createColumn();

        Job<?, ?> job = createJobWithActions();

        assertThat(column.getCoverageText(job)).isEqualTo(CoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.COVERAGE_DEFAULT_URL);
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultForUnavailableCoverageMetric() {
        CoverageColumn column = createColumn();
        column.setCoverageMetric(CoverageMetric.CLASS.getName());

        Job<?, ?> job = createJobWithCoverageAction(Fraction.ZERO, Fraction.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(CoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.COVERAGE_DEFAULT_URL);
        assertThat(column.getCoverageValue(job)).isEmpty();

        column.setCoverageType(PROJECT_COVERAGE_DELTA.getDisplayName());

        assertThat(column.getCoverageText(job)).isEqualTo(CoverageColumn.COVERAGE_NA_TEXT);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.COVERAGE_DEFAULT_URL);
        assertThat(column.getCoverageValue(job)).isEmpty();
    }

    @Test
    void shouldCalculateProjectCoverage() {
        CoverageColumn column = createColumn();

        Fraction coverageFraction = Fraction.getFraction(1, 2);
        Fraction coveragePercentage = FractionFormatter.transformFractionToPercentage(coverageFraction);
        String coveragePercentageText =
                FractionFormatter.formatPercentage(coveragePercentage, Functions.getCurrentLocale());

        Job<?, ?> job = createJobWithCoverageAction(Fraction.ZERO, coverageFraction);

        assertThat(column.getCoverageText(job)).isEqualTo(coveragePercentageText);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.COVERAGE_DEFAULT_URL);
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(coverage -> {
                    assertThat(coverage.get()).isEqualTo(coveragePercentage);
                    assertThat(column.getDisplayColors(job, coverage))
                            .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(CoverageLevel.LVL_50.getColorizationId()));
                });
    }

    @Test
    void shouldCalculateProjectCoverageDelta() {
        CoverageColumn column = createColumn();
        column.setCoverageType(PROJECT_COVERAGE_DELTA.getDisplayName());

        Fraction coverageDelta = Fraction.getFraction(1, 20);
        Fraction coverageDeltaPercentage = FractionFormatter.transformFractionToPercentage(coverageDelta);
        String coverageDeltaPercentageText =
                FractionFormatter.formatDeltaPercentage(coverageDeltaPercentage, Functions.getCurrentLocale());
        Job<?, ?> job = createJobWithCoverageAction(coverageDelta, Fraction.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(coverageDeltaPercentageText);
        assertThat(column.getRelativeCoverageUrl()).isEqualTo(WebUtils.COVERAGE_DEFAULT_URL);
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(coverage -> {
                    assertThat(coverage.get()).isEqualTo(coverageDeltaPercentage);
                    assertThat(column.getDisplayColors(job, coverage))
                            .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(
                                    CoverageChangeTendency.INCREASED.getColorizationId()));
                });
    }

    /**
     * Creates a {@link CoverageColumn} for displaying the project coverage.
     *
     * @return the created column.
     */
    private CoverageColumn createColumn() {
        CoverageColumn column = new CoverageColumn();
        column.setColumnName(COLUMN_NAME);
        column.setCoverageType(PROJECT_COVERAGE.getDisplayName());
        column.setCoverageMetric(COVERAGE_METRIC.getName());
        return column;
    }

    /**
     * Creates a mock of {@link CoverageBuildAction} which provides the passed project coverage delta and percentage.
     *
     * @param coverageDelta
     *         The project coverage delta
     * @param coveragePercentage
     *         The project coverage percentage
     *
     * @return the created mock
     */
    private Job<?, ?> createJobWithCoverageAction(final Fraction coverageDelta, final Fraction coveragePercentage) {
        CoverageBuildAction coverageBuildAction =
                createCoverageBuildAction(COVERAGE_METRIC, coverageDelta, coveragePercentage);
        return createJobWithActions(coverageBuildAction);
    }
}
