package io.jenkins.plugins.coverage.metrics.visualization.dashboard;

import java.util.Optional;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Metric;

import hudson.Functions;
import hudson.model.Job;

import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;
import io.jenkins.plugins.coverage.metrics.CoveragePercentage;
import io.jenkins.plugins.coverage.metrics.Messages;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProviderFactory;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageLevel;

import static io.jenkins.plugins.coverage.metrics.testutil.CoverageStubs.*;
import static io.jenkins.plugins.coverage.metrics.testutil.JobStubs.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageColumn}.
 *
 * @author Florian Orendi
 */
@DefaultLocale("en")
class CoverageColumnTest {
    private static final String COLUMN_NAME = "Test Column";
    private static final ProjectCoverage PROJECT_COVERAGE = new ProjectCoverage();
    private static final ProjectCoverageDelta PROJECT_COVERAGE_DELTA = new ProjectCoverageDelta();
    private static final Metric COVERAGE_METRIC = Metric.BRANCH;

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();

    @Test
    void shouldHaveWorkingDataGetters() {
        CoverageColumn column = createColumn();
        assertThat(column.getColumnName()).isEqualTo(COLUMN_NAME);
        assertThat(column.getCoverageType()).isEqualTo(PROJECT_COVERAGE.getDisplayName());
        assertThat(column.getCoverageMetric()).isEqualTo(COVERAGE_METRIC.name());
    }

    @Test
    void shouldProvideSelectedColumn() {
        CoverageColumn column = createColumn();
        Job<?, ?> job = createJobWithCoverageAction(Fraction.ZERO);

        column.setCoverageType(Messages.Project_Coverage_Type());
        assertThat(column.getCoverageType()).isEqualTo(Messages.Project_Coverage_Type());
        assertThat(column.getSelectedCoverageColumnType()).isInstanceOf(ProjectCoverage.class);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#overview");

        column.setCoverageType(Messages.Project_Coverage_Delta_Type());
        assertThat(column.getCoverageType()).isEqualTo(Messages.Project_Coverage_Delta_Type());
        assertThat(column.getSelectedCoverageColumnType()).isInstanceOf(ProjectCoverageDelta.class);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#overview");

        column.setCoverageType(Messages.Change_Coverage_Type());
        assertThat(column.getCoverageType()).isEqualTo(Messages.Change_Coverage_Type());
        assertThat(column.getSelectedCoverageColumnType()).isInstanceOf(ChangeCoverage.class);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#changeCoverage");

        column.setCoverageType(Messages.Change_Coverage_Delta_Type());
        assertThat(column.getCoverageType()).isEqualTo(Messages.Change_Coverage_Delta_Type());
        assertThat(column.getSelectedCoverageColumnType()).isInstanceOf(ChangeCoverageDelta.class);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#changeCoverage");

        column.setCoverageType(Messages.Indirect_Coverage_Changes_Type());
        assertThat(column.getCoverageType()).isEqualTo(Messages.Indirect_Coverage_Changes_Type());
        assertThat(column.getSelectedCoverageColumnType()).isInstanceOf(IndirectCoverageChanges.class);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#indirectCoverage");
    }

    @Test
    void shouldProvideEmptyCoverageUrlWithoutAction() {
        CoverageColumn column = createColumn();
        assertThat(column.getRelativeCoverageUrl(createJob())).isEmpty();
    }

    @Test
    void shouldProvideBackgroundColorFillPercentage() {
        CoverageColumn column = createColumn();
        assertThat(column.getBackgroundColorFillPercentage("+5,0%")).isEqualTo("100%");
        assertThat(column.getBackgroundColorFillPercentage("+5.0%")).isEqualTo("100%");
        assertThat(column.getBackgroundColorFillPercentage("5,00%")).isEqualTo("5.00%");
        assertThat(column.getBackgroundColorFillPercentage("5.00%")).isEqualTo("5.00%");
    }

    @Test
    void shouldShowNoResultIfBuild() {
        CoverageColumn column = createColumn();

        Job<?, ?> job = createJob();

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());

        Optional<CoveragePercentage> coverageValue = column.getCoverageValue(job);
        assertThat(coverageValue).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultIfNoAction() {
        CoverageColumn column = createColumn();

        Job<?, ?> job = createJobWithActions();

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultForUnavailableMetric() {
        CoverageColumn column = createColumn();
        column.setCoverageMetric(Metric.CLASS.name());

        Job<?, ?> job = createJobWithCoverageAction(Fraction.ZERO);

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();

        column.setCoverageType(PROJECT_COVERAGE_DELTA.getDisplayName());

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();
    }

    @Test
    void shouldCalculateProjectCoverage() {
        CoverageColumn column = createColumn();

        Fraction coverageFraction = Fraction.getFraction(1, 2);
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(coverageFraction);
        String coveragePercentageText = coveragePercentage.formatPercentage(Functions.getCurrentLocale());

        Job<?, ?> job = createJobWithCoverageAction(coverageFraction);

        assertThat(column.getCoverageText(job)).isEqualTo(coveragePercentageText);
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
        CoveragePercentage coverageDeltaPercentage = CoveragePercentage.valueOf(coverageDelta);
        String coverageDeltaPercentageText =
                coverageDeltaPercentage.formatDeltaPercentage(Functions.getCurrentLocale());
        Job<?, ?> job = createJobWithCoverageAction(coverageDelta);

        assertThat(column.getCoverageText(job)).isEqualTo(coverageDeltaPercentageText);
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
        column.setCoverageMetric(COVERAGE_METRIC.name());
        return column;
    }

    /**
     * Creates a mock of {@link CoverageBuildAction} which provides all available coverages.
     *
     * @param coverage
     *         The coverage to be provided
     *
     * @return the created mock
     */
    private Job<?, ?> createJobWithCoverageAction(final Fraction coverage) {
        CoverageBuildAction coverageBuildAction =
                createCoverageBuildAction(COVERAGE_METRIC, coverage);
        return createJobWithActions(coverageBuildAction);
    }
}
