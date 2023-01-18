package io.jenkins.plugins.coverage.metrics.charts;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.echarts.SeriesBuilder;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Metric;

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
    static final String MUTATION_COVERAGE = "mutation";

    @Override
    protected Map<String, Integer> computeSeries(final CoverageStatistics statistics) {
        Map<String, Integer> series = new HashMap<>();

        series.put(LINE_COVERAGE, getRoundedPercentage(statistics, Metric.LINE));
        if (statistics.containsValue(Baseline.PROJECT, Metric.BRANCH)) {
            series.put(BRANCH_COVERAGE, getRoundedPercentage(statistics, Metric.BRANCH));
        }
        if (statistics.containsValue(Baseline.PROJECT, Metric.MUTATION)) {
            series.put(MUTATION_COVERAGE, getRoundedPercentage(statistics, Metric.MUTATION));
        }
        return series;
    }

    private int getRoundedPercentage(final CoverageStatistics statistics, final Metric metric) {
        Coverage coverage = (Coverage) statistics.getValue(Baseline.PROJECT, metric)
                .orElse(Coverage.nullObject(metric));
        return (int) Math.round(coverage.getCoveredPercentage().multiplyBy(Fraction.getFraction(100, 1)).doubleValue());
    }
}
