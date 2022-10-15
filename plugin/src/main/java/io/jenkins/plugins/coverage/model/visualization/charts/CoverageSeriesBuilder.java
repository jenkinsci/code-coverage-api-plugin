package io.jenkins.plugins.coverage.model.visualization.charts;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.echarts.SeriesBuilder;
import edu.hm.hafner.metric.Coverage;

import io.jenkins.plugins.coverage.model.CoverageBuildAction;

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

        series.put(LINE_COVERAGE, getRoundedPercentage(coverageBuildAction.getLineCoverage()));
        series.put(BRANCH_COVERAGE, getRoundedPercentage(coverageBuildAction.getBranchCoverage()));
        return series;
    }

    /**
     * Returns the covered percentage as rounded integer value in the range of {@code [0, 100]}.
     *
     * @return the covered percentage
     */
    // TODO: we should make the charts accept float values
    public int getRoundedPercentage(final Coverage coverage) {
        if (coverage.isSet()) {
            return (int) Math.round(coverage.getCoveredPercentage()
                    .multiplyBy(Fraction.getFraction(100, 1)).doubleValue());
        }
        return 0;
    }
}
