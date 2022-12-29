package io.jenkins.plugins.coverage.metrics;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FractionValue;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.FilteredLog;

import hudson.Functions;
import hudson.model.Job;
import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProviderFactory;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageChangeTendency;

import static io.jenkins.plugins.coverage.metrics.testutil.JobStubs.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link CoverageMetricColumn}.
 *
 * @author Florian Orendi
 */
@DefaultLocale("en")
class CoverageMetricColumnTest extends AbstractCoverageTest {
    private static final String COLUMN_NAME = "Test Column";
    private static final Metric COVERAGE_METRIC = Metric.BRANCH;

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();

    @Test
    void shouldHaveWorkingDataGetters() {
        CoverageMetricColumn column = createColumn();

        assertThat(column.getColumnName()).isEqualTo(COLUMN_NAME);
        assertThat(column.getBaseline()).isEqualTo(Baseline.PROJECT);
        assertThat(column.getMetric()).isEqualTo(COVERAGE_METRIC);
        assertThat(column.getRelativeCoverageUrl(createJob())).isEmpty();
    }

    @Test
    void shouldProvideSelectedColumn() {
        CoverageMetricColumn column = createColumn();
        Job<?, ?> job = createJobWithCoverageAction();

        column.setBaseline(Baseline.PROJECT);
        assertThat(column.getBaseline()).isEqualTo(Baseline.PROJECT);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#overview");

        column.setBaseline(Baseline.PROJECT_DELTA);
        assertThat(column.getBaseline()).isEqualTo(Baseline.PROJECT_DELTA);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#overview");

        column.setBaseline(Baseline.CHANGE);
        assertThat(column.getBaseline()).isEqualTo(Baseline.CHANGE);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#changeCoverage");

        column.setBaseline(Baseline.CHANGE_DELTA);
        assertThat(column.getBaseline()).isEqualTo(Baseline.CHANGE_DELTA);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#changeCoverage");

        column.setBaseline(Baseline.INDIRECT);
        assertThat(column.getBaseline()).isEqualTo(Baseline.INDIRECT);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#indirectCoverage");
    }

    @Test
    void shouldProvideBackgroundColorFillPercentage() {
        CoverageMetricColumn column = createColumn();

        assertThat(column.getBackgroundColorFillPercentage("+5,0%")).isEqualTo("100%");
        assertThat(column.getBackgroundColorFillPercentage("+5.0%")).isEqualTo("100%");
        assertThat(column.getBackgroundColorFillPercentage("5,00%")).isEqualTo("5.00%");
        assertThat(column.getBackgroundColorFillPercentage("5.00%")).isEqualTo("5.00%");
    }

    @Test
    void shouldShowNoResultIfBuild() {
        CoverageMetricColumn column = createColumn();

        Job<?, ?> job = createJob();

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());

        Optional<? extends Value> coverageValue = column.getCoverageValue(job);
        assertThat(coverageValue).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultIfNoAction() {
        CoverageMetricColumn column = createColumn();

        Job<?, ?> job = createJobWithActions();

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultForUnavailableMetric() {
        CoverageMetricColumn column = createColumn();
        column.setMetric(Metric.MUTATION);

        Job<?, ?> job = createJobWithCoverageAction();

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();

        column.setBaseline(Baseline.PROJECT_DELTA);

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();
    }

    @Test
    void shouldCalculateProjectCoverage() {
        CoverageMetricColumn column = createColumn();

        Fraction coverageFraction = Fraction.getFraction(1, 2);
        CoveragePercentage coveragePercentage = CoveragePercentage.valueOf(coverageFraction);

        Job<?, ?> job = createJobWithCoverageAction();

        assertThat(column.getCoverageText(job)).isEqualTo("93.97%");
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(coverage -> {
                    assertThat(coverage.get()).isEqualTo(new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(109).setMissed(7).build());
                    assertThat(column.getDisplayColors(job, coverage).getLineColor())
                            .isEqualTo(Color.white);
                });
    }

    @Test
    void shouldCalculateProjectCoverageDelta() {
        CoverageMetricColumn column = createColumn();
        column.setBaseline(Baseline.PROJECT_DELTA);

        Fraction coverageDelta = Fraction.getFraction(1, 20);
        CoveragePercentage coverageDeltaPercentage = CoveragePercentage.valueOf(coverageDelta);
        String coverageDeltaPercentageText =
                coverageDeltaPercentage.formatDeltaPercentage(Functions.getCurrentLocale());
        Job<?, ?> job = createJobWithCoverageAction();

        assertThat(column.getCoverageText(job)).isEqualTo(coverageDeltaPercentageText);
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(coverage -> {
                    assertThat(coverage.get()).isEqualTo(new FractionValue(Metric.BRANCH, coverageDelta));
                    assertThat(column.getDisplayColors(job, coverage))
                            .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(
                                    CoverageChangeTendency.INCREASED.getColorizationId()));
                });
    }

    private CoverageMetricColumn createColumn() {
        CoverageMetricColumn column = new CoverageMetricColumn();
        column.setColumnName(COLUMN_NAME);
        column.setBaseline(Baseline.PROJECT);
        column.setMetric(COVERAGE_METRIC);
        return column;
    }

    private Job<?, ?> createJobWithCoverageAction() {
        var node = readJacocoResult(JACOCO_CODING_STYLE_FILE);
        var run = mock(Run.class);
        var delta = new TreeMap<Metric, Fraction>();
        delta.put(Metric.BRANCH, Fraction.getFraction("0.05"));
        CoverageBuildAction coverageBuildAction =
                new CoverageBuildAction(run, "coverage", "Code Coverage", node, QualityGateStatus.INACTIVE, new FilteredLog("Test"),
                        "-", delta, List.of(), new TreeMap<>(), List.of(), false);
        when(run.getAction(CoverageBuildAction.class)).thenReturn(coverageBuildAction);
        when(run.getActions(CoverageBuildAction.class)).thenReturn(Collections.singletonList(coverageBuildAction));

        var job = mock(Job.class);
        when(job.getLastCompletedBuild()).thenReturn(run);

        return job;
    }
}
