package io.jenkins.plugins.coverage.metrics.charts;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.echarts.SeriesBuilder;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Value;

import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;

/**
 * Builds one x-axis point for the series of a line chart showing the line and branch coverage of a project.
 *
 * @author Ullrich Hafner
 */
public class CoverageSeriesBuilder extends SeriesBuilder<CoverageStatistics> {
    static final String LINE_COVERAGE = "line";
    static final String BRANCH_COVERAGE = "branch";

    @Override
    protected Map<String, Integer> computeSeries(final CoverageStatistics statistics) {
        Map<String, Integer> series = new HashMap<>();

        series.put(LINE_COVERAGE, getRoundedPercentage(statistics.getValue(Baseline.PROJECT, Metric.LINE)
                .orElse(Coverage.nullObject(Metric.LINE))));
        series.put(BRANCH_COVERAGE, getRoundedPercentage(statistics.getValue(Baseline.PROJECT, Metric.BRANCH)
                .orElse(Coverage.nullObject(Metric.BRANCH))));

        return series;
    }

    // TODO: we should make the charts accept float values
    private int getRoundedPercentage(final Value coverage) {
        if (coverage instanceof Coverage) {
            return (int) Math.round(((Coverage) coverage).getCoveredPercentage()
                    .multiplyBy(Fraction.getFraction(100, 1)).doubleValue());
        }
        return 0;
    }
}
