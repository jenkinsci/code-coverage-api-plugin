package io.jenkins.plugins.coverage.util;

import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.MainPanel;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

/**
 * Due to Coverage TrendChart is displayed twice, in ({@link CoverageReport} and {@link MainPanel},
 * this helper class provides static verifying methods that can be used in all tests for a specific TrendChart.
 */
@SuppressWarnings("hideutilityclassconstructor")
public class TrendChartTestUtil {

    /**
     * Verifies if a specific generated TrendChart has the correct buildnumbers in its axis and the right coverage values for
     * its builds.
     * @param trendChart from coverage report
     * @param firstBuildInChartNumber first buildnumber displayed in TrendChart
     * @param lastBuildInChartNumber last buildnumber displayed in TrendChart
     */
    public static void verifyTrendChart(String trendChart, int firstBuildInChartNumber, int lastBuildInChartNumber) {
        assertThatJson(trendChart)
                .inPath("$.xAxis[*].data[*]")
                .isArray()
                .contains("#" + firstBuildInChartNumber)
                .contains("#" + lastBuildInChartNumber);

        assertThatJson(trendChart)
                .node("series")
                .isArray()
                .hasSize(2);

        assertThatJson(trendChart)
                .and(
                        a -> a.node("series[0].name").isEqualTo("Line"),
                        a -> a.node("series[1].name").isEqualTo("Branch")
                );

        assertThatJson(trendChart)
                .and(
                        a -> a.node("series[0].data").isArray().contains(96).contains(91),
                        a -> a.node("series[1].data").isArray().contains(89).contains(94)
                );
    }

    /**
     * Verifies if specific a generated TrendChart has the correct number of builds in its axis and the right coverage values for
     * its builds.
     * @param trendChart which should only contain one record
     */
    public static void verifyTrendChartContainsOnlyOneRecord(String trendChart) {
        assertThatJson(trendChart)
                .inPath("$.xAxis[*].data[*]")
                .isArray()
                .contains("#" + 1);

        assertThatJson(trendChart)
                .node("series")
                .isArray()
                .hasSize(2);

        assertThatJson(trendChart)
                .and(
                        a -> a.node("series[0].name").isEqualTo("Line"),
                        a -> a.node("series[1].name").isEqualTo("Branch")
                );

        assertThatJson(trendChart).node("series[0].data").isArray().contains(95);
    }

}
