package io.jenkins.plugins.coverage.util;

import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.MainPanel;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

/**
 * Due to Coverage TrendChart is displayed twice, in ({@link CoverageReport} and {@link MainPanel}, this helper class
 * provides static verifying methods that can be used in all tests for a specific TrendChart.
 */
@SuppressWarnings("hideutilityclassconstructor")
public class TrendChartTestUtil {
    private static final int FIRST_LINE_COVERAGE = 96;
    private static final int FIRST_BRANCH_COVERAGE = 89;
    private static final int SECOND_LINE_COVERAGE = 91;
    private static final int SECOND_BRANCH_COVERAGE = 94;
    private static final String LINE_COVERAGE = "Line";
    private static final String BRANCH_COVERAGE = "Branch";

    /**
     * Verifies if a specific generated TrendChart has the correct build numbers in its axis and the right coverage
     * values for its builds.
     *
     * @param trendChart
     *         from coverage report
     * @param firstBuildInChartNumber
     *         first buildnumber displayed in TrendChart
     * @param lastBuildInChartNumber
     *         last buildnumber displayed in TrendChart
     */
    public static void verifyTrendChart(final String trendChart, final int firstBuildInChartNumber,
            final int lastBuildInChartNumber) {
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
                        a -> a.node("series[0].name").isEqualTo(LINE_COVERAGE),
                        a -> a.node("series[0].data").isArray()
                                .containsExactly(FIRST_LINE_COVERAGE, SECOND_LINE_COVERAGE),
                        a -> a.node("series[1].name").isEqualTo(BRANCH_COVERAGE),
                        a -> a.node("series[1].data").isArray()
                                .containsExactly(FIRST_BRANCH_COVERAGE, SECOND_BRANCH_COVERAGE)
                );
    }

    /**
     * Verifies if specific a generated TrendChart has the correct number of builds in its axis and the right coverage
     * values for its builds.
     *
     * @param trendChart
     *         which should only contain one record
     */
    public static void verifyTrendChartContainsOnlyOneRecord(final String trendChart) {
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
                        a -> a.node("series[0].name").isEqualTo(LINE_COVERAGE),
                        a -> a.node("series[0].data").isArray().containsExactly(FIRST_LINE_COVERAGE),
                        a -> a.node("series[1].name").isEqualTo(BRANCH_COVERAGE),
                        a -> a.node("series[1].data").isArray().contains(FIRST_BRANCH_COVERAGE)
                );
    }
}
