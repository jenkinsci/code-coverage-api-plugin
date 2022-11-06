package io.jenkins.plugins.coverage.metrics.visualization.charts;

import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.JacksonFacade;
import edu.hm.hafner.echarts.LineSeries;
import edu.hm.hafner.echarts.LineSeries.FilledMode;
import edu.hm.hafner.echarts.LineSeries.StackedMode;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.echarts.LinesDataSet;
import edu.hm.hafner.echarts.Palette;

import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;

/**
 * Builds the Java side model for a trend chart showing the line and branch coverage of a project.
 * The number of builds to consider is controlled by a {@link ChartModelConfiguration} instance. The created model object
 * can be serialized to JSON (e.g., using the {@link JacksonFacade}) and can be used 1:1 as ECharts configuration object in the corresponding JS file.
 *
 * @author Ullrich Hafner
 * @see JacksonFacade
 */
public class CoverageTrendChart {
    /**
     * Creates the chart for the specified results.
     *
     * @param results
     *         the forensics results to render - these results must be provided in descending order, i.e. the current *
     *         build is the head of the list, then the previous builds, and so on
     * @param configuration
     *         the chart configuration to be used
     *
     * @return the chart model, ready to be serialized to JSON
     */
    public LinesChartModel create(final Iterable<? extends BuildResult<CoverageBuildAction>> results,
            final ChartModelConfiguration configuration) {
        CoverageSeriesBuilder builder = new CoverageSeriesBuilder();
        LinesDataSet dataSet = builder.createDataSet(configuration, results);

        LinesChartModel model = new LinesChartModel(dataSet);
        if (!dataSet.isEmpty()) {
            model.useContinuousRangeAxis();
            model.setRangeMax(100);
            model.setRangeMin(Math.max(0,
                    Math.min(createRangeMinFor(dataSet, CoverageSeriesBuilder.LINE_COVERAGE),
                            createRangeMinFor(dataSet, CoverageSeriesBuilder.BRANCH_COVERAGE)
                    )));

            LineSeries lineSeries = new LineSeries("Line", Palette.GREEN.getNormal(),
                    StackedMode.SEPARATE_LINES, FilledMode.FILLED);
            lineSeries.addAll(dataSet.getSeries(CoverageSeriesBuilder.LINE_COVERAGE));
            model.addSeries(lineSeries);

            LineSeries branchSeries = new LineSeries("Branch", Palette.GREEN.getHover(),
                    StackedMode.SEPARATE_LINES, FilledMode.FILLED);
            branchSeries.addAll(dataSet.getSeries(CoverageSeriesBuilder.BRANCH_COVERAGE));
            model.addSeries(branchSeries);
        }
        return model;
    }

    private int createRangeMinFor(final LinesDataSet dataSet, final String coverage) {
        return min(dataSet, coverage) - 10;
    }

    private Integer min(final LinesDataSet dataSet, final String dataSetId) {
        return dataSet.getSeries(dataSetId).stream().reduce(Math::min).orElse(0);
    }
}
