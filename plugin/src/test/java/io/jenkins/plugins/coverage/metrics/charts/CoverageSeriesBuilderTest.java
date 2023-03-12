package io.jenkins.plugins.coverage.metrics.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.ChartModelConfiguration.AxisType;
import edu.hm.hafner.echarts.line.LinesChartModel;
import edu.hm.hafner.echarts.line.LinesDataSet;
import edu.hm.hafner.util.VisibleForTesting;

import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageSeriesBuilder}.
 *
 * @author Ullrich Hafner
 */
class CoverageSeriesBuilderTest {
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

        BuildResult<CoverageStatistics> smallLineCoverage = createResult(1,
                new CoverageBuilder().setMetric(Metric.LINE).setCovered(1).setMissed(1).build(),
                new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(3).setMissed(1).build());

        LinesChartModel lineCoverage = trendChart.create(Collections.singletonList(smallLineCoverage),
                createConfiguration());
        verifySeriesDetails(lineCoverage);

        BuildResult<CoverageStatistics> smallBranchCoverage = createResult(1,
                new CoverageBuilder().setMetric(Metric.LINE).setCovered(3).setMissed(1).build(),
                new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(1).setMissed(1).build());

        LinesChartModel branchCoverage = trendChart.create(Collections.singletonList(smallBranchCoverage),
                createConfiguration());
        verifySeriesDetails(branchCoverage);
    }

    @VisibleForTesting
    private BuildResult<CoverageStatistics> createResult(final int buildNumber,
            final Coverage lineCoverage, final Coverage branchCoverage) {
        var statistics = new CoverageStatistics(
                List.of(lineCoverage, branchCoverage), Collections.emptyNavigableMap(),
                Collections.emptyList(), Collections.emptyNavigableMap(),
                Collections.emptyList(), Collections.emptyNavigableMap());
        Build build = new Build(buildNumber);

        return new BuildResult<>(build, statistics);
    }

    private void verifySeriesDetails(final LinesChartModel lineCoverage) {
        assertThat(lineCoverage.getBuildNumbers()).containsExactly(1);
        assertThat(lineCoverage.getSeries()).hasSize(2);
        assertThat(lineCoverage.getRangeMax()).isEqualTo(100.0);
        assertThat(lineCoverage.getRangeMin()).isEqualTo(50.0);
    }

    @Test
    void shouldHaveTwoValuesForSingleBuild() {
        CoverageSeriesBuilder builder = new CoverageSeriesBuilder();

        BuildResult<CoverageStatistics> singleResult = createResult(1,
                new CoverageBuilder().setMetric(Metric.LINE).setCovered(1).setMissed(1).build(),
                new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(3).setMissed(1).build());

        LinesDataSet dataSet = builder.createDataSet(createConfiguration(), Collections.singletonList(singleResult));

        assertThat(dataSet.getDomainAxisSize()).isEqualTo(1);
        assertThat(dataSet.getDomainAxisLabels()).containsExactly("#1");

        assertThat(dataSet.getDataSetIds()).containsExactlyInAnyOrder(
                CoverageSeriesBuilder.LINE_COVERAGE,
                CoverageSeriesBuilder.BRANCH_COVERAGE);

        assertThat(dataSet.getSeries(CoverageSeriesBuilder.LINE_COVERAGE)).containsExactly(50.0);
        assertThat(dataSet.getSeries(CoverageSeriesBuilder.BRANCH_COVERAGE)).containsExactly(75.0);
    }

    private ChartModelConfiguration createConfiguration() {
        ChartModelConfiguration configuration = mock(ChartModelConfiguration.class);
        when(configuration.getAxisType()).thenReturn(AxisType.BUILD);
        return configuration;
    }
}
