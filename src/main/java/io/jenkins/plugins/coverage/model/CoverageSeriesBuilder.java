package io.jenkins.plugins.coverage.model;

import java.util.HashMap;
import java.util.Map;

import edu.hm.hafner.echarts.SeriesBuilder;

/**
 * Builds one x-axis point for the series of a line chart showing the line and branch coverage of a project.
 *
 * @author Ullrich Hafner
 */
public class CoverageSeriesBuilder extends SeriesBuilder<CoverageBuildAction> {
    static final String LINE_COVERAGE = "line";
    static final String BRANCH_COVERAGE = "branch";

    @Override
    protected Map<String, Integer> computeSeries(final CoverageBuildAction coverageBuildAction) {
        Map<String, Integer> series = new HashMap<>();

        series.put(LINE_COVERAGE, coverageBuildAction.getLineCoverage().getRoundedPercentage());
        series.put(BRANCH_COVERAGE, coverageBuildAction.getBranchCoverage().getRoundedPercentage());

        return series;
    }
}
