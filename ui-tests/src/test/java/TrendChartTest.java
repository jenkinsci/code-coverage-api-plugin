import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.PageObject;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.Adapter;
import io.jenkins.plugins.coverage.MainPanel;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;
import static org.assertj.core.api.Assertions.*;

public class TrendChartTest extends AbstractJUnitTest {
    private static final String JACOCO_ANALYSIS_MODEL_XML = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODINGSTYLE_XML = "jacoco-codingstyle.xml";
    private static final String RESOURCES_FOLDER = "/io.jenkins.plugins.coverage";

    /**
     * Check if the generated TrendChart has the correct number of builds in its axis and the right coverage values for its builds.
     */
    public static void verifyTrendChart(String trendChart) {
        assertThatJson(trendChart)
                .inPath("$.xAxis[*].data[*]")
                .isArray()
                .hasSize(2)
                .contains("#2")
                .contains("#3");

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
     * Trend-charts are available for a project with two or more builds. A Project with only one build should not
     * generate a Trend-chart.
     */
    @Test
    public void verifyNoTrendChartIsGenerated(PageObject pageObject) {
        //MainPanel mp = new MainPanel(job);
        //mp.open();
        //assertThat(mp.trendChartIsDisplayed()).isFalse();
    }

}
