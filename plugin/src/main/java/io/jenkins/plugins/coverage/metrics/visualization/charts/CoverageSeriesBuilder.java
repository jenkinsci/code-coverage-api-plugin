package io.jenkins.plugins.coverage.metrics.visualization.charts;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.echarts.SeriesBuilder;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Metric;

import io.jenkins.plugins.coverage.metrics.Baseline;
import io.jenkins.plugins.coverage.metrics.CoverageBuildAction;

/**
 * Builds one x-axis point for the series of a line chart showing the line and branch coverage of a project.
 *
 * @author Ullrich Hafner
 */
public class CoverageSeriesBuilder extends SeriesBuilder<CoverageBuildAction> {
    static final String LINE_COVERAGE = "line";
    static final String BRANCH_COVERAGE = "branch";

    @Override
    protected Map<String, Integer> computeSeries(final CoverageBuildAction action) {
        Map<String, Integer> series = new HashMap<>();

        extractValue(action, Metric.LINE, series, LINE_COVERAGE);
        extractValue(action, Metric.BRANCH, series, BRANCH_COVERAGE);

        return series;
    }

    private void extractValue(final CoverageBuildAction action, final Metric metric,
            final Map<String, Integer> series, final String seriesId) {
        action.getAllValues(Baseline.PROJECT).stream()
                .filter(v -> v.getMetric().equals(metric))
                .forEach(v -> series.put(seriesId, getRoundedPercentage((Coverage) v)));
    }

    // TODO: we should make the charts accept float values
    private int getRoundedPercentage(final Coverage coverage) {
        if (coverage.isSet()) {
            return (int) Math.round(coverage.getCoveredPercentage()
                    .multiplyBy(Fraction.getFraction(100, 1)).doubleValue());
        }
        return 0;
    }
}
