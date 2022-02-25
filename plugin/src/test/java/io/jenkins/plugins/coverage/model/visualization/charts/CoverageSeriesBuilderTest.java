package io.jenkins.plugins.coverage.model.visualization.charts;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.ChartModelConfiguration.AxisType;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.echarts.LinesDataSet;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageBuildAction;

import static io.jenkins.plugins.coverage.model.ResultStubs.*;
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

        BuildResult<CoverageBuildAction> smallLineCoverage = createResult(1, new Coverage(1, 1), new Coverage(3, 1));

        LinesChartModel lineCoverage = trendChart.create(Collections.singletonList(smallLineCoverage),
                createConfiguration());
        verifySeriesDetails(lineCoverage);

        BuildResult<CoverageBuildAction> smallBranchCoverage = createResult(1, new Coverage(3, 1), new Coverage(1, 1));

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

        BuildResult<CoverageBuildAction> singleResult = createResult(1, new Coverage(1, 1), new Coverage(3, 1));

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
