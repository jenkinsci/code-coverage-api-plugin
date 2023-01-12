package io.jenkins.plugins.coverage.metrics.visualization.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.ChartModelConfiguration.AxisType;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.echarts.LinesDataSet;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.util.VisibleForTesting;

import io.jenkins.plugins.coverage.metrics.Baseline;
import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageSeriesBuilder}.
 *
 * @author Ullrich Hafner
 */
class CoverageSeriesBuilderTest {
    /**
     * Creates a new build result for the given build.
     *
     * @param buildNumber
     *         the number of the build
     * @param lineCoverage
     *         the line coverage in the build
     * @param branchCoverage
     *         the branch coverage in the build
     *
     * @return the {@link BuildResult} stub, that contains a {@link CoverageBuildAction} instance with the specified
     *         behavior
     */
    @VisibleForTesting
    private BuildResult<CoverageBuildAction> createResult(final int buildNumber,
            final Coverage lineCoverage, final Coverage branchCoverage) {
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        when(action.getAllValues(Baseline.PROJECT)).thenReturn(List.of(lineCoverage, branchCoverage));
        Build build = new Build(buildNumber);

        return new BuildResult<>(build, action);
    }

    @Test
    void shouldHaveEmptyDataSetForEmptyIterator() {
        CoverageSeriesBuilder builder = new CoverageSeriesBuilder();

        LinesDataSet model = builder.createDataSet(createConfiguration(), new ArrayList<>());

        assertThat(model.getDomainAxisSize()).isEqualTo(0);
        assertThat(model.getDataSetIds()).isEmpty();
    }

    @Test
    void shouldCreateChart() {
        CoverageTrendChart trendChart = new CoverageTrendChart();

        BuildResult<CoverageBuildAction> smallLineCoverage = createResult(1,
                new CoverageBuilder().setMetric(Metric.LINE).setCovered(1).setMissed(1).build(),
                new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(3).setMissed(1).build());

        LinesChartModel lineCoverage = trendChart.create(Collections.singletonList(smallLineCoverage),
                createConfiguration());
        verifySeriesDetails(lineCoverage);

        BuildResult<CoverageBuildAction> smallBranchCoverage = createResult(1,
                new CoverageBuilder().setMetric(Metric.LINE).setCovered(3).setMissed(1).build(),
                new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(1).setMissed(1).build());

        LinesChartModel branchCoverage = trendChart.create(Collections.singletonList(smallBranchCoverage),
                createConfiguration());
        verifySeriesDetails(branchCoverage);
    }

    private void verifySeriesDetails(final LinesChartModel lineCoverage) {
        assertThat(lineCoverage.getBuildNumbers()).containsExactly(1);
        assertThat(lineCoverage.getSeries()).hasSize(2);
        assertThat(lineCoverage.getRangeMax()).isEqualTo(100);
        assertThat(lineCoverage.getRangeMin()).isEqualTo(40);
    }

    @Test
    void shouldHaveTwoValuesForSingleBuild() {
        CoverageSeriesBuilder builder = new CoverageSeriesBuilder();

        BuildResult<CoverageBuildAction> singleResult = createResult(1,
                new CoverageBuilder().setMetric(Metric.LINE).setCovered(1).setMissed(1).build(),
                new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(3).setMissed(1).build());

        LinesDataSet dataSet = builder.createDataSet(createConfiguration(), Collections.singletonList(singleResult));

        assertThat(dataSet.getDomainAxisSize()).isEqualTo(1);
        assertThat(dataSet.getDomainAxisLabels()).containsExactly("#1");

        assertThat(dataSet.getDataSetIds()).containsExactlyInAnyOrder(
                CoverageSeriesBuilder.LINE_COVERAGE,
                CoverageSeriesBuilder.BRANCH_COVERAGE);

        assertThat(dataSet.getSeries(CoverageSeriesBuilder.LINE_COVERAGE)).containsExactly(50);
        assertThat(dataSet.getSeries(CoverageSeriesBuilder.BRANCH_COVERAGE)).containsExactly(75);
    }

    private ChartModelConfiguration createConfiguration() {
        ChartModelConfiguration configuration = mock(ChartModelConfiguration.class);
        when(configuration.getAxisType()).thenReturn(AxisType.BUILD);
        return configuration;
    }
}
