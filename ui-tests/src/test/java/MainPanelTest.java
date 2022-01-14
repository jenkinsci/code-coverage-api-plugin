import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;

import io.jenkins.plugins.coverage.MainPanel;

import static org.assertj.core.api.Assertions.*;

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

    /**
     * Verifies no Trendchart is displayed, when builds never contained more than one report.
     *
     * @param mainPanel
     *         of project
     */
    public static void verifyTrendChartNotDisplayed(final MainPanel mainPanel) {
        mainPanel.open();
        assertThat(mainPanel.isChartAvailable()).isFalse();
    }

}

