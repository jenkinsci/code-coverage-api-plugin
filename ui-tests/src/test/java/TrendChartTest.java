import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;

public class TrendChartTest extends AbstractJUnitTest {

    /**
     * Check if the generated TrendChart has the correct number of builds in its axis and the right coverage values for
     * its builds.
     */
    public static void verifyTrendChart(final String trendChart) {
        assertThatJson(trendChart)
                .inPath("$.xAxis[*].data[*]")
                .isArray()
                .hasSize(3)
                .contains("#2")
                .contains("#5");

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

}
