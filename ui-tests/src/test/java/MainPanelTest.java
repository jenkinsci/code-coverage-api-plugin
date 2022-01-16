import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;

import io.jenkins.plugins.coverage.MainPanel;

/**
 * UI-Tests for MainPanel of a project.
 */
public class MainPanelTest extends AbstractJUnitTest {

    /**
     * Verifies displayed Trendchart.
     *
     * @param mainPanel
     *         of project
     */
    public static void verifyTrendChartWithTwoReports(final MainPanel mainPanel) {
        mainPanel.open();

        String trendChart = mainPanel.getCoverageTrendChart();
        TrendChartTest.verifyTrendChart(trendChart);
    }

}

