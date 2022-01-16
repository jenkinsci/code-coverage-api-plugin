import io.jenkins.plugins.coverage.CoverageReport;
import io.jenkins.plugins.coverage.MainPanel;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

/**
 * Due to Coverage TrendChart is displayed twice, in ({@link CoverageReport} and {@link MainPanel},
 * this helper class provides static verifying methods that can be used in all tests for a specific TrendChart.
 */
public class TrendChartTestUtil {

    /**
     * Verifies if a specific generated TrendChart has the correct number of builds in its axis and the right coverage values for
     * its builds.
     */
    public static void verifyTrendChart(final String trendChart, int firstBuildInChartNumber, int lastBuildInChartNumber ) {
        assertThatJson(trendChart)
                .inPath("$.xAxis[*].data[*]")
                .isArray()
                .contains("#"+firstBuildInChartNumber)
                .contains("#"+lastBuildInChartNumber);

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
                        a -> a.node("series[0].data").isArray().contains(95).contains(91),
                        a -> a.node("series[1].data").isArray().contains(88).contains(93)
                );
    }


    /**
     * Verifies if specific a generated TrendChart has the correct number of builds in its axis and the right coverage values for
     * its builds.
     */
    public static void verifyTrendChartContainsOnlyOneRecord(final String trendChart) {
        assertThatJson(trendChart)
                .inPath("$.xAxis[*].data[*]")
                .isArray()
                .contains("#"+1);

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
